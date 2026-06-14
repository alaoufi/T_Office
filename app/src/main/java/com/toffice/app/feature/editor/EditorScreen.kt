package com.toffice.app.feature.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.FormatTextdirectionLToR
import androidx.compose.material.icons.automirrored.filled.FormatTextdirectionRToL
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toffice.app.feature.editor.model.DocBundle
import com.toffice.app.feature.editor.model.DocSerializer
import com.toffice.app.feature.editor.model.FONT_FAMILY_NAMES
import com.toffice.app.feature.editor.model.PAGE_SIZES
import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.RichTextOps
import com.toffice.app.feature.editor.model.currentPresetId
import com.toffice.app.feature.editor.model.isLandscape
import com.toffice.app.feature.editor.model.pageSizeById
import com.toffice.app.feature.editor.model.paginationPageCount
import com.toffice.app.feature.editor.model.withSize
import com.toffice.app.feature.editor.ui.HorizontalRuler
import com.toffice.app.feature.editor.ui.VerticalRuler

private val SWATCHES = listOf(
    0xFF000000.toInt(), 0xFFD32F2F.toInt(), 0xFF1565C0.toInt(),
    0xFF2E7D32.toInt(), 0xFFEF6C00.toInt(), 0xFF6A1B9A.toInt(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var initialized by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var value by remember { mutableStateOf(TextFieldValue()) }
    var page by remember { mutableStateOf(PageSettings()) }
    var header by remember { mutableStateOf(TextFieldValue()) }
    var footer by remember { mutableStateOf(TextFieldValue()) }

    // الحقل المركّز حالياً يحدّد أين يطبّق شريط التنسيق
    var focusTarget by remember { mutableStateOf(EditField.Body) }
    // الترويسة/التذييل قيد التحرير (null = مقفلة، تُفتح بنقر مزدوج فقط)
    var hfEditing by remember { mutableStateOf<EditField?>(null) }
    // ارتفاع الورقة الفعلي (لمدّ المسطرة الجانبية ورسم فواصل الصفحات)
    var sheetHeightPx by remember { mutableStateOf(0) }

    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    fun update(newRaw: TextFieldValue) {
        val new = RichTextOps.maybeContinueList(value, newRaw)
        if (new.annotatedString != value.annotatedString) {
            undoStack.add(value)
            if (undoStack.size > 120) undoStack.removeAt(0)
            redoStack.clear()
        }
        value = new
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(value)
            value = undoStack.removeAt(undoStack.lastIndex)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(value)
            value = redoStack.removeAt(redoStack.lastIndex)
        }
    }

    LaunchedEffect(ui.isLoading) {
        if (!ui.isLoading && !initialized) {
            title = ui.title
            val bundle = DocSerializer.parse(ui.json)
            value = TextFieldValue(bundle.body)
            page = bundle.page
            header = TextFieldValue(bundle.header)
            footer = TextFieldValue(bundle.footer)
            initialized = true
        }
    }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    var showMenu by remember { mutableStateOf(false) }
    var showPageSetup by remember { mutableStateOf(false) }
    var showSaveAs by remember { mutableStateOf(false) }
    var ribbonTab by remember { mutableStateOf(RibbonTab.Home) }
    var showRuler by remember { mutableStateOf(true) }
    var showFindReplace by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.openDocx(uri) { newTitle, bundle ->
            title = newTitle
            value = TextFieldValue(bundle.body)
            page = bundle.page
            header = TextFieldValue(bundle.header)
            footer = TextFieldValue(bundle.footer)
            undoStack.clear()
            redoStack.clear()
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MIME_DOCX)
    ) { uri ->
        if (uri != null) viewModel.exportDocx(uri, value.annotatedString, page, header.annotatedString, footer.annotatedString)
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) viewModel.exportPdf(uri, value.annotatedString, page, header.annotatedString, footer.annotatedString)
    }
    val txtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) viewModel.exportText(uri, value.text)
    }

    fun persist() {
        if (initialized) {
            val json = DocSerializer.serialize(DocBundle(value.annotatedString, page, header.annotatedString, footer.annotatedString))
            viewModel.save(title, json, value.annotatedString, page, header.annotatedString, footer.annotatedString)
        }
    }

    // القيمة والتغيير حسب الحقل المركّز (المتن/الترويسة/التذييل)
    val activeValue = when (focusTarget) {
        EditField.Body -> value
        EditField.Header -> header
        EditField.Footer -> footer
    }
    val activeOnChange: (TextFieldValue) -> Unit = when (focusTarget) {
        EditField.Body -> { v -> update(v) }
        EditField.Header -> { v -> header = v }
        EditField.Footer -> { v -> footer = v }
    }

    BackHandler { persist(); onBack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { persist(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "تراجع")
                    }
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "إعادة")
                    }
                    IconButton(onClick = { persist() }) { Icon(Icons.Default.Save, contentDescription = "حفظ") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
        ) {
            // شريط التبويبات (Ribbon) — يتبع اتجاه الصفحة
            CompositionLocalProviderDir(page.rtlPage) {
                EditorRibbon(
                    tab = ribbonTab,
                    onTab = { ribbonTab = it },
                    value = activeValue,
                    onChange = activeOnChange,
                    page = page,
                    wordCount = wordCount(value.text),
                    showRuler = showRuler,
                    onToggleRuler = { showRuler = !showRuler },
                    onTogglePageNumber = { page = page.copy(showPageNumber = !page.showPageNumber) },
                    onFindReplace = { showFindReplace = true; focusTarget = EditField.Body },
                    onOpen = { openLauncher.launch(arrayOf(MIME_DOCX, "application/msword", "*/*")) },
                    onSave = { persist() },
                    onSaveAs = { showSaveAs = true },
                    onExportPdf = { pdfLauncher.launch("${title.ifBlank { "مستند" }}.pdf") },
                    onPageSetup = { showPageSetup = true },
                )
                if (showFindReplace) {
                    FindReplaceBar(
                        find = findQuery,
                        onFind = { findQuery = it },
                        replace = replaceQuery,
                        onReplace = { replaceQuery = it },
                        onNext = { value = RichTextOps.findNext(value, findQuery) },
                        onReplaceOne = { value = RichTextOps.replaceCurrent(value, findQuery, replaceQuery) },
                        onReplaceAll = {
                            val (nv, count) = RichTextOps.replaceAll(value, findQuery, replaceQuery)
                            update(nv)
                            viewModel.notify("تم استبدال $count")
                        },
                        onClose = { showFindReplace = false },
                    )
                }
            }

            if (showPageSetup) {
                PageSetupDialog(
                    page = page,
                    onDismiss = { showPageSetup = false },
                    onApply = { newPage -> page = newPage; showPageSetup = false },
                )
            }

            if (showSaveAs) {
                val base = title.ifBlank { "مستند" }
                SaveAsDialog(
                    onDismiss = { showSaveAs = false },
                    onPick = { fmt ->
                        showSaveAs = false
                        when (fmt) {
                            SaveFormat.DOCX -> exportLauncher.launch("$base.docx")
                            SaveFormat.PDF -> pdfLauncher.launch("$base.pdf")
                            SaveFormat.TXT -> txtLauncher.launch("$base.txt")
                        }
                    },
                )
            }

            CompositionLocalProviderDir(page.rtlPage) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                ) {
                    val rulerThick = if (showRuler) 22.dp else 0.dp
                    val gap = if (showRuler) 2.dp else 0.dp
                    val density = LocalDensity.current.density
                    val sheetWidthDp = maxWidth - rulerThick - gap
                    val scale = sheetWidthDp.value / page.pageWidthPt
                    val pageHeightDp = page.pageHeightPt * scale
                    // ارتفاع المحتوى الفعلي وعدد الصفحات
                    val sheetHeightDp = if (sheetHeightPx > 0) sheetHeightPx / density else pageHeightDp
                    val pageCount = paginationPageCount(sheetHeightDp, pageHeightDp)

                    // اتجاه المسطرة يتبع اتجاه الفقرة الحالية للمتن (تلقائي ← اتجاه الصفحة)
                    val rulerRtl = when (RichTextOps.currentDirection(value)) {
                        TextDirection.Rtl -> true
                        TextDirection.Ltr -> false
                        else -> page.rtlPage
                    }
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        if (showRuler) {
                            Row {
                                Spacer(Modifier.width(rulerThick + gap))
                                HorizontalRuler(
                                    pageWidthPt = page.pageWidthPt,
                                    marginLeftPt = page.marginLeftPt,
                                    marginRightPt = page.marginRightPt,
                                    scale = scale,
                                    rtl = rulerRtl,
                                    onChange = { l, r -> page = page.copy(marginLeftPt = l, marginRightPt = r) },
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                        Row {
                            if (showRuler) {
                                VerticalRuler(
                                    pageHeightPt = page.pageHeightPt,
                                    heightDp = sheetHeightDp,
                                    marginTopPt = page.marginTopPt,
                                    marginBottomPt = page.marginBottomPt,
                                    scale = scale,
                                    onChange = { t, b -> page = page.copy(marginTopPt = t, marginBottomPt = b) },
                                )
                                Spacer(Modifier.width(gap))
                            }
                            PageSheet(
                                widthDp = sheetWidthDp,
                                pageHeightDp = pageHeightDp,
                                pageCount = pageCount,
                                page = page,
                                scale = scale,
                                value = value,
                                onValueChange = { update(it) },
                                header = header,
                                onHeaderChange = { header = it },
                                footer = footer,
                                onFooterChange = { footer = it },
                                hfEditing = hfEditing,
                                onStartEditHF = { field -> hfEditing = field; focusTarget = field },
                                onBodyFocus = { focusTarget = EditField.Body; hfEditing = null },
                                onSheetHeight = { sheetHeightPx = it },
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompositionLocalProviderDir(rtl: Boolean, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}

@Composable
private fun PageSheet(
    widthDp: androidx.compose.ui.unit.Dp,
    pageHeightDp: Float,
    pageCount: Int,
    page: PageSettings,
    scale: Float,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    header: TextFieldValue,
    onHeaderChange: (TextFieldValue) -> Unit,
    footer: TextFieldValue,
    onFooterChange: (TextFieldValue) -> Unit,
    hfEditing: EditField?,
    onStartEditHF: (EditField) -> Unit,
    onBodyFocus: () -> Unit,
    onSheetHeight: (Int) -> Unit,
) {
    val mlDp = page.marginLeftPt * scale
    val mrDp = page.marginRightPt * scale
    val mtDp = page.marginTopPt * scale
    val mbDp = page.marginBottomPt * scale
    val bodyMin = (pageHeightDp - mtDp - mbDp).coerceAtLeast(80f)

    val density = LocalDensity.current.density
    val dashColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier
            .width(widthDp)
            .shadow(3.dp)
            .background(Color.White)
            .onSizeChanged { onSheetHeight(it.height) }
            .drawWithContent {
                drawContent()
                val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
                // خطوط توضيح الترويسة (تحت الهامش العلوي) والتذييل (فوق الهامش السفلي)
                val topY = mtDp * density
                val botY = size.height - mbDp * density
                drawLine(dashColor, Offset(mlDp * density, topY), Offset(size.width - mrDp * density, topY), strokeWidth = 1f, pathEffect = dash)
                if (botY > topY) {
                    drawLine(dashColor, Offset(mlDp * density, botY), Offset(size.width - mrDp * density, botY), strokeWidth = 1f, pathEffect = dash)
                }
                // فواصل صفحات ثلاثية الأبعاد (ظل + فجوة) مع شارة «صفحة X من Y»
                val pageH = pageHeightDp * density
                val totalLabel = arabicDigits(pageCount)
                val chip = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 10f * density
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                for (p in 1 until pageCount) {
                    val y = p * pageH
                    if (y >= size.height) break
                    // ظل أسفل الصفحة العليا
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x40000000)),
                            startY = y - 9f * density, endY = y,
                        ),
                        topLeft = Offset(0f, y - 9f * density),
                        size = Size(size.width, 9f * density),
                    )
                    drawLine(Color(0x55000000), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.2f * density)
                    // إضاءة أعلى الصفحة السفلى
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0x18000000), Color.Transparent),
                            startY = y, endY = y + 7f * density,
                        ),
                        topLeft = Offset(0f, y),
                        size = Size(size.width, 7f * density),
                    )
                    // شارة رقم الصفحة
                    val label = "صفحة ${arabicDigits(p + 1)} من $totalLabel"
                    val tw = chip.measureText(label)
                    val chipW = tw + 16f * density
                    val chipH = 16f * density
                    val cx = size.width / 2f
                    drawRoundRect(
                        color = Color(0xCC424242),
                        topLeft = Offset(cx - chipW / 2f, y - chipH - 2f * density),
                        size = Size(chipW, chipH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(chipH / 2f, chipH / 2f),
                    )
                    drawContext.canvas.nativeCanvas.drawText(label, cx, y - 6f * density, chip)
                }
            },
    ) {
        // منطقة الهامش العلوي + الترويسة
        Box(
            Modifier
                .fillMaxWidth()
                .height(mtDp.dp)
                .absolutePadding(left = mlDp.dp, right = mrDp.dp, bottom = 2.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            HeaderFooterField(
                value = header,
                onChange = onHeaderChange,
                placeholder = "الترويسة (نقر مزدوج للتحرير)",
                editing = hfEditing == EditField.Header,
                onStartEditing = { onStartEditHF(EditField.Header) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // المتن
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = bodyMin.dp)
                .absolutePadding(left = mlDp.dp, right = mrDp.dp)
                .onFocusChanged { if (it.isFocused) onBodyFocus() },
            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF1A1A1A)),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (value.text.isEmpty()) {
                    Text("اكتب مستندك هنا…", color = Color(0xFFBBBBBB), textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                }
                inner()
            },
        )
        // منطقة الهامش السفلي + التذييل + رقم الصفحة
        Box(
            Modifier
                .fillMaxWidth()
                .height(mbDp.dp)
                .absolutePadding(left = mlDp.dp, right = mrDp.dp, top = 2.dp, bottom = 2.dp),
        ) {
            HeaderFooterField(
                value = footer,
                onChange = onFooterChange,
                placeholder = "التذييل (نقر مزدوج للتحرير)",
                editing = hfEditing == EditField.Footer,
                onStartEditing = { onStartEditHF(EditField.Footer) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            if (page.showPageNumber) {
                Text(
                    "صفحة ١ من ${arabicDigits(pageCount)}",
                    color = Color(0xFF555555),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                )
            }
        }
    }
}

/** يحوّل عدداً إلى أرقام عربية للعرض. */
fun arabicDigits(n: Int): String {
    val ar = "٠١٢٣٤٥٦٧٨٩"
    return n.toString().map { if (it in '0'..'9') ar[it - '0'] else it }.joinToString("")
}

/** الحقل القابل للتنسيق: المتن أو الترويسة أو التذييل. */
enum class EditField { Body, Header, Footer }

/**
 * حقل ترويسة/تذييل: مقفل وخافت افتراضياً، ولا يُحرَّر إلا بنقر مزدوج (مثل Word/WPS).
 * عند التحرير يخضع لأدوات التنسيق (لأنه يصبح الحقل المركّز).
 */
@Composable
private fun HeaderFooterField(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    placeholder: String,
    editing: Boolean,
    onStartEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (editing) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF444444), textAlign = TextAlign.Right),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (value.text.isEmpty()) {
                    Text(placeholder, color = Color(0xFFCCCCCC), fontSize = 13.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                }
                inner()
            },
        )
        return
    }
    // عرض مقفل: نقر مزدوج للتحرير
    Box(
        modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onStartEditing() }) },
    ) {
        if (value.text.isEmpty()) {
            Text(placeholder, color = Color(0xFFCCCCCC), fontSize = 13.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
        } else {
            Text(value.annotatedString, fontSize = 13.sp, color = Color(0xFF777777), textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
        }
    }
}

enum class SaveFormat(val label: String, val ext: String) {
    DOCX("Word مستند (DOCX)", "docx"),
    PDF("PDF", "pdf"),
    TXT("نص عادي (TXT)", "txt"),
}

@Composable
private fun SaveAsDialog(onDismiss: () -> Unit, onPick: (SaveFormat) -> Unit) {
    var fmt by remember { mutableStateOf(SaveFormat.DOCX) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حفظ باسم — اختر الصيغة") },
        text = {
            Column {
                SaveFormat.entries.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clickable { fmt = f }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = fmt == f, onClick = { fmt = f })
                        Text(f.label, Modifier.padding(start = 4.dp))
                    }
                }
                Text(
                    "ملاحظة: صيغة DOC الثنائية القديمة غير مدعومة — استخدم DOCX.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onPick(fmt) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

/** شريط البحث والاستبدال. */
@Composable
private fun FindReplaceBar(
    find: String,
    onFind: (String) -> Unit,
    replace: String,
    onReplace: (String) -> Unit,
    onNext: () -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = find,
                onValueChange = onFind,
                placeholder = { Text("بحث") },
                singleLine = true,
                modifier = Modifier.width(170.dp),
            )
            IconButton(onClick = onNext) { Icon(Icons.Default.Search, contentDescription = "التالي") }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = replace,
                onValueChange = onReplace,
                placeholder = { Text("استبدال بـ") },
                singleLine = true,
                modifier = Modifier.width(170.dp),
            )
            TextButton(onClick = onReplaceOne) { Text("استبدال") }
            TextButton(onClick = onReplaceAll) { Text("الكل") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
        }
    }
}

/** تبويبات الشريط (Ribbon) بنمط WPS. */
enum class RibbonTab { File, Home, Insert, View, Review }

/** عدد الكلمات في النص. */
fun wordCount(text: String): Int =
    text.split(Regex("\\s+")).count { it.isNotBlank() }

@Composable
private fun EditorRibbon(
    tab: RibbonTab,
    onTab: (RibbonTab) -> Unit,
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    page: PageSettings,
    wordCount: Int,
    showRuler: Boolean,
    onToggleRuler: () -> Unit,
    onTogglePageNumber: () -> Unit,
    onFindReplace: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onExportPdf: () -> Unit,
    onPageSetup: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        // صف التبويبات
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RibbonTabButton("ملف", tab == RibbonTab.File) { onTab(RibbonTab.File) }
            RibbonTabButton("الصفحة الرئيسية", tab == RibbonTab.Home) { onTab(RibbonTab.Home) }
            RibbonTabButton("إدراج", tab == RibbonTab.Insert) { onTab(RibbonTab.Insert) }
            RibbonTabButton("عرض", tab == RibbonTab.View) { onTab(RibbonTab.View) }
            RibbonTabButton("مراجعة", tab == RibbonTab.Review) { onTab(RibbonTab.Review) }
        }
        HorizontalDivider()
        // أدوات التبويب المختار
        when (tab) {
            RibbonTab.Home -> FormatToolbar(value, onChange)
            RibbonTab.File -> Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            ) {
                RibbonButton(Icons.Default.FolderOpen, "فتح", onClick = onOpen)
                RibbonButton(Icons.Default.Save, "حفظ", onClick = onSave)
                RibbonButton(Icons.Default.SaveAs, "حفظ باسم", onClick = onSaveAs)
                RibbonButton(Icons.Default.PictureAsPdf, "PDF", onClick = onExportPdf)
                ToolDivider()
                RibbonButton(Icons.Default.AspectRatio, "إعداد الصفحة", onClick = onPageSetup)
            }
            RibbonTab.Insert -> Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            ) {
                RibbonButton(Icons.Default.Numbers, "رقم الصفحة", active = page.showPageNumber, onClick = onTogglePageNumber)
                RibbonButton(Icons.Default.TableChart, "جدول (قريباً)", enabled = false) {}
                RibbonButton(Icons.Default.Image, "صورة (قريباً)", enabled = false) {}
            }
            RibbonTab.View -> Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            ) {
                RibbonButton(Icons.Default.Straighten, if (showRuler) "إخفاء المسطرة" else "إظهار المسطرة", active = showRuler, onClick = onToggleRuler)
            }
            RibbonTab.Review -> Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RibbonButton(Icons.Default.Search, "إيجاد واستبدال", onClick = onFindReplace)
                ToolDivider()
                Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("  عدد الكلمات: $wordCount", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun RibbonTabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = color, style = MaterialTheme.typography.titleSmall)
        Box(
            Modifier
                .padding(top = 4.dp)
                .height(2.dp)
                .width(if (selected) 24.dp else 0.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun RibbonButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

@Composable
private fun PageSetupDialog(
    page: PageSettings,
    onDismiss: () -> Unit,
    onApply: (PageSettings) -> Unit,
) {
    var sizeId by remember { mutableStateOf(page.currentPresetId()) }
    var landscape by remember { mutableStateOf(page.isLandscape()) }
    var rtlPage by remember { mutableStateOf(page.rtlPage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعداد الصفحة") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("حجم الصفحة", style = MaterialTheme.typography.labelLarge)
                PAGE_SIZES.forEach { preset ->
                    Row(
                        Modifier.fillMaxWidth().clickable { sizeId = preset.id }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = sizeId == preset.id, onClick = { sizeId = preset.id })
                        Text(preset.label, Modifier.padding(start = 4.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("توجيه أفقي (Landscape)", Modifier.weight(1f))
                    Switch(checked = landscape, onCheckedChange = { landscape = it })
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("اتجاه الصفحة عربي (من اليمين)", Modifier.weight(1f))
                    Switch(checked = rtlPage, onCheckedChange = { rtlPage = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(page.withSize(pageSizeById(sizeId), landscape).copy(rtlPage = rtlPage))
            }) { Text("تطبيق") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

private val FONT_SIZES = listOf(10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72)

private val LINE_SPACINGS = listOf(
    1.0f to "مفرد (١٫٠)",
    1.15f to "١٫١٥",
    1.5f to "١٫٥",
    2.0f to "مزدوج (٢٫٠)",
)

private val BULLET_STYLES = listOf(
    "●  نقطة" to RichTextOps.ListSpec(numbered = false, glyph = "•"),
    "■  مربّع" to RichTextOps.ListSpec(numbered = false, glyph = "▪"),
    "◆  معيّن" to RichTextOps.ListSpec(numbered = false, glyph = "◆"),
    "✤  زهرة" to RichTextOps.ListSpec(numbered = false, glyph = "✤"),
    "➢  سهم" to RichTextOps.ListSpec(numbered = false, glyph = "➢"),
    "✔  صح" to RichTextOps.ListSpec(numbered = false, glyph = "✔"),
    "✧  نجمة" to RichTextOps.ListSpec(numbered = false, glyph = "✧"),
    "◦  دائرة مفرغة" to RichTextOps.ListSpec(numbered = false, glyph = "◦"),
    "−  شرطة" to RichTextOps.ListSpec(numbered = false, glyph = "-"),
)

private val NUMBER_STYLES = listOf(
    "١.  عربي نقطة" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.DECIMAL, sep = "."),
    "١)  عربي قوس" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.DECIMAL, sep = ")"),
    "(١) عربي قوسان" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.DECIMAL, sep = ")", wrap = true),
    "١-  عربي شرطة" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.DECIMAL, sep = "-"),
    "أ.  حروف عربية" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.ARABIC_ALPHA, sep = "."),
    "A.  لاتيني كبير" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.UPPER_ALPHA, sep = "."),
    "a)  لاتيني صغير" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.LOWER_ALPHA, sep = ")"),
    "I.  روماني كبير" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.UPPER_ROMAN, sep = "."),
    "i.  روماني صغير" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.LOWER_ROMAN, sep = "."),
    "١.    مسافة واسعة" to RichTextOps.ListSpec(numbered = true, numType = RichTextOps.NumType.DECIMAL, sep = ".", spaces = 3),
)

@Composable
private fun FormatToolbar(value: TextFieldValue, onChange: (TextFieldValue) -> Unit) {
    val cur = RichTextOps.currentAttrs(value)
    val curAlign = RichTextOps.currentAlign(value)
    val curDir = RichTextOps.currentDirection(value)
    val curSpacing = RichTextOps.currentLineSpacing(value)

    var fontMenu by remember { mutableStateOf(false) }
    var sizeMenu by remember { mutableStateOf(false) }
    var colorMenu by remember { mutableStateOf(false) }
    var alignMenu by remember { mutableStateOf(false) }
    var dirMenu by remember { mutableStateOf(false) }
    var spacingMenu by remember { mutableStateOf(false) }
    var listDialog by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolToggle(Icons.Default.FormatBold, "غامق", cur.bold) { onChange(RichTextOps.toggleBold(value)) }
        ToolToggle(Icons.Default.FormatItalic, "مائل", cur.italic) { onChange(RichTextOps.toggleItalic(value)) }
        ToolToggle(Icons.Default.FormatUnderlined, "تسطير", cur.underline) { onChange(RichTextOps.toggleUnderline(value)) }
        ToolDivider()

        // عائلة الخط (قائمة)
        Box {
            Row(
                Modifier.clickable { fontMenu = true }.padding(horizontal = 6.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.FontDownload, "الخط", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" ${FONT_FAMILY_NAMES[cur.fontFamily] ?: "افتراضي"} ", style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                FONT_FAMILY_NAMES.forEach { (code, name) ->
                    MenuChoice(name, Icons.Default.FontDownload, cur.fontFamily == code) {
                        onChange(RichTextOps.setFontFamily(value, code)); fontMenu = false
                    }
                }
            }
        }
        ToolDivider()

        // حجم الخط (قائمة)
        Box {
            Row(
                Modifier.clickable { sizeMenu = true }.padding(horizontal = 6.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.FormatSize, "حجم الخط", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" ${cur.sizeSp} ", style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = sizeMenu, onDismissRequest = { sizeMenu = false }) {
                FONT_SIZES.forEach { sz ->
                    DropdownMenuItem(text = { Text("$sz") }, onClick = { onChange(RichTextOps.setSize(value, sz)); sizeMenu = false })
                }
            }
        }
        ToolDivider()

        // لون النص (قائمة)
        Box {
            IconButton(onClick = { colorMenu = true }) {
                Icon(Icons.Default.FormatColorText, "لون النص", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = colorMenu, onDismissRequest = { colorMenu = false }) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    SWATCHES.forEach { argb ->
                        ColorSwatch(Color(argb)) { onChange(RichTextOps.setColor(value, argb)); colorMenu = false }
                    }
                }
            }
        }
        ToolDivider()

        // المحاذاة (قائمة واحدة)
        Box {
            IconButton(onClick = { alignMenu = true }) {
                Icon(alignIcon(curAlign), "المحاذاة", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = alignMenu, onDismissRequest = { alignMenu = false }) {
                MenuChoice("تلقائي حسب اللغة", Icons.Default.Language, curAlign == TextAlign.Start) { onChange(RichTextOps.setAlign(value, TextAlign.Start)); alignMenu = false }
                MenuChoice("يمين", Icons.AutoMirrored.Filled.FormatAlignRight, curAlign == TextAlign.Right) { onChange(RichTextOps.setAlign(value, TextAlign.Right)); alignMenu = false }
                MenuChoice("توسيط", Icons.Default.FormatAlignCenter, curAlign == TextAlign.Center) { onChange(RichTextOps.setAlign(value, TextAlign.Center)); alignMenu = false }
                MenuChoice("يسار", Icons.AutoMirrored.Filled.FormatAlignLeft, curAlign == TextAlign.Left) { onChange(RichTextOps.setAlign(value, TextAlign.Left)); alignMenu = false }
                MenuChoice("ضبط", Icons.Default.FormatAlignJustify, curAlign == TextAlign.Justify) { onChange(RichTextOps.setAlign(value, TextAlign.Justify)); alignMenu = false }
            }
        }

        // اتجاه الأسطر RTL/LTR (قائمة واحدة) — للفقرات فقط
        Box {
            IconButton(onClick = { dirMenu = true }) {
                Icon(dirIcon(curDir), "اتجاه الأسطر", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = dirMenu, onDismissRequest = { dirMenu = false }) {
                MenuChoice("تلقائي", Icons.Default.Language, curDir == TextDirection.Content) { onChange(RichTextOps.setDirection(value, TextDirection.Content)); dirMenu = false }
                MenuChoice("عربي ← (RTL)", Icons.AutoMirrored.Filled.FormatTextdirectionRToL, curDir == TextDirection.Rtl) { onChange(RichTextOps.setDirection(value, TextDirection.Rtl)); dirMenu = false }
                MenuChoice("لاتيني → (LTR)", Icons.AutoMirrored.Filled.FormatTextdirectionLToR, curDir == TextDirection.Ltr) { onChange(RichTextOps.setDirection(value, TextDirection.Ltr)); dirMenu = false }
            }
        }
        ToolDivider()

        // تباعد الأسطر (قائمة)
        Box {
            IconButton(onClick = { spacingMenu = true }) {
                Icon(Icons.Default.FormatLineSpacing, "تباعد الأسطر", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = spacingMenu, onDismissRequest = { spacingMenu = false }) {
                LINE_SPACINGS.forEach { (mult, label) ->
                    MenuChoice(label, Icons.Default.FormatLineSpacing, kotlin.math.abs(curSpacing - mult) < 0.01f) {
                        onChange(RichTextOps.setLineSpacing(value, mult)); spacingMenu = false
                    }
                }
            }
        }

        // المسافة البادئة (زيادة/إنقاص)
        ToolToggle(Icons.AutoMirrored.Filled.FormatIndentIncrease, "زيادة المسافة البادئة", false) {
            onChange(RichTextOps.changeIndent(value, +1))
        }
        ToolToggle(Icons.AutoMirrored.Filled.FormatIndentDecrease, "إنقاص المسافة البادئة", false) {
            onChange(RichTextOps.changeIndent(value, -1))
        }
        ToolDivider()

        // النقاط والترقيم — مربّع اختيار مدمج (شبكة معاينات)
        ToolToggle(Icons.AutoMirrored.Filled.FormatListBulleted, "النقاط والترقيم", false) { listDialog = true }
        ToolDivider()

        // المزيد (تظليل / يتوسطه خط)
        Box {
            IconButton(onClick = { moreMenu = true }) {
                Icon(Icons.Default.MoreHoriz, "المزيد", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                MenuChoice("يتوسطه خط", Icons.Default.StrikethroughS, cur.strike) { onChange(RichTextOps.toggleStrike(value)); moreMenu = false }
                MenuChoice("تظليل أصفر", Icons.Default.FormatColorFill, cur.highlightArgb != 0) { onChange(RichTextOps.setHighlight(value, 0xFFFFEB3B.toInt())); moreMenu = false }
                MenuChoice("إزالة التظليل", Icons.Default.FormatColorReset, false) { onChange(RichTextOps.setHighlight(value, 0)); moreMenu = false }
            }
        }
    }

    if (listDialog) {
        ListPickerDialog(
            onPick = { spec -> onChange(RichTextOps.applyList(value, spec)); listDialog = false },
            onDismiss = { listDialog = false },
        )
    }
}

/** مربّع اختيار النقاط والترقيم — شبكة معاينات مدمجة (مثل WPS). */
@Composable
private fun ListPickerDialog(
    onPick: (RichTextOps.ListSpec?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(null) }) { Text("بلا قائمة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        title = { Text("النقاط والترقيم") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("نقاط", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                ListStyleGrid(BULLET_STYLES.map { it.second }, onPick)
                Spacer(Modifier.height(12.dp))
                Text("ترقيم", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                ListStyleGrid(NUMBER_STYLES.map { it.second }, onPick)
            }
        },
    )
}

@Composable
private fun ListStyleGrid(specs: List<RichTextOps.ListSpec>, onPick: (RichTextOps.ListSpec?) -> Unit) {
    Column {
        specs.chunked(4).forEach { row ->
            Row {
                row.forEach { spec ->
                    ListStyleCell(spec) { onPick(spec) }
                }
            }
        }
    }
}

@Composable
private fun ListStyleCell(spec: RichTextOps.ListSpec, onClick: () -> Unit) {
    val lines = if (spec.numbered) {
        (1..3).map { RichTextOps.previewMarker(spec, it).trimEnd() }
    } else {
        List(3) { spec.glyph }
    }
    Box(
        Modifier
            .padding(3.dp)
            .size(width = 60.dp, height = 52.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            lines.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                    Box(
                        Modifier
                            .padding(start = 3.dp)
                            .width(22.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }
}

private fun alignIcon(a: TextAlign) = when (a) {
    TextAlign.Center -> Icons.Default.FormatAlignCenter
    TextAlign.Left -> Icons.AutoMirrored.Filled.FormatAlignLeft
    TextAlign.Right -> Icons.AutoMirrored.Filled.FormatAlignRight
    TextAlign.Justify -> Icons.Default.FormatAlignJustify
    else -> Icons.AutoMirrored.Filled.FormatAlignRight // تلقائي (أيقونة محاذاة لا كرة أرضية)
}

private fun dirIcon(d: TextDirection) = when (d) {
    TextDirection.Rtl -> Icons.AutoMirrored.Filled.FormatTextdirectionRToL
    TextDirection.Ltr -> Icons.AutoMirrored.Filled.FormatTextdirectionLToR
    else -> Icons.Default.Language
}

@Composable
private fun MenuChoice(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Icon(icon, null, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = { if (active) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
        onClick = onClick,
    )
}

@Composable
private fun ToolToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = onClick) { Icon(icon, contentDescription = desc, tint = tint) }
}

@Composable
private fun ToolDivider() {
    Spacer(Modifier.width(4.dp))
    Box(Modifier.size(width = 1.dp, height = 24.dp).background(MaterialTheme.colorScheme.outlineVariant))
    Spacer(Modifier.width(4.dp))
}

@Composable
private fun ColorSwatch(color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .size(24.dp)
            .background(color, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
    )
}
