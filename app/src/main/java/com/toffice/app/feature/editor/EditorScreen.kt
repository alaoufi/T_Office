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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
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

    // تخطيط متجاوب: التابلت شريط أدوات علوي، الجوال سفلي
    val isCompact = LocalConfiguration.current.screenWidthDp < 600

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
                    // قائمة «ملف» وأدواتها
                    Box {
                        Row(
                            Modifier.clickable { showMenu = true }.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Text(" ملف", style = MaterialTheme.typography.labelLarge)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("فتح ملف…") },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                                onClick = { showMenu = false; openLauncher.launch(arrayOf(MIME_DOCX, "application/msword", "*/*")) },
                            )
                            DropdownMenuItem(
                                text = { Text("حفظ") },
                                leadingIcon = { Icon(Icons.Default.Save, null) },
                                onClick = { showMenu = false; persist() },
                            )
                            DropdownMenuItem(
                                text = { Text("حفظ باسم… (DOCX)") },
                                leadingIcon = { Icon(Icons.Default.SaveAs, null) },
                                onClick = { showMenu = false; exportLauncher.launch("${title.ifBlank { "مستند" }}.docx") },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("إعداد الصفحة") },
                                leadingIcon = { Icon(Icons.Default.AspectRatio, null) },
                                onClick = { showMenu = false; showPageSetup = true },
                            )
                            DropdownMenuItem(
                                text = { Text(if (page.showPageNumber) "إخفاء ترقيم الصفحات" else "إظهار ترقيم الصفحات") },
                                leadingIcon = { Icon(Icons.Default.Numbers, null) },
                                onClick = { page = page.copy(showPageNumber = !page.showPageNumber); showMenu = false },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("تصدير PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                onClick = { showMenu = false; pdfLauncher.launch("${title.ifBlank { "مستند" }}.pdf") },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (isCompact) FormatToolbar(value = activeValue, onChange = activeOnChange)
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
        ) {
            if (!isCompact) FormatToolbar(value = activeValue, onChange = activeOnChange)

            if (showPageSetup) {
                PageSetupDialog(
                    page = page,
                    onDismiss = { showPageSetup = false },
                    onApply = { newPage -> page = newPage; showPageSetup = false },
                )
            }

            CompositionLocalProviderDir(page.rtlPage) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                ) {
                    val rulerThick = 22.dp
                    val gap = 2.dp
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
                        Row {
                            VerticalRuler(
                                pageHeightPt = page.pageHeightPt,
                                heightDp = sheetHeightDp,
                                marginTopPt = page.marginTopPt,
                                marginBottomPt = page.marginBottomPt,
                                scale = scale,
                                onChange = { t, b -> page = page.copy(marginTopPt = t, marginBottomPt = b) },
                            )
                            Spacer(Modifier.width(gap))
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
    val breakColor = MaterialTheme.colorScheme.outline
    Column(
        Modifier
            .width(widthDp)
            .shadow(3.dp)
            .background(Color.White)
            .onSizeChanged { onSheetHeight(it.height) }
            .drawWithContent {
                drawContent()
                // خطوط فاصل الصفحات + رقم الصفحة
                val pageH = pageHeightDp * density
                val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(150, 120, 120, 120)
                    textSize = 10f * density
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                for (p in 1 until pageCount) {
                    val y = p * pageH
                    if (y >= size.height) break
                    drawLine(
                        breakColor,
                        androidx.compose.ui.geometry.Offset(0f, y),
                        androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1f,
                        pathEffect = dash,
                    )
                    drawContext.canvas.nativeCanvas.drawText("صفحة ${p + 1}", size.width / 2f, y - 4f * density, paint)
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
                    "١",
                    color = Color(0xFF555555),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                )
            }
        }
    }
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
    var bulletMenu by remember { mutableStateOf(false) }
    var numberMenu by remember { mutableStateOf(false) }
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

        // القوائم (نقطية / مرقّمة) — بخيارات الفاصل والمسافة
        Box {
            IconButton(onClick = { bulletMenu = true }) {
                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "قائمة نقطية", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = bulletMenu, onDismissRequest = { bulletMenu = false }) {
                BULLET_STYLES.forEach { (label, spec) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { onChange(RichTextOps.applyList(value, spec)); bulletMenu = false })
                }
                MenuChoice("بلا قائمة", Icons.Default.FormatColorReset, false) { onChange(RichTextOps.applyList(value, null)); bulletMenu = false }
            }
        }
        Box {
            IconButton(onClick = { numberMenu = true }) {
                Icon(Icons.Default.FormatListNumbered, "قائمة مرقّمة", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = numberMenu, onDismissRequest = { numberMenu = false }) {
                NUMBER_STYLES.forEach { (label, spec) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { onChange(RichTextOps.applyList(value, spec)); numberMenu = false })
                }
                MenuChoice("بلا قائمة", Icons.Default.FormatColorReset, false) { onChange(RichTextOps.applyList(value, null)); numberMenu = false }
            }
        }
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
