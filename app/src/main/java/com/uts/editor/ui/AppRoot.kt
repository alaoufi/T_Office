package com.uts.editor.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.uts.editor.data.FileIo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uts.editor.R
import com.uts.editor.data.AppSettings
import com.uts.editor.model.LoadMode
import com.uts.editor.model.TextEncoding
import com.uts.editor.ui.theme.syntaxColorsFor
import com.uts.editor.util.PdfExporter
import com.uts.editor.util.PrintHelper
import com.uts.editor.util.ShareHelper
import com.uts.editor.viewmodel.EditorTab
import com.uts.editor.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    viewModel: EditorViewModel,
    settings: AppSettings,
    onLanguageApplied: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var menuOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showGoto by remember { mutableStateOf(false) }
    var showSaveEncoding by remember { mutableStateOf(false) }
    var pendingClose by remember { mutableStateOf<Int?>(null) }
    var pendingSaveEncoding by remember { mutableStateOf<TextEncoding?>(null) }
    var showSaveName by remember { mutableStateOf(false) }

    val pickSaveFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { viewModel.setSaveFolder(it) } }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.open(it) } }

    val saveAsLauncher = rememberLauncherForActivityResult(
        remember { CreateTextDocument() }
    ) { uri -> uri?.let { viewModel.saveAs(it, pendingSaveEncoding) } }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val tab = viewModel.active ?: return@rememberLauncherForActivityResult
        uri?.let {
            scope.launch {
                runCatching {
                    val doc = PdfExporter.render(tab.field.text)
                    context.contentResolver.openOutputStream(it)?.use { os -> PdfExporter.writeTo(doc, os) }
                }
            }
        }
    }

    // Save flow for an unsaved document: use the default folder if one is set,
    // otherwise fall back to the system "create document" picker.
    val startNewSave: () -> Unit = {
        if (settings.saveFolderUri != null) showSaveName = true
        else { pendingSaveEncoding = null; saveAsLauncher.launch(viewModel.active?.doc?.displayName ?: "untitled.txt") }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it.text) }
    }

    val active = viewModel.active
    val dark = isDark(settings)
    val syntaxColors = remember(dark) { syntaxColorsFor(dark) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    val title = active?.doc?.displayName ?: stringResource(R.string.app_name)
                    val modified = active?.isModified() == true
                    Text(
                        text = if (modified) "• $title" else title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.newDocument() }) {
                        Icon(Icons.Filled.Add, stringResource(R.string.action_new))
                    }
                    IconButton(onClick = {
                        viewModel.save(onNeedSaveAs = startNewSave)
                    }) { Icon(Icons.Filled.Save, stringResource(R.string.action_save)) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.action_menu))
                    }
                    OverflowMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        onOpen = { menuOpen = false; openLauncher.launch(arrayOf("*/*")) },
                        onSaveAs = {
                            menuOpen = false; pendingSaveEncoding = null
                            saveAsLauncher.launch(active?.doc?.displayName ?: "untitled.txt")
                        },
                        onFind = { menuOpen = false; viewModel.showFind(true) },
                        onGoto = { menuOpen = false; showGoto = true },
                        onShare = {
                            menuOpen = false
                            active?.let { ShareHelper.shareText(context, it.doc.displayName, it.field.text) }
                        },
                        onExportPdf = {
                            menuOpen = false
                            exportPdfLauncher.launch((active?.doc?.displayName ?: "document") + ".pdf")
                        },
                        onPrint = {
                            menuOpen = false
                            active?.let { PrintHelper.print(context, it.doc.displayName, it.field.text) }
                        },
                        onReopenEncoding = { menuOpen = false; viewModel.reopenActiveWithEncoding() },
                        onSaveEncoding = { menuOpen = false; showSaveEncoding = true },
                        onSettings = { menuOpen = false; showSettings = true },
                    )
                },
            )
        },
        bottomBar = { active?.let { StatusBar(it) } },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (viewModel.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            TabStrip(
                tabs = viewModel.tabs,
                activeIndex = viewModel.activeIndex,
                onSelect = { viewModel.switchTo(it) },
                onClose = { idx ->
                    if (!viewModel.requestCloseTab(idx)) pendingClose = idx
                },
            )
            if (viewModel.findState.visible) {
                FindReplaceBar(
                    state = viewModel.findState,
                    readOnly = active?.doc?.loadMode == LoadMode.READONLY_LARGE,
                    onQueryChange = { viewModel.updateFind(query = it) },
                    onReplacementChange = { viewModel.updateFind(replacement = it) },
                    onToggleRegex = { viewModel.updateFind(regex = it) },
                    onToggleCase = { viewModel.updateFind(matchCase = it) },
                    onToggleWord = { viewModel.updateFind(wholeWord = it) },
                    onNext = { viewModel.findNext() },
                    onPrevious = { viewModel.findPrevious() },
                    onReplace = { viewModel.replaceCurrent() },
                    onReplaceAll = { viewModel.replaceAll() },
                    onClose = { viewModel.showFind(false) },
                )
            }
            if (active != null) {
                val readOnly = active.doc.loadMode == LoadMode.READONLY_LARGE
                Box(Modifier.fillMaxSize()) {
                    EditorArea(
                        value = active.field,
                        onValueChange = { viewModel.onTextChange(it) },
                        language = active.doc.language,
                        syntaxEnabled = settings.syntaxEnabled,
                        syntaxColors = syntaxColors,
                        fontSizeSp = settings.fontSizeSp,
                        showLineNumbers = settings.lineNumbers,
                        wordWrap = settings.wordWrap,
                        readOnly = readOnly,
                        matches = viewModel.findState.matches,
                        currentMatch = viewModel.findState.current,
                        modifier = Modifier.padding(4.dp),
                    )
                    if (readOnly) {
                        LargeFileControls(
                            onPrev = { viewModel.loadPreviousLargePage() },
                            onNext = { viewModel.loadNextLargePage() },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                        )
                    }
                }
            }
        }
    }

    // ---- Dialogs ----

    viewModel.encodingPrompt?.let { prompt ->
        EncodingDialog(
            prompt = prompt,
            onPreview = { sample, enc -> viewModel.previewDecode(sample, enc) },
            onConfirm = { viewModel.confirmEncoding(it) },
            onDismiss = { viewModel.cancelEncodingPrompt() },
        )
    }

    viewModel.zipPrompt?.let { zip ->
        ZipPickerDialog(
            entries = zip.entries,
            onPick = { viewModel.openZipEntry(it) },
            onDismiss = { viewModel.dismissZipPrompt() },
        )
    }

    viewModel.binaryPrompt?.let { bin ->
        BinaryFileDialog(
            name = bin.displayName,
            onOpenAnyway = { viewModel.openBinaryAnyway() },
            onCancel = { viewModel.cancelBinaryPrompt() },
        )
    }

    if (showGoto && active != null) {
        GotoLineDialog(
            maxLine = active.doc.stats.lines,
            onGo = { viewModel.gotoLine(it); showGoto = false },
            onDismiss = { showGoto = false },
        )
    }

    if (showSaveEncoding && active != null) {
        SaveEncodingDialog(
            current = active.doc.encoding,
            onConfirm = { enc ->
                showSaveEncoding = false
                if (active.doc.uri != null) viewModel.saveWithEncoding(enc)
                else { pendingSaveEncoding = enc; saveAsLauncher.launch(active.doc.displayName) }
            },
            onDismiss = { showSaveEncoding = false },
        )
    }

    if (showSaveName && active != null) {
        FileNameDialog(
            initial = active.doc.displayName,
            folderName = settings.saveFolderName,
            onConfirm = { name ->
                showSaveName = false
                viewModel.saveNewToDefaultFolder(name, onFallback = {
                    pendingSaveEncoding = null; saveAsLauncher.launch(name)
                })
            },
            onDismiss = { showSaveName = false },
        )
    }

    pendingClose?.let { idx ->
        val tab = viewModel.tabs.getOrNull(idx)
        if (tab != null) {
            DiscardDialog(
                name = tab.doc.displayName,
                onSave = {
                    pendingClose = null
                    viewModel.switchTo(idx)
                    viewModel.save(onNeedSaveAs = {
                        pendingSaveEncoding = null; saveAsLauncher.launch(tab.doc.displayName)
                    })
                },
                onDiscard = { pendingClose = null; viewModel.closeTab(idx) },
                onCancel = { pendingClose = null },
            )
        } else pendingClose = null
    }

    if (viewModel.recoverableDrafts.isNotEmpty()) {
        RecoveryDialog(
            count = viewModel.recoverableDrafts.size,
            name = viewModel.recoverableDrafts.first().displayName,
            onRestore = { viewModel.restoreDrafts() },
            onDiscard = { viewModel.discardDrafts() },
        )
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            onTheme = { scope.launch { viewModel.settingsStore.setTheme(it) } },
            onFontSize = { scope.launch { viewModel.settingsStore.setFontSize(it) } },
            onAutosave = { scope.launch { viewModel.settingsStore.setAutosave(it) } },
            onSyntax = { scope.launch { viewModel.settingsStore.setSyntax(it) } },
            onLineNumbers = { scope.launch { viewModel.settingsStore.setLineNumbers(it) } },
            onWordWrap = { scope.launch { viewModel.settingsStore.setWordWrap(it) } },
            onLanguageApplied = onLanguageApplied,
            onPickSaveFolder = { pickSaveFolderLauncher.launch(null) },
            onClearSaveFolder = { viewModel.clearSaveFolder() },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun OverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onSaveAs: () -> Unit,
    onFind: () -> Unit,
    onGoto: () -> Unit,
    onShare: () -> Unit,
    onExportPdf: () -> Unit,
    onPrint: () -> Unit,
    onReopenEncoding: () -> Unit,
    onSaveEncoding: () -> Unit,
    onSettings: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text(stringResource(R.string.action_open)) }, onClick = onOpen)
        DropdownMenuItem(text = { Text(stringResource(R.string.action_save_as)) }, onClick = onSaveAs)
        DropdownMenuItem(text = { Text(stringResource(R.string.action_find)) }, onClick = onFind)
        DropdownMenuItem(text = { Text(stringResource(R.string.action_goto_line)) }, onClick = onGoto)
        DropdownMenuItem(text = { Text(stringResource(R.string.encoding_reopen_with)) }, onClick = onReopenEncoding)
        DropdownMenuItem(text = { Text(stringResource(R.string.encoding_save_with)) }, onClick = onSaveEncoding)
        DropdownMenuItem(text = { Text(stringResource(R.string.action_share)) }, onClick = onShare)
        DropdownMenuItem(text = { Text(stringResource(R.string.action_export_pdf)) }, onClick = onExportPdf)
        DropdownMenuItem(text = { Text(stringResource(R.string.action_print)) }, onClick = onPrint)
        DropdownMenuItem(text = { Text(stringResource(R.string.action_settings)) }, onClick = onSettings)
    }
}

@Composable
private fun TabStrip(
    tabs: List<EditorTab>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
) {
    if (tabs.size <= 1) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == activeIndex
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onSelect(index) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 2.dp)) {
                    Text(
                        text = (if (tab.isModified()) "• " else "") + tab.doc.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                    IconButton(onClick = { onClose(index) }, modifier = Modifier.padding(0.dp)) {
                        Icon(Icons.Filled.Close, stringResource(R.string.tab_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBar(tab: EditorTab) {
    val (line, col) = caretPosition(tab.field.text, tab.field.selection.start)
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val s = tab.doc.stats
            StatusItem(stringResource(R.string.status_position, line, col))
            StatusItem(stringResource(R.string.status_lines, s.lines))
            StatusItem(stringResource(R.string.status_words, s.words))
            StatusItem(stringResource(R.string.status_chars, s.chars))
            StatusItem(stringResource(R.string.status_size, EditorViewModel.humanSize(s.sizeBytes)))
            StatusItem(stringResource(R.string.status_encoding, tab.doc.encoding.displayName))
        }
    }
}

@Composable
private fun StatusItem(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
}

@Composable
private fun LargeFileControls(onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.action_previous))
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.action_next))
            }
        }
    }
}

private fun isDark(settings: AppSettings): Boolean = when (settings.theme) {
    com.uts.editor.data.ThemeMode.DARK -> true
    com.uts.editor.data.ThemeMode.LIGHT -> false
    com.uts.editor.data.ThemeMode.SYSTEM -> false // refined by system in theme; status colors only
}

/**
 * Like [ActivityResultContracts.CreateDocument] but derives the MIME type from
 * the requested file name, so the storage provider preserves the exact
 * extension (e.g. .json, .kt, .sql) instead of appending ".txt".
 */
private class CreateTextDocument : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(FileIo.mimeForName(input))
            .putExtra(Intent.EXTRA_TITLE, input)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

private fun caretPosition(text: String, offset: Int): Pair<Int, Int> {
    val safe = offset.coerceIn(0, text.length)
    var line = 1
    var lineStart = 0
    var i = 0
    while (i < safe) {
        if (text[i] == '\n') { line++; lineStart = i + 1 }
        i++
    }
    return line to (safe - lineStart + 1)
}
