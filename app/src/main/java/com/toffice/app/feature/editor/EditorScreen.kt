package com.toffice.app.feature.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toffice.app.feature.editor.model.DocBundle
import com.toffice.app.feature.editor.model.DocSerializer
import com.toffice.app.feature.editor.model.PAGE_SIZES
import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.RichTextOps
import com.toffice.app.feature.editor.model.currentPresetId
import com.toffice.app.feature.editor.model.isLandscape
import com.toffice.app.feature.editor.model.pageSizeById
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
    var header by remember { mutableStateOf("") }
    var footer by remember { mutableStateOf("") }

    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    fun update(new: TextFieldValue) {
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
            header = bundle.header
            footer = bundle.footer
            initialized = true
        }
    }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    var showMenu by remember { mutableStateOf(false) }
    var showPageSetup by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MIME_DOCX)
    ) { uri ->
        if (uri != null) viewModel.exportDocx(uri, value.annotatedString, page, header, footer)
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) viewModel.exportPdf(uri, value.annotatedString, page, header, footer)
    }

    fun persist() {
        if (initialized) {
            val json = DocSerializer.serialize(DocBundle(value.annotatedString, page, header, footer))
            viewModel.save(title, json, value.annotatedString, page, header, footer)
        }
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
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "المزيد")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                        DropdownMenuItem(
                            text = { Text("تصدير Word (DOCX)") },
                            leadingIcon = { Icon(Icons.Default.Upload, null) },
                            onClick = { showMenu = false; exportLauncher.launch("${title.ifBlank { "مستند" }}.docx") },
                        )
                        DropdownMenuItem(
                            text = { Text("تصدير PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                            onClick = { showMenu = false; pdfLauncher.launch("${title.ifBlank { "مستند" }}.pdf") },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            FormatToolbar(value = value, onChange = { update(it) })

            if (showPageSetup) {
                PageSetupDialog(
                    page = page,
                    onDismiss = { showPageSetup = false },
                    onApply = { newPage -> page = newPage; showPageSetup = false },
                )
            }

            CompositionLocalProviderLtr {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                ) {
                    val rulerThick = 22.dp
                    val gap = 2.dp
                    val sheetWidthDp = maxWidth - rulerThick - gap
                    val scale = sheetWidthDp.value / page.pageWidthPt
                    val pageHeightDp = page.pageHeightPt * scale

                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Row {
                            Spacer(Modifier.width(rulerThick + gap))
                            HorizontalRuler(
                                pageWidthPt = page.pageWidthPt,
                                marginLeftPt = page.marginLeftPt,
                                marginRightPt = page.marginRightPt,
                                scale = scale,
                                onChange = { l, r -> page = page.copy(marginLeftPt = l, marginRightPt = r) },
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Row {
                            VerticalRuler(
                                pageHeightPt = page.pageHeightPt,
                                heightDp = pageHeightDp,
                                marginTopPt = page.marginTopPt,
                                marginBottomPt = page.marginBottomPt,
                                scale = scale,
                                onChange = { t, b -> page = page.copy(marginTopPt = t, marginBottomPt = b) },
                            )
                            Spacer(Modifier.width(gap))
                            PageSheet(
                                widthDp = sheetWidthDp,
                                pageHeightDp = pageHeightDp,
                                page = page,
                                scale = scale,
                                value = value,
                                onValueChange = { update(it) },
                                header = header,
                                onHeaderChange = { header = it },
                                footer = footer,
                                onFooterChange = { footer = it },
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
private fun CompositionLocalProviderLtr(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr, content = content,
    )
}

@Composable
private fun PageSheet(
    widthDp: androidx.compose.ui.unit.Dp,
    pageHeightDp: Float,
    page: PageSettings,
    scale: Float,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    header: String,
    onHeaderChange: (String) -> Unit,
    footer: String,
    onFooterChange: (String) -> Unit,
) {
    val mlDp = page.marginLeftPt * scale
    val mrDp = page.marginRightPt * scale
    val mtDp = page.marginTopPt * scale
    val mbDp = page.marginBottomPt * scale
    val bodyMin = (pageHeightDp - mtDp - mbDp).coerceAtLeast(80f)

    Column(
        Modifier
            .width(widthDp)
            .shadow(3.dp)
            .background(Color.White),
    ) {
        // منطقة الهامش العلوي + الترويسة
        Box(
            Modifier
                .fillMaxWidth()
                .height(mtDp.dp)
                .padding(start = mlDp.dp, end = mrDp.dp, bottom = 2.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            PlainEditField(header, onHeaderChange, "الترويسة", Color(0xFF888888), Modifier.fillMaxWidth())
        }
        // المتن
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = bodyMin.dp)
                .padding(start = mlDp.dp, end = mrDp.dp),
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
                .padding(start = mlDp.dp, end = mrDp.dp, top = 2.dp, bottom = 2.dp),
        ) {
            PlainEditField(footer, onFooterChange, "التذييل", Color(0xFF888888), Modifier.align(Alignment.TopCenter))
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

@Composable
private fun PlainEditField(value: String, onChange: (String) -> Unit, placeholder: String, color: Color, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 12.sp, color = color, textAlign = TextAlign.Right),
        cursorBrush = SolidColor(color),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = Color(0xFFCCCCCC), fontSize = 12.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            }
            inner()
        },
    )
}

@Composable
private fun PageSetupDialog(
    page: PageSettings,
    onDismiss: () -> Unit,
    onApply: (PageSettings) -> Unit,
) {
    var sizeId by remember { mutableStateOf(page.currentPresetId()) }
    var landscape by remember { mutableStateOf(page.isLandscape()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعداد الصفحة") },
        text = {
            Column {
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
                    Text("اتجاه أفقي", Modifier.weight(1f))
                    Switch(checked = landscape, onCheckedChange = { landscape = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(page.withSize(pageSizeById(sizeId), landscape)) }) { Text("تطبيق") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun FormatToolbar(value: TextFieldValue, onChange: (TextFieldValue) -> Unit) {
    val cur = RichTextOps.currentAttrs(value)
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
        ToolToggle(Icons.Default.StrikethroughS, "يتوسطه خط", cur.strike) { onChange(RichTextOps.toggleStrike(value)) }
        ToolDivider()
        ToolButton(Icons.Default.Language, "محاذاة تلقائية حسب اللغة") { onChange(RichTextOps.setAlign(value, TextAlign.Start)) }
        ToolButton(Icons.AutoMirrored.Filled.FormatAlignRight, "يمين") { onChange(RichTextOps.setAlign(value, TextAlign.Right)) }
        ToolButton(Icons.Default.FormatAlignCenter, "توسيط") { onChange(RichTextOps.setAlign(value, TextAlign.Center)) }
        ToolButton(Icons.AutoMirrored.Filled.FormatAlignLeft, "يسار") { onChange(RichTextOps.setAlign(value, TextAlign.Left)) }
        ToolButton(Icons.Default.FormatAlignJustify, "ضبط") { onChange(RichTextOps.setAlign(value, TextAlign.Justify)) }
        ToolDivider()
        ToolButton(Icons.Default.Remove, "تصغير") { onChange(RichTextOps.setSize(value, cur.sizeSp - 2)) }
        Text("${cur.sizeSp}", style = MaterialTheme.typography.labelLarge)
        ToolButton(Icons.Default.Add, "تكبير") { onChange(RichTextOps.setSize(value, cur.sizeSp + 2)) }
        ToolDivider()
        SWATCHES.forEach { argb -> ColorSwatch(Color(argb)) { onChange(RichTextOps.setColor(value, argb)) } }
        ToolDivider()
        ToolButton(Icons.Default.FormatColorFill, "تظليل") { onChange(RichTextOps.setHighlight(value, 0xFFFFEB3B.toInt())) }
        ToolButton(Icons.Default.FormatColorReset, "إزالة التظليل") { onChange(RichTextOps.setHighlight(value, 0)) }
    }
}

@Composable
private fun ToolToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = onClick) { Icon(icon, contentDescription = desc, tint = tint) }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = desc, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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
