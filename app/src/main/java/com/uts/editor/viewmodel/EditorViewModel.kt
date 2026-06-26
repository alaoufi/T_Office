package com.uts.editor.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uts.editor.data.EncodingDetector
import com.uts.editor.data.FileIo
import com.uts.editor.data.RecoveryStore
import com.uts.editor.data.SettingsStore
import com.uts.editor.data.ZipSupport
import com.uts.editor.model.DocumentState
import com.uts.editor.model.LineEnding
import com.uts.editor.model.LoadMode
import com.uts.editor.model.SyntaxLanguage
import com.uts.editor.model.TextEncoding
import com.uts.editor.model.TextStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val resolver get() = getApplication<Application>().contentResolver
    val settingsStore = SettingsStore(app)
    private val recovery = RecoveryStore(app)

    val tabs = mutableStateListOf<EditorTab>()
    var activeIndex by mutableIntStateOf(0)
        private set

    val active: EditorTab? get() = tabs.getOrNull(activeIndex)

    var encodingPrompt by mutableStateOf<EncodingPrompt?>(null)
        private set
    var zipPrompt by mutableStateOf<ZipPrompt?>(null)
        private set
    var binaryPrompt by mutableStateOf<BinaryPrompt?>(null)
        private set
    var findState by mutableStateOf(FindState())
        private set
    var isBusy by mutableStateOf(false)
        private set
    var recoverableDrafts by mutableStateOf<List<RecoveryStore.Draft>>(emptyList())
        private set

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init {
        recoverableDrafts = recovery.pending()
        startAutosaveLoop()
        if (tabs.isEmpty() && recoverableDrafts.isEmpty()) newDocument()
    }

    // ----------------------------------------------------------------- tabs

    fun newDocument() {
        val id = UUID.randomUUID().toString()
        val name = "untitled.txt"
        val doc = DocumentState(
            id = id,
            displayName = name,
            encoding = TextEncoding.UTF_8,
            language = SyntaxLanguage.PLAIN,
            stats = TextStats(),
        )
        val tab = EditorTab(doc, TextFieldValue(""))
        tabs.add(tab)
        activeIndex = tabs.lastIndex
    }

    fun switchTo(index: Int) {
        if (index in tabs.indices) activeIndex = index
    }

    /** Returns true if the tab was closed; false if it needs a discard confirmation. */
    fun requestCloseTab(index: Int): Boolean {
        val tab = tabs.getOrNull(index) ?: return true
        if (tab.isModified()) return false
        closeTab(index)
        return true
    }

    fun closeTab(index: Int) {
        val tab = tabs.getOrNull(index) ?: return
        recovery.delete(tab.id)
        tabs.removeAt(index)
        if (tabs.isEmpty()) newDocument()
        if (activeIndex >= tabs.size) activeIndex = tabs.lastIndex
    }

    // ----------------------------------------------------------------- open

    fun handleViewIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> intent.data?.let { open(it) }
            Intent.ACTION_SEND -> {
                val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (shared != null) openSharedText(shared)
                else (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { open(it) }
            }
        }
    }

    private fun openSharedText(text: String) {
        val id = UUID.randomUUID().toString()
        val doc = DocumentState(id = id, displayName = "shared.txt", isModified = true)
        val tab = EditorTab(doc, TextFieldValue(text))
        tab.savedSignature = -1 // force modified
        tab.doc = tab.doc.copy(stats = TextStats.of(text, text.toByteArray().size.toLong()), isModified = true)
        tabs.add(tab); activeIndex = tabs.lastIndex
    }

    fun open(uri: Uri) {
        viewModelScope.launch {
            runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            isBusy = true
            try {
                val meta = withContext(Dispatchers.IO) { FileIo.queryMeta(resolver, uri) }
                if (meta.name.endsWith(".zip", true)) {
                    val entries = withContext(Dispatchers.IO) { ZipSupport.listTextEntries(resolver, uri) }
                    zipPrompt = ZipPrompt(uri, entries)
                    return@launch
                }
                if (meta.size > FileIo.ABSOLUTE_LIMIT_BYTES) {
                    emit("File exceeds the 500 MB limit.")
                    return@launch
                }
                val sample = withContext(Dispatchers.IO) { FileIo.readSample(resolver, uri) }

                // Honour a remembered encoding for this exact file.
                val rememberedId = withContext(Dispatchers.IO) { settingsStore.lastEncodingFor(uri.toString()) }
                val remembered = TextEncoding.byId(rememberedId)
                if (remembered != null) {
                    loadFromUri(uri, meta.name, meta.size, remembered)
                    return@launch
                }

                // Assess the file first: if it isn't text (image, archive, binary),
                // warn instead of treating it as text / asking for an encoding.
                if (EncodingDetector.looksBinary(sample)) {
                    binaryPrompt = BinaryPrompt(uri, meta.name, meta.size)
                    return@launch
                }

                val detection = EncodingDetector.detect(sample)
                if (detection.needsConfirmation) {
                    encodingPrompt = EncodingPrompt(
                        uri = uri, zipEntry = null, displayName = meta.name, size = meta.size,
                        sample = sample, suggested = detection.encoding,
                        confidence = detection.confidence, reason = EncodingPromptReason.DETECT_FAILED,
                    )
                } else {
                    loadFromUri(uri, meta.name, meta.size, detection.encoding)
                }
            } catch (e: Exception) {
                emit("Could not open file: ${e.message}")
            } finally {
                isBusy = false
            }
        }
    }

    fun openZipEntry(entry: ZipSupport.Entry) {
        val prompt = zipPrompt ?: return
        zipPrompt = null
        viewModelScope.launch {
            isBusy = true
            try {
                val bytes = withContext(Dispatchers.IO) {
                    ZipSupport.readEntryBytes(resolver, prompt.uri, entry.name)
                }
                if (EncodingDetector.looksBinary(bytes)) {
                    emit("\"${entry.name}\" is not a text file.")
                    return@launch
                }
                val detection = EncodingDetector.detect(bytes)
                val enc = detection.encoding
                val raw = withContext(Dispatchers.IO) { String(bytes, enc.charset()) }
                val (text, ending) = normalizeIn(raw)
                val baseName = entry.name.substringAfterLast('/')
                val id = UUID.randomUUID().toString()
                val doc = DocumentState(
                    id = id, uri = null, displayName = baseName, encoding = enc,
                    lineEnding = ending, language = SyntaxLanguage.fromFileName(baseName),
                    loadMode = LoadMode.EDITABLE, isModified = true,
                    stats = TextStats.of(text, bytes.size.toLong()), totalBytes = bytes.size.toLong(),
                    zipEntry = entry.name,
                )
                val tab = EditorTab(doc, TextFieldValue(text)).also { it.savedSignature = -1 }
                tabs.add(tab); activeIndex = tabs.lastIndex
            } catch (e: Exception) {
                emit("Could not read entry: ${e.message}")
            } finally {
                isBusy = false
            }
        }
    }

    fun dismissZipPrompt() { zipPrompt = null }

    /** User chose to open a non-text file anyway: load it losslessly as Latin-1
     *  (1:1 byte mapping) so bytes are preserved for viewing. */
    fun openBinaryAnyway() {
        val p = binaryPrompt ?: return
        binaryPrompt = null
        viewModelScope.launch {
            val enc = if (TextEncoding.ISO_8859_1.isAvailable()) TextEncoding.ISO_8859_1 else TextEncoding.UTF_8
            loadFromUri(p.uri, p.displayName, p.size, enc)
        }
    }

    fun cancelBinaryPrompt() { binaryPrompt = null }

    /** Confirm an encoding chosen in the manual dialog. */
    fun confirmEncoding(encoding: TextEncoding) {
        val prompt = encodingPrompt ?: return
        encodingPrompt = null
        viewModelScope.launch {
            prompt.uri?.let { loadFromUri(it, prompt.displayName, prompt.size, encoding) }
        }
    }

    fun cancelEncodingPrompt() { encodingPrompt = null }

    /** Reopen the active document with a different encoding (re-decodes from disk). */
    fun reopenActiveWithEncoding() {
        val tab = active ?: return
        val uri = tab.doc.uri ?: run { emit("This document has no source file."); return }
        viewModelScope.launch {
            val sample = withContext(Dispatchers.IO) { FileIo.readSample(resolver, uri) }
            val detection = EncodingDetector.detect(sample)
            encodingPrompt = EncodingPrompt(
                uri = uri, zipEntry = tab.doc.zipEntry, displayName = tab.doc.displayName,
                size = tab.doc.totalBytes, sample = sample, suggested = tab.doc.encoding,
                confidence = detection.confidence, reason = EncodingPromptReason.USER_REQUEST_REOPEN,
            )
        }
    }

    private suspend fun loadFromUri(uri: Uri, name: String, size: Long, encoding: TextEncoding) {
        isBusy = true
        try {
            val loadMode = FileIo.loadModeFor(size)
            val language = SyntaxLanguage.fromFileName(name)
            val id = UUID.randomUUID().toString()

            if (loadMode == LoadMode.EDITABLE) {
                val raw = withContext(Dispatchers.IO) { FileIo.readAll(resolver, uri, encoding) }
                val (text, ending) = normalizeIn(raw)
                val doc = DocumentState(
                    id = id, uri = uri, displayName = name, encoding = encoding, lineEnding = ending,
                    language = language, loadMode = loadMode, isModified = false,
                    stats = TextStats.of(text, size), totalBytes = size,
                )
                val tab = EditorTab(doc, TextFieldValue(text))
                tabs.add(tab); activeIndex = tabs.lastIndex
            } else {
                // Large file: read-only first page.
                val window = withContext(Dispatchers.IO) {
                    FileIo.readLineWindow(resolver, uri, encoding, 0, LARGE_PAGE_LINES)
                }
                val doc = DocumentState(
                    id = id, uri = uri, displayName = name, encoding = encoding,
                    lineEnding = LineEnding.detect(window), language = language,
                    loadMode = loadMode, isModified = false,
                    stats = TextStats.of(window, size), totalBytes = size,
                )
                val tab = EditorTab(doc, TextFieldValue(window))
                tabs.add(tab); activeIndex = tabs.lastIndex
                emit("Large file (${humanSize(size)}) opened read-only in paged mode.")
            }
            withContext(Dispatchers.IO) { settingsStore.rememberEncoding(uri.toString(), encoding.id) }
        } catch (e: Exception) {
            emit("Could not open file: ${e.message}")
        } finally {
            isBusy = false
        }
    }

    fun loadNextLargePage() {
        val tab = active ?: return
        val uri = tab.doc.uri ?: return
        if (tab.doc.loadMode != LoadMode.READONLY_LARGE) return
        viewModelScope.launch {
            val next = tab.pageStartLine + LARGE_PAGE_LINES
            val window = withContext(Dispatchers.IO) {
                FileIo.readLineWindow(resolver, uri, tab.doc.encoding, next, LARGE_PAGE_LINES)
            }
            if (window.isNotEmpty()) {
                tab.pageStartLine = next
                tab.field = TextFieldValue(window)
            } else emit("End of file.")
        }
    }

    fun loadPreviousLargePage() {
        val tab = active ?: return
        val uri = tab.doc.uri ?: return
        if (tab.doc.loadMode != LoadMode.READONLY_LARGE || tab.pageStartLine == 0) return
        viewModelScope.launch {
            val prev = (tab.pageStartLine - LARGE_PAGE_LINES).coerceAtLeast(0)
            val window = withContext(Dispatchers.IO) {
                FileIo.readLineWindow(resolver, uri, tab.doc.encoding, prev, LARGE_PAGE_LINES)
            }
            tab.pageStartLine = prev
            tab.field = TextFieldValue(window)
        }
    }

    // ----------------------------------------------------------------- edit

    fun onTextChange(newValue: TextFieldValue) {
        val tab = active ?: return
        if (tab.doc.loadMode == LoadMode.READONLY_LARGE) return
        if (newValue.text != tab.field.text) {
            tab.pushUndo(tab.field)
        }
        tab.field = newValue
        refreshStats(tab)
    }

    fun undo() {
        val tab = active ?: return
        tab.undo()?.let { tab.field = it; refreshStats(tab) }
    }

    fun redo() {
        val tab = active ?: return
        tab.redo()?.let { tab.field = it; refreshStats(tab) }
    }

    private fun refreshStats(tab: EditorTab) {
        val text = tab.field.text
        val size = text.toByteArray(tab.doc.encoding.charset()).size.toLong()
        tab.doc = tab.doc.copy(
            stats = TextStats.of(text, size),
            isModified = tab.isModified(),
        )
    }

    // ----------------------------------------------------------------- save

    fun save(onNeedSaveAs: () -> Unit) {
        val tab = active ?: return
        val uri = tab.doc.uri
        if (uri == null) { onNeedSaveAs(); return }
        writeTo(tab, uri, tab.doc.encoding)
    }

    fun saveAs(uri: Uri, encoding: TextEncoding? = null) {
        val tab = active ?: return
        runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = FileIo.queryMeta(resolver, uri).name
        tab.doc = tab.doc.copy(uri = uri, displayName = name, language = SyntaxLanguage.fromFileName(name))
        writeTo(tab, uri, encoding ?: tab.doc.encoding)
    }

    /** Persist the user's chosen default save folder (a SAF tree Uri). */
    fun setSaveFolder(treeUri: Uri) {
        viewModelScope.launch {
            runCatching {
                resolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            val name = withContext(Dispatchers.IO) { FileIo.treeDisplayName(getApplication(), treeUri) }
            settingsStore.setSaveFolder(treeUri.toString(), name)
        }
    }

    fun clearSaveFolder() {
        viewModelScope.launch { settingsStore.setSaveFolder(null, null) }
    }

    /**
     * Create [fileName] inside the configured default folder and save the active
     * tab there. Returns false (so the caller can fall back to Save As) when no
     * folder is set or creation fails.
     */
    fun saveNewToDefaultFolder(fileName: String, onFallback: () -> Unit) {
        val tab = active ?: return
        viewModelScope.launch {
            val folder = settingsStore.current().saveFolderUri
            if (folder == null) { onFallback(); return@launch }
            val uri = withContext(Dispatchers.IO) {
                FileIo.createInTree(getApplication(), Uri.parse(folder), fileName)
            }
            if (uri == null) { emit("Could not write to the chosen folder."); onFallback(); return@launch }
            tab.doc = tab.doc.copy(
                uri = uri, displayName = fileName,
                language = SyntaxLanguage.fromFileName(fileName),
            )
            writeTo(tab, uri, tab.doc.encoding)
        }
    }

    fun saveWithEncoding(encoding: TextEncoding) {
        val tab = active ?: return
        val uri = tab.doc.uri ?: return
        tab.doc = tab.doc.copy(encoding = encoding)
        writeTo(tab, uri, encoding)
    }

    private fun writeTo(tab: EditorTab, uri: Uri, encoding: TextEncoding) {
        viewModelScope.launch {
            isBusy = true
            try {
                withContext(Dispatchers.IO) {
                    FileIo.writeAll(resolver, uri, tab.field.text, encoding, tab.doc.lineEnding)
                    settingsStore.rememberEncoding(uri.toString(), encoding.id)
                }
                tab.markSaved()
                tab.doc = tab.doc.copy(encoding = encoding, isModified = false)
                recovery.delete(tab.id)
                emit("Saved")
            } catch (e: Exception) {
                emit("Save failed: ${e.message}")
            } finally {
                isBusy = false
            }
        }
    }

    // ----------------------------------------------------------------- find

    fun showFind(show: Boolean) { findState = findState.copy(visible = show) }

    fun updateFind(
        query: String = findState.query,
        replacement: String = findState.replacement,
        regex: Boolean = findState.regex,
        matchCase: Boolean = findState.matchCase,
        wholeWord: Boolean = findState.wholeWord,
    ) {
        findState = findState.copy(
            query = query, replacement = replacement,
            regex = regex, matchCase = matchCase, wholeWord = wholeWord,
        )
        recomputeMatches()
    }

    private fun recomputeMatches() {
        val tab = active ?: return
        val text = tab.field.text
        val q = findState.query
        if (q.isEmpty()) { findState = findState.copy(matches = emptyList(), current = -1); return }
        val matches = try {
            val pattern = buildPattern(q) ?: run {
                findState = findState.copy(matches = emptyList(), current = -1); return
            }
            pattern.findAll(text).map { it.range.first..it.range.last + 1 }.toList()
        } catch (_: Exception) { emptyList() }
        findState = findState.copy(matches = matches, current = if (matches.isEmpty()) -1 else 0)
        moveSelectionToCurrent()
    }

    private fun buildPattern(query: String): Regex? {
        val opts = if (findState.matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
        val base = if (findState.regex) query else Regex.escape(query)
        val withWord = if (findState.wholeWord) "\\b(?:$base)\\b" else base
        return runCatching { Regex(withWord, opts) }.getOrNull()
    }

    fun findNext() {
        if (findState.matches.isEmpty()) return
        val next = (findState.current + 1) % findState.matches.size
        findState = findState.copy(current = next)
        moveSelectionToCurrent()
    }

    fun findPrevious() {
        if (findState.matches.isEmpty()) return
        val prev = (findState.current - 1 + findState.matches.size) % findState.matches.size
        findState = findState.copy(current = prev)
        moveSelectionToCurrent()
    }

    private fun moveSelectionToCurrent() {
        val tab = active ?: return
        val idx = findState.current
        val range = findState.matches.getOrNull(idx) ?: return
        tab.field = tab.field.copy(
            selection = androidx.compose.ui.text.TextRange(range.first, range.last)
        )
    }

    fun replaceCurrent() {
        val tab = active ?: return
        if (tab.doc.loadMode == LoadMode.READONLY_LARGE) return
        val idx = findState.current
        val range = findState.matches.getOrNull(idx) ?: return
        val text = tab.field.text
        val replacement = computeReplacement(text.substring(range.first, range.last))
        val newText = text.replaceRange(range.first, range.last, replacement)
        tab.pushUndo(tab.field)
        tab.field = TextFieldValue(
            newText,
            selection = androidx.compose.ui.text.TextRange(range.first + replacement.length)
        )
        refreshStats(tab)
        recomputeMatches()
    }

    fun replaceAll() {
        val tab = active ?: return
        if (tab.doc.loadMode == LoadMode.READONLY_LARGE) return
        val pattern = buildPattern(findState.query) ?: return
        val text = tab.field.text
        val newText = if (findState.regex) {
            pattern.replace(text) { m -> expandRegexReplacement(findState.replacement, m) }
        } else {
            pattern.replace(text, Regex.escapeReplacement(findState.replacement))
        }
        if (newText != text) {
            tab.pushUndo(tab.field)
            tab.field = TextFieldValue(newText)
            refreshStats(tab)
        }
        recomputeMatches()
    }

    private fun computeReplacement(matched: String): String {
        if (!findState.regex) return findState.replacement
        val m = buildPattern(findState.query)?.find(matched) ?: return findState.replacement
        return expandRegexReplacement(findState.replacement, m)
    }

    private fun expandRegexReplacement(template: String, m: MatchResult): String {
        val sb = StringBuilder()
        var i = 0
        while (i < template.length) {
            val ch = template[i]
            if (ch == '$' && i + 1 < template.length && template[i + 1].isDigit()) {
                val g = template[i + 1].digitToInt()
                sb.append(m.groupValues.getOrNull(g) ?: "")
                i += 2
            } else { sb.append(ch); i++ }
        }
        return sb.toString()
    }

    fun gotoLine(line: Int) {
        val tab = active ?: return
        val text = tab.field.text
        var offset = 0
        var current = 1
        while (current < line && offset < text.length) {
            val nl = text.indexOf('\n', offset)
            if (nl == -1) break
            offset = nl + 1
            current++
        }
        tab.field = tab.field.copy(selection = androidx.compose.ui.text.TextRange(offset))
    }

    // ------------------------------------------------------------- recovery

    fun restoreDrafts() {
        for (draft in recoverableDrafts) {
            val enc = TextEncoding.byId(draft.encodingId) ?: TextEncoding.UTF_8
            val ending = runCatching { LineEnding.valueOf(draft.lineEnding) }.getOrDefault(LineEnding.LF)
            val id = draft.id
            val doc = DocumentState(
                id = id, uri = draft.uriString?.let { Uri.parse(it) }, displayName = draft.displayName,
                encoding = enc, lineEnding = ending, language = SyntaxLanguage.fromFileName(draft.displayName),
                isModified = true, stats = TextStats.of(draft.content, draft.content.length.toLong()),
            )
            val tab = EditorTab(doc, TextFieldValue(draft.content)).also { it.savedSignature = -1 }
            tabs.add(tab)
        }
        if (tabs.isNotEmpty()) activeIndex = tabs.lastIndex
        recoverableDrafts = emptyList()
    }

    fun discardDrafts() {
        recovery.clearAll()
        recoverableDrafts = emptyList()
        if (tabs.isEmpty()) newDocument()
    }

    private fun startAutosaveLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(AUTOSAVE_INTERVAL_MS)
                if (!settingsStore.current().autosaveEnabled) continue
                autosaveNow()
            }
        }
    }

    fun autosaveNow() {
        for (tab in tabs) {
            if (tab.doc.loadMode == LoadMode.READONLY_LARGE) continue
            if (!tab.isModified()) continue
            runCatching {
                recovery.save(
                    RecoveryStore.Draft(
                        id = tab.id, displayName = tab.doc.displayName,
                        uriString = tab.doc.uri?.toString(), encodingId = tab.doc.encoding.id,
                        lineEnding = tab.doc.lineEnding.name, content = tab.field.text,
                    )
                )
            }
        }
    }

    // -------------------------------------------------------------- helpers

    /** Convert any line endings to LF for editing; report the original style. */
    private fun normalizeIn(raw: String): Pair<String, LineEnding> {
        val ending = LineEnding.detect(raw)
        val normalized = if (raw.contains('\r')) raw.replace("\r\n", "\n").replace("\r", "\n") else raw
        return normalized to ending
    }

    fun previewDecode(sample: ByteArray, encoding: TextEncoding, maxChars: Int = 2000): String {
        return runCatching {
            val s = String(sample, encoding.charset())
            if (s.length > maxChars) s.substring(0, maxChars) + "…" else s
        }.getOrElse { "" }
    }

    private fun emit(text: String) { _messages.tryEmit(UiMessage(text)) }

    companion object {
        private const val AUTOSAVE_INTERVAL_MS = 30_000L
        private const val LARGE_PAGE_LINES = 5_000

        fun humanSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format("%.1f MB", mb)
            return String.format("%.2f GB", mb / 1024.0)
        }
    }
}
