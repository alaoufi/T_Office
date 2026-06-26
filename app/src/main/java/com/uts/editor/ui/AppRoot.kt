package com.uts.editor.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uts.editor.R
import com.uts.editor.data.AppLanguage
import com.uts.editor.data.AppSettings
import com.uts.editor.model.LoadMode
import com.uts.editor.ui.theme.syntaxColorsFor
import com.uts.editor.util.LocaleManager
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
    var pendingClose by remember { mutableStateOf<Int?>(null) }
    var showSaveName by remember { mutableStateOf(false) }

    val pickSaveFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { viewModel.setSaveFolder(it) } }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.open(it) } }

    val saveAsLauncher = rememberLauncherForActivityResult(
        remember { CreateTextDocument() }
    ) { uri -> uri?.let { viewModel.saveAs(it) } }

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

    val startNewSave: () -> Unit = {
        if (settings.saveFolderUri != null) showSaveName = true
        else saveAsLauncher.launch(viewModel.active?.doc?.displayName ?: "untitled.txt")
    }
    val onCloseActive: () -> Unit = {
        val idx = viewModel.activeIndex
        if (!viewModel.requestCloseTab(idx)) pendingClose = idx
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it.text) }
    }

    val active = viewModel.active
    val dark = isDark(settings)
    val syntaxColors = remember(dark) { syntaxColorsFor(dark) }

    val layoutDir = when (LocaleManager.storedLanguage(context)) {
        AppLanguage.ARABIC -> LayoutDirection.Rtl
        AppLanguage.ENGLISH -> LayoutDirection.Ltr
        AppLanguage.SYSTEM -> LocalLayoutDirection.current
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { active?.let { StatusBar(it) } },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val readOnly = active?.doc?.loadMode == LoadMode.READONLY_LARGE

                // One compact row: overflow menu (carries file actions + file name) + edit/format tools.
                CompactToolbar(
                    enabled = active != null && !readOnly,
                    fileName = active?.doc?.displayName ?: stringResource(R.string.app_name),
                    modified = active?.isModified() == true,
                    menuOpen = menuOpen,
                    onMenuOpen = { menuOpen = true },
                    onMenuDismiss = { menuOpen = false },
                    onNew = { menuOpen = false; viewModel.newDocument() },
                    onOpen = { menuOpen = false; openLauncher.launch(arrayOf("*/*")) },
                    onSave = { menuOpen = false; viewModel.save(onNeedSaveAs = startNewSave) },
                    onSaveAs = { menuOpen = false; saveAsLauncher.launch(active?.doc?.displayName ?: "untitled.txt") },
                    onCloseDoc = { menuOpen = false; onCloseActive() },
                    onGoto = { menuOpen = false; showGoto = true },
                    onShare = { menuOpen = false; active?.let { ShareHelper.shareText(context, it.doc.displayName, it.field.text) } },
                    onExportPdf = { menuOpen = false; exportPdfLauncher.launch((active?.doc?.displayName ?: "document") + ".pdf") },
                    onPrint = { menuOpen = false; active?.let { PrintHelper.print(context, it.doc.displayName, it.field.text) } },
                    onSettings = { menuOpen = false; showSettings = true },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onFind = { viewModel.showFind(true) },
                    onBold = { viewModel.wrapSelection("**", "**") },
                    onItalic = { viewModel.wrapSelection("*", "*") },
                    onHeading = { viewModel.prefixLines("# ") },
                    onList = { viewModel.prefixLines("- ") },
                    onQuote = { viewModel.prefixLines("> ") },
                )

                if (viewModel.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())

                TabStrip(
                    tabs = viewModel.tabs,
                    activeIndex = viewModel.activeIndex,
                    onSelect = { viewModel.switchTo(it) },
                    onClose = { idx -> if (!viewModel.requestCloseTab(idx)) pendingClose = idx },
                )

                if (viewModel.findState.visible) {
                    FindReplaceBar(
                        state = viewModel.findState,
                        readOnly = readOnly,
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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

        if (showSaveName && active != null) {
            FileNameDialog(
                initial = active.doc.displayName,
                folderName = settings.saveFolderName,
                onConfirm = { name ->
                    showSaveName = false
                    viewModel.saveNewToDefaultFolder(name, onFallback = { saveAsLauncher.launch(name) })
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
                        viewModel.save(onNeedSaveAs = { saveAsLauncher.launch(tab.doc.displayName) })
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
}

/** Single low-profile row: the ⋮ menu (file actions + current file name) followed
 *  by the edit/format tools. Designed to use as little vertical space as possible. */
@Composable
private fun CompactToolbar(
    enabled: Boolean,
    fileName: String,
    modified: Boolean,
    menuOpen: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onNew: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onCloseDoc: () -> Unit,
    onGoto: () -> Unit,
    onShare: () -> Unit,
    onExportPdf: () -> Unit,
    onPrint: () -> Unit,
    onSettings: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFind: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onHeading: () -> Unit,
    onList: () -> Unit,
    onQuote: () -> Unit,
) {
    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                ToolButton(Icons.Filled.MoreVert, R.string.action_menu, onClick = onMenuOpen)
                DropdownMenu(expanded = menuOpen, onDismissRequest = onMenuDismiss) {
                    // File name header (at the top of the menu, as requested).
                    DropdownMenuItem(
                        enabled = false,
                        text = {
                            Text(
                                text = if (modified) "• $fileName" else fileName,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {},
                    )
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_new)) }, onClick = onNew)
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_open)) }, onClick = onOpen)
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_save)) }, onClick = onSave)
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_save_as)) }, onClick = onSaveAs)
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_close_document)) }, onClick = onCloseDoc)
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_goto_line)) }, onClick = onGoto)
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_share)) }, onClick = onShare)
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_export_pdf)) }, onClick = onExportPdf)
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_print)) }, onClick = onPrint)
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_settings)) }, onClick = onSettings)
                }
            }
            ToolDivider()
            ToolButton(Icons.Filled.Undo, R.string.action_undo, enabled = enabled, onClick = onUndo)
            ToolButton(Icons.Filled.Redo, R.string.action_redo, enabled = enabled, onClick = onRedo)
            ToolButton(Icons.Filled.Search, R.string.action_find, onClick = onFind)
            ToolDivider()
            ToolButton(Icons.Filled.FormatBold, R.string.format_bold, enabled = enabled, onClick = onBold)
            ToolButton(Icons.Filled.FormatItalic, R.string.format_italic, enabled = enabled, onClick = onItalic)
            ToolButton(Icons.Filled.Title, R.string.format_heading, enabled = enabled, onClick = onHeading)
            ToolButton(Icons.Filled.FormatListBulleted, R.string.format_list, enabled = enabled, onClick = onList)
            ToolButton(Icons.Filled.FormatQuote, R.string.format_quote, enabled = enabled, onClick = onQuote)
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, descRes: Int, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = stringResource(descRes), modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun ToolDivider() {
    Spacer(Modifier.width(3.dp))
    Surface(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.width(1.dp).height(22.dp),
    ) {}
    Spacer(Modifier.width(3.dp))
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
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == activeIndex
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { onSelect(index) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, end = 2.dp)) {
                    Text(
                        text = (if (tab.isModified()) "• " else "") + tab.doc.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                    IconButton(onClick = { onClose(index) }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.Close, stringResource(R.string.tab_close), modifier = Modifier.size(15.dp))
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
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val s = tab.doc.stats
            StatusItem(stringResource(R.string.status_position, line, col))
            StatusItem(stringResource(R.string.status_lines, s.lines))
            StatusItem(stringResource(R.string.status_words, s.words))
            StatusItem(stringResource(R.string.status_chars, s.chars))
            StatusItem(stringResource(R.string.status_size, EditorViewModel.humanSize(s.sizeBytes)))
        }
    }
}

@Composable
private fun StatusItem(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, maxLines = 1)
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
    com.uts.editor.data.ThemeMode.SYSTEM -> false
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
            .setType(com.uts.editor.data.FileIo.mimeForName(input))
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
