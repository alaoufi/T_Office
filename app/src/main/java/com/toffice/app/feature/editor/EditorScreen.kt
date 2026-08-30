package com.toffice.app.feature.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.toffice.app.feature.editor.io.ImageStore
import com.toffice.app.feature.editor.model.CharAttrs
import com.toffice.app.feature.editor.model.DocBlock
import com.toffice.app.feature.editor.model.DocImage
import com.toffice.app.feature.editor.model.ImageBlock
import com.toffice.app.feature.editor.model.TableBlock
import com.toffice.app.feature.editor.model.TextBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
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
import androidx.compose.material.icons.filled.Subscript
import androidx.compose.material.icons.filled.Superscript
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import com.toffice.app.feature.editor.model.DocBundle
import com.toffice.app.feature.editor.model.DocSerializer
import com.toffice.app.feature.editor.model.FONT_FAMILY_NAMES
import com.toffice.app.feature.editor.model.PAGE_SIZES
import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.RichTextOps
import com.toffice.app.feature.editor.model.TableData
import com.toffice.app.feature.editor.model.TableOps
import com.toffice.app.feature.editor.model.currentPresetId
import com.toffice.app.feature.editor.model.isLandscape
import com.toffice.app.feature.editor.model.pageSizeById
import com.toffice.app.feature.editor.model.paginationPageCount
import com.toffice.app.feature.editor.model.withSize
import com.toffice.app.feature.editor.ui.HorizontalRuler
import com.toffice.app.feature.editor.ui.VerticalRuler

private val SWATCHES = listOf(
    // رماديات
    0xFF000000.toInt(), 0xFF424242.toInt(), 0xFF757575.toInt(), 0xFF9E9E9E.toInt(), 0xFFBDBDBD.toInt(), 0xFFFFFFFF.toInt(),
    // أحمر/وردي
    0xFFB71C1C.toInt(), 0xFFD32F2F.toInt(), 0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFFAD1457.toInt(), 0xFFFF8A80.toInt(),
    // برتقالي/أصفر
    0xFFEF6C00.toInt(), 0xFFFF9800.toInt(), 0xFFFFB300.toInt(), 0xFFFDD835.toInt(), 0xFFFFF176.toInt(), 0xFF795548.toInt(),
    // أخضر
    0xFF1B5E20.toInt(), 0xFF2E7D32.toInt(), 0xFF43A047.toInt(), 0xFF7CB342.toInt(), 0xFF00897B.toInt(), 0xFF00BCD4.toInt(),
    // أزرق/بنفسجي
    0xFF0D47A1.toInt(), 0xFF1565C0.toInt(), 0xFF1E88E5.toInt(), 0xFF3949AB.toInt(), 0xFF6A1B9A.toInt(), 0xFF8E24AA.toInt(),
)

/** ألوان تظليل شائعة (فاتحة). */
private val HIGHLIGHTS = listOf(
    0xFFFFF176.toInt(), 0xFFFFF59D.toInt(), 0xFFA5D6A7.toInt(), 0xFF80DEEA.toInt(),
    0xFF90CAF9.toInt(), 0xFFCE93D8.toInt(), 0xFFFFAB91.toInt(), 0xFFEEEEEE.toInt(),
)

/** حالة كتلة في واجهة المحرّر (بعد المتن الرئيسي): نص قابل للتحرير أو جدول أو صورة. */
sealed interface BlockUi
data class TextBlockUi(val value: TextFieldValue) : BlockUi
data class TableBlockUi(val data: TableData) : BlockUi
data class ImageBlockUi(val img: DocImage) : BlockUi

private fun DocBlock.toUi(): BlockUi = when (this) {
    is TextBlock -> TextBlockUi(TextFieldValue(content))
    is TableBlock -> TableBlockUi(table)
    is ImageBlock -> ImageBlockUi(image)
}

private fun BlockUi.toModel(): DocBlock = when (this) {
    is TextBlockUi -> TextBlock(value.annotatedString)
    is TableBlockUi -> TableBlock(data)
    is ImageBlockUi -> ImageBlock(img)
}

/** يحمّل المستند: المتن الرئيسي = أول كتلة نصية، والباقي كتل بالترتيب. */
/** أقصى طول لحقل نص واحد؛ النص الأكبر يُقسَّم إلى كتل لتفادي تجمّد الحقول الضخمة في Compose. */
internal const val MAX_FIELD_CHARS = 4000

/** يقسّم نصاً منسّقاً كبيراً إلى أجزاء عند حدود الفقرات (مع سقف صارم) للحفاظ على الأداء. */
internal fun splitLargeText(content: androidx.compose.ui.text.AnnotatedString): List<androidx.compose.ui.text.AnnotatedString> {
    if (content.length <= MAX_FIELD_CHARS) return listOf(content)
    val text = content.text
    val parts = mutableListOf<androidx.compose.ui.text.AnnotatedString>()
    var start = 0
    while (start < text.length) {
        var end = minOf(start + MAX_FIELD_CHARS, text.length)
        if (end < text.length) {
            // مدّد حتى نهاية الفقرة التالية دون تجاوز ضعف الحد (تفادي كسر فقرة إن أمكن)
            val hardCap = minOf(start + MAX_FIELD_CHARS * 2, text.length)
            val nl = text.indexOf('\n', end)
            end = if (nl in end until hardCap) nl + 1 else hardCap
        }
        parts.add(content.subSequence(start, end))
        start = end
    }
    return parts
}

private fun loadBlocksInto(bundle: DocBundle, extra: MutableList<BlockUi>, setBody: (TextFieldValue) -> Unit) {
    val eff = bundle.effectiveBlocks()
    extra.clear()
    if (eff.isNotEmpty() && eff[0] is TextBlock) {
        // قسّم المتن الكبير: الجزء الأول متن، والبقية كتل نصية (يمنع الحقل الضخم الواحد)
        val parts = splitLargeText((eff[0] as TextBlock).content)
        setBody(TextFieldValue(parts[0]))
        parts.drop(1).forEach { extra.add(TextBlockUi(TextFieldValue(it))) }
        eff.drop(1).forEach { extra.add(it.toUi()) }
    } else {
        setBody(TextFieldValue(""))
        eff.forEach { extra.add(it.toUi()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
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
    // عند فتح PDF من «فتح ملف» — يُعرض داخل نفس التطبيق
    var pdfUri by remember { mutableStateOf<android.net.Uri?>(null) }
    // كتل ما بعد المتن الرئيسي، بالترتيب (نص/جدول/صورة) — المحرّك الكتلي
    val extraBlocks = remember { mutableStateListOf<BlockUi>() }
    // الكتلة النصية المركّزة ضمن extraBlocks (-1 = المتن الرئيسي)
    var focusedExtra by remember { mutableStateOf(-1) }
    var showInsertTable by remember { mutableStateOf(false) }

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
            // التحليل خارج الخيط الرئيسي حتى لا تتجمّد المستندات كثيرة النص
            val bundle = withContext(Dispatchers.Default) { DocSerializer.parse(ui.json) }
            page = bundle.page
            header = TextFieldValue(bundle.header)
            footer = TextFieldValue(bundle.footer)
            loadBlocksInto(bundle, extraBlocks) { value = it }
            initialized = true
        }
    }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    var showMenu by remember { mutableStateOf(false) }
    var showPageSetup by remember { mutableStateOf(false) }
    var showSaveAs by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var docPages by remember { mutableStateOf(1) }
    var showFormatBar by remember { mutableStateOf(false) } // شريط التنسيق يُفتح عند الطلب
    var openMenu by remember { mutableStateOf<String?>(null) }
    var showRuler by remember { mutableStateOf(true) }
    var showPreview by remember { mutableStateOf(false) }
    var showFindReplace by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }

    // قائمة الكتل الكاملة (للحفظ/التصدير): المتن الرئيسي ثم كتل ما بعده بالترتيب
    fun allBlocks(): List<DocBlock> = buildList {
        add(TextBlock(value.annotatedString))
        extraBlocks.forEach { add(it.toModel()) }
    }

    // إدراج كتلة (جدول/صورة) في موضع المؤشر: يُقسَّم النص المركّز، وما بعده ينتقل لما بعد الكتلة
    fun insertBlock(newBlock: BlockUi) {
        val fe = focusedExtra
        if (fe >= 0 && fe < extraBlocks.size && extraBlocks[fe] is TextBlockUi) {
            val tb = extraBlocks[fe] as TextBlockUi
            val full = tb.value.annotatedString
            val cur = tb.value.selection.start.coerceIn(0, full.length)
            val after = full.subSequence(cur, full.length)
            extraBlocks[fe] = TextBlockUi(TextFieldValue(full.subSequence(0, cur)))
            extraBlocks.add(fe + 1, newBlock)
            if (after.isNotEmpty()) extraBlocks.add(fe + 2, TextBlockUi(TextFieldValue(after)))
        } else if (focusTarget == EditField.Body) {
            val full = value.annotatedString
            val cur = value.selection.start.coerceIn(0, full.length)
            val after = full.subSequence(cur, full.length)
            update(TextFieldValue(full.subSequence(0, cur)))
            extraBlocks.add(0, newBlock)
            if (after.isNotEmpty()) extraBlocks.add(1, TextBlockUi(TextFieldValue(after)))
        } else {
            extraBlocks.add(newBlock)
        }
    }

    val context = LocalContext.current
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
            if (mime.contains("pdf") || uri.toString().lowercase().endsWith(".pdf")) {
                pdfUri = uri // فتح PDF داخل نفس التطبيق (عارض مطابق)
            } else viewModel.openDocx(uri) { newTitle, bundle ->
                title = newTitle
                page = bundle.page
                header = TextFieldValue(bundle.header)
                footer = TextFieldValue(bundle.footer)
                loadBlocksInto(bundle, extraBlocks) { value = it }
                undoStack.clear()
                redoStack.clear()
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MIME_DOCX)
    ) { uri ->
        if (uri != null) viewModel.exportDocx(uri, allBlocks(), page, header.annotatedString, footer.annotatedString)
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
    val imageScope = rememberCoroutineScope()
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) imageScope.launch {
            val img = withContext(Dispatchers.IO) { ImageStore.importImage(context, uri) }
            if (img != null) insertBlock(ImageBlockUi(img)) else viewModel.notify("تعذّر إدراج الصورة")
        }
    }

    fun persist(silent: Boolean = false) {
        if (initialized) {
            val blocks = allBlocks()
            val json = DocSerializer.serialize(DocBundle(value.annotatedString, page, header.annotatedString, footer.annotatedString, blocks = blocks))
            viewModel.save(title, json, blocks, page, header.annotatedString, footer.annotatedString, silent = silent)
        }
    }

    // حفظ تلقائي مُؤجَّل: يحفظ بصمت بعد توقّف الكتابة بثانية ونصف (منع فقد العمل)
    LaunchedEffect(initialized) {
        if (!initialized) return@LaunchedEffect
        snapshotFlow {
            listOf(
                value.annotatedString, header.annotatedString, footer.annotatedString,
                page, title, extraBlocks.toList(),
            )
        }.drop(1).debounce(1500).collect { persist(silent = true) }
    }

    // حفظ فوري عند مغادرة التطبيق/الشاشة (الخلفية) حتى لا يضيع أي تعديل
    val lifecycleOwner = LocalLifecycleOwner.current
    val saveNow by rememberUpdatedState { persist(silent = true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) saveNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // الكتلة النصية المركّزة ضمن extraBlocks (إن وُجدت)
    val focusedExtraText = focusedExtra.takeIf { it in extraBlocks.indices }?.let { extraBlocks[it] as? TextBlockUi }
    // القيمة والتغيير حسب الحقل المركّز (المتن/كتلة نصية/الترويسة/التذييل)
    val activeValue = when (focusTarget) {
        EditField.Header -> header
        EditField.Footer -> footer
        EditField.Body -> focusedExtraText?.value ?: value
    }
    val activeOnChange: (TextFieldValue) -> Unit = when (focusTarget) {
        EditField.Header -> { v -> header = v }
        EditField.Footer -> { v -> footer = v }
        EditField.Body -> { v ->
            if (focusedExtra in extraBlocks.indices && extraBlocks[focusedExtra] is TextBlockUi) {
                extraBlocks[focusedExtra] = TextBlockUi(v)
            } else {
                update(v)
            }
        }
    }

    // أوامر التحرير (قص/نسخ/لصق/تحديد) على الحقل المركّز
    val clipboard = LocalClipboardManager.current
    fun doCopy() {
        val v = activeValue
        val s = minOf(v.selection.start, v.selection.end)
        val e = maxOf(v.selection.start, v.selection.end)
        if (e > s) clipboard.setText(AnnotatedString(v.text.substring(s, e)))
    }
    fun doCut() {
        doCopy()
        if (activeValue.selection.start != activeValue.selection.end) {
            activeOnChange(RichTextOps.replaceSelection(activeValue, ""))
        }
    }
    fun doPaste() {
        val t = clipboard.getText()?.text
        if (!t.isNullOrEmpty()) activeOnChange(RichTextOps.replaceSelection(activeValue, t))
    }
    fun doSelectAll() {
        activeOnChange(activeValue.copy(selection = TextRange(0, activeValue.text.length)))
    }

    BackHandler { persist(); onBack() }

    // عرض PDF داخل نفس التطبيق عند فتحه من «فتح ملف»
    val openPdf = pdfUri
    if (openPdf != null) {
        PdfViewerScreen(uri = openPdf, title = "عارض PDF", onBack = { pdfUri = null })
        return
    }

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
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
        ) {
            // شريط القوائم وشريط التنسيق دائماً RTL (واجهة التطبيق عربية) بغضّ النظر عن اتجاه صفحة المستند
            CompositionLocalProviderDir(rtl = true) {
                Column {
                    EditorMenuBar(
                        openMenu = openMenu,
                        onOpenMenu = { openMenu = it },
                        page = page,
                        showRuler = showRuler,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        value = activeValue,
                        onChange = activeOnChange,
                        onUndo = { undo() },
                        onRedo = { redo() },
                        onCut = { doCut() },
                        onCopy = { doCopy() },
                        onPaste = { doPaste() },
                        onSelectAll = { doSelectAll() },
                        onFindReplace = { showFindReplace = true; focusTarget = EditField.Body },
                        onOpen = { openLauncher.launch(arrayOf(MIME_DOCX, "application/msword", "*/*")) },
                        onSave = { persist() },
                        onSaveAs = { showSaveAs = true },
                        onExportPdf = { pdfLauncher.launch("${title.ifBlank { "مستند" }}.pdf") },
                        onPageSetup = { showPageSetup = true },
                        onTogglePageNumber = { page = page.copy(showPageNumber = !page.showPageNumber) },
                        onToggleRuler = { showRuler = !showRuler },
                        showPreview = showPreview,
                        onTogglePreview = { showPreview = !showPreview },
                        onStats = { showStats = true },
                        formatBarOpen = showFormatBar,
                        onToggleFormatBar = { showFormatBar = !showFormatBar },
                        onDelete = {
                            if (activeValue.selection.start != activeValue.selection.end) {
                                activeOnChange(RichTextOps.replaceSelection(activeValue, ""))
                            }
                        },
                        onInsertText = { activeOnChange(RichTextOps.replaceSelection(activeValue, it)) },
                        onInsertTable = { showInsertTable = true },
                        onInsertImage = { imageLauncher.launch("image/*") },
                        onClose = { persist(); onBack() },
                    )
                    if (showFormatBar) FormatToolbar(value = activeValue, onChange = activeOnChange)
                    if (showFindReplace) {
                        val (mCur, mTot) = RichTextOps.matchInfo(value, findQuery, caseSensitive)
                        FindReplaceBar(
                            find = findQuery,
                            onFind = { findQuery = it },
                            replace = replaceQuery,
                            onReplace = { replaceQuery = it },
                            matchCurrent = mCur,
                            matchTotal = mTot,
                            caseSensitive = caseSensitive,
                            onToggleCase = { caseSensitive = !caseSensitive },
                            onPrev = { value = RichTextOps.findPrev(value, findQuery, caseSensitive) },
                            onNext = { value = RichTextOps.findNext(value, findQuery, caseSensitive) },
                            onReplaceOne = { value = RichTextOps.replaceCurrent(value, findQuery, replaceQuery, caseSensitive) },
                            onReplaceAll = {
                                val (nv, count) = RichTextOps.replaceAll(value, findQuery, replaceQuery, caseSensitive)
                                update(nv)
                                viewModel.notify("تم استبدال ${arabicDigits(count)}")
                            },
                            onClose = { showFindReplace = false },
                        )
                    }
                }
            }

            if (showPageSetup) {
                PageSetupDialog(
                    page = page,
                    onDismiss = { showPageSetup = false },
                    onApply = { newPage -> page = newPage; showPageSetup = false },
                )
            }

            if (showStats) {
                val fullText = buildString {
                    append(value.text)
                    extraBlocks.forEach { if (it is TextBlockUi) { append('\n'); append(it.value.text) } }
                }
                StatsDialog(
                    words = wordCount(fullText),
                    chars = fullText.length,
                    charsNoSpace = fullText.count { !it.isWhitespace() },
                    pages = docPages,
                    onDismiss = { showStats = false },
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

            if (showInsertTable) {
                InsertTableDialog(
                    onDismiss = { showInsertTable = false },
                    onInsert = { r, c -> insertBlock(TableBlockUi(TableOps.newTable(r, c))); showInsertTable = false },
                )
            }

            CompositionLocalProviderDir(page.rtlPage) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                ) {
                  if (showPreview) {
                    PrintPreview(
                        body = value.annotatedString,
                        header = header.annotatedString,
                        footer = footer.annotatedString,
                        page = page,
                        maxWidth = maxWidth,
                    )
                  } else {
                    val rulerThick = if (showRuler) 22.dp else 0.dp
                    val gap = if (showRuler) 2.dp else 0.dp
                    val density = LocalDensity.current.density
                    val sheetWidthDp = maxWidth - rulerThick - gap
                    val scale = sheetWidthDp.value / page.pageWidthPt
                    val pageHeightDp = page.pageHeightPt * scale
                    // ارتفاع المحتوى الفعلي (لا يقل عن صفحة) وعدد الصفحات — يمنع اختفاء المسطرة الرأسية
                    val sheetHeightDp = maxOf(pageHeightDp, if (sheetHeightPx > 0) sheetHeightPx / density else pageHeightDp)
                    val pageCount = paginationPageCount(sheetHeightDp, pageHeightDp)
                    LaunchedEffect(pageCount) { docPages = pageCount }

                    // اتجاه المسطرة يتبع اتجاه الصفحة (ثابت لا يتأثر بالتنسيق)
                    val rulerRtl = page.rtlPage
                    // عرض كسول: لا تُركَّب/تُقاس إلا الكتل المرئية — يمنع تجمّد المستندات الكبيرة
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (showRuler) {
                            item(key = "hruler") {
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
                        }
                        item(key = "sheet") {
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
                                    onBodyFocus = { focusTarget = EditField.Body; hfEditing = null; focusedExtra = -1 },
                                    onSheetHeight = { sheetHeightPx = it },
                                )
                            }
                        }
                        // كتل ما بعد المتن بالترتيب (نص/جدول/صورة) — كلٌّ عنصر كسول مستقل
                        val blockBg = if (page.bgColorArgb != 0) Color(page.bgColorArgb) else Color.White
                        items(extraBlocks.size, key = { "block_$it" }) { i ->
                            BlockItem(
                                block = extraBlocks[i],
                                bgColor = blockBg,
                                onTextChange = { v -> extraBlocks[i] = TextBlockUi(v) },
                                onTextFocus = { focusedExtra = i; focusTarget = EditField.Body; hfEditing = null },
                                onTableChange = { t -> extraBlocks[i] = TableBlockUi(t) },
                                onImageResize = { im -> extraBlocks[i] = ImageBlockUi(im) },
                                onDelete = {
                                    (extraBlocks[i] as? ImageBlockUi)?.let { ImageStore.delete(it.img.path) }
                                    extraBlocks.removeAt(i)
                                    if (focusedExtra >= extraBlocks.size) focusedExtra = -1
                                },
                            )
                        }
                        item(key = "tail") { Spacer(Modifier.height(24.dp)) }
                    }
                  }
                }
            }
        }
    }
}

/** معاينة الطباعة: صفحات A4 حقيقية بفجوة بينها + ترويسة/تذييل/رقم صفحة لكل صفحة. */
@Composable
private fun PrintPreview(
    body: AnnotatedString,
    header: AnnotatedString,
    footer: AnnotatedString,
    page: PageSettings,
    maxWidth: androidx.compose.ui.unit.Dp,
) {
    val density = LocalDensity.current.density
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val pageWdp = maxWidth - 24.dp
    val scale = (pageWdp.value / page.pageWidthPt).coerceAtLeast(0.1f)
    val pageHdp = page.pageHeightPt * scale
    val contentWpx = ((page.pageWidthPt - page.marginLeftPt - page.marginRightPt) * scale * density).coerceAtLeast(10f)
    val contentHpx = ((page.pageHeightPt - page.marginTopPt - page.marginBottomPt) * scale * density).coerceAtLeast(10f)
    val leftPx = page.marginLeftPt * scale * density
    val topPx = page.marginTopPt * scale * density
    val ldir = if (page.rtlPage) LayoutDirection.Rtl else LayoutDirection.Ltr
    val baseStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF1A1A1A))
    val hfStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF555555))

    val layout = remember(body, contentWpx, scale) {
        measurer.measure(
            body,
            style = baseStyle,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = contentWpx.toInt()),
            layoutDirection = ldir,
        )
    }
    val slices = remember(layout, contentHpx) {
        val list = mutableListOf<Int>()
        val total = layout.lineCount
        var start = 0
        while (start < total) {
            val sTop = layout.getLineTop(start)
            var end = start
            while (end < total && layout.getLineBottom(end) - sTop <= contentHpx) end++
            if (end == start) end = start + 1
            list.add(start)
            start = end
        }
        if (list.isEmpty()) list.add(0)
        list
    }
    val total = slices.size

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        slices.forEachIndexed { idx, sLine ->
            val sliceTop = if (layout.lineCount > 0) layout.getLineTop(sLine) else 0f
            Box(Modifier.width(pageWdp).height(pageHdp.dp).shadow(4.dp).background(Color.White)) {
                Canvas(Modifier.fillMaxSize()) {
                    if (header.text.isNotBlank()) {
                        drawText(measurer, header, topLeft = Offset(leftPx, topPx * 0.2f), style = hfStyle, size = Size(contentWpx, topPx))
                    }
                    clipRect(leftPx, topPx, leftPx + contentWpx, topPx + contentHpx) {
                        drawText(layout, topLeft = Offset(leftPx, topPx - sliceTop))
                    }
                    val footY = topPx + contentHpx + (page.marginBottomPt * scale * density) * 0.15f
                    if (footer.text.isNotBlank()) {
                        drawText(measurer, footer, topLeft = Offset(leftPx, footY), style = hfStyle, size = Size(contentWpx, size.height - footY))
                    }
                    val pn = measurer.measure(AnnotatedString("صفحة ${arabicDigits(idx + 1)} من ${arabicDigits(total)}"), style = TextStyle(fontSize = 12.sp, color = Color(0xFF777777)))
                    drawText(pn, topLeft = Offset((size.width - pn.size.width) / 2f, size.height - pn.size.height - 6f * density))
                }
            }
            Spacer(Modifier.height(14.dp))
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
            .background(if (page.bgColorArgb != 0) Color(page.bgColorArgb) else Color.White)
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
                val pageH = pageHeightDp * density
                // فجوة رمادية واضحة بين الصفحات (كوورد) تحتوي التذييل ورقم الصفحة بعيداً عن جسم النص
                val useArabic = page.rtlPage
                fun digits(n: Int) = if (useArabic) arabicDigits(n) else n.toString()
                val footerText = footer.annotatedString.text.replace("\n", " ").trim()
                val gapColor = Color(0xFFD7DBE2)
                val edge = Color(0x33000000)
                val gapH = 36f * density
                val fPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#444444")
                    textSize = 11f * density
                    isAntiAlias = true
                }
                fun drawPageNumber(cy: Float, num: Int) {
                    val pnLabel = "${digits(num)} / ${digits(pageCount)}"
                    val x = when (page.pageNumberAlign) {
                        0 -> { fPaint.textAlign = android.graphics.Paint.Align.RIGHT; size.width - mrDp * density }
                        2 -> { fPaint.textAlign = android.graphics.Paint.Align.LEFT; mlDp * density }
                        else -> { fPaint.textAlign = android.graphics.Paint.Align.CENTER; size.width / 2f }
                    }
                    drawContext.canvas.nativeCanvas.drawText(pnLabel, x, cy, fPaint)
                }
                for (p in 1 until pageCount) {
                    val y = p * pageH
                    if (y >= size.height) break
                    val top = y - gapH / 2f
                    // خطّ فاصل بين جسم الصفحة والتذييل، ثم الفجوة الرمادية، ثم خطّ قبل الصفحة التالية
                    drawLine(edge, Offset(0f, top), Offset(size.width, top), strokeWidth = 1f)
                    drawRect(color = gapColor, topLeft = Offset(0f, top), size = Size(size.width, gapH))
                    drawLine(edge, Offset(0f, top + gapH), Offset(size.width, top + gapH), strokeWidth = 1f)
                    // التذييل (سطر أعلى الفجوة) ثم رقم الصفحة (سطر أسفلها)
                    if (footerText.isNotEmpty()) {
                        fPaint.textAlign = android.graphics.Paint.Align.CENTER
                        drawContext.canvas.nativeCanvas.drawText(footerText, size.width / 2f, top + 14f * density, fPaint)
                    }
                    if (page.showPageNumber) drawPageNumber(top + 30f * density, p)
                }
                // رقم صفحة الورقة الأخيرة (لا فجوة بعدها) في هامشها السفلي
                if (page.showPageNumber && pageCount >= 1) {
                    drawPageNumber(size.height - mbDp * density + 14f * density, pageCount)
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
                placeholder = "التذييل (نقر مزدوج للتحرير) — يتكرّر على كل صفحة",
                editing = hfEditing == EditField.Footer,
                onStartEditing = { onStartEditHF(EditField.Footer) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            // رقم الصفحة يُرسم تلقائياً على كل صفحة عبر Canvas (انظر drawWithContent)
        }
    }
}

/** يحوّل عدداً إلى أرقام عربية للعرض. */
fun arabicDigits(n: Int): String {
    val ar = "٠١٢٣٤٥٦٧٨٩"
    return n.toString().map { if (it in '0'..'9') ar[it - '0'] else it }.joinToString("")
}

/** الحقل القابل للتنسيق: المتن (أو كتلة نصية) أو الترويسة أو التذييل. */
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
    matchCurrent: Int,
    matchTotal: Int,
    caseSensitive: Boolean,
    onToggleCase: () -> Unit,
    onPrev: () -> Unit,
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
                modifier = Modifier.width(150.dp),
            )
            // عدّاد النتائج «الحالي/الإجمالي»
            Text(
                if (matchTotal > 0) " ${arabicDigits(matchCurrent)}/${arabicDigits(matchTotal)} " else " ٠ ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onPrev) { Icon(Icons.Default.ArrowDropDown, contentDescription = "السابق", modifier = Modifier.rotate(180f)) }
            IconButton(onClick = onNext) { Icon(Icons.Default.ArrowDropDown, contentDescription = "التالي") }
            // حسّاس لحالة الأحرف (Aa)
            IconButton(onClick = onToggleCase) {
                Text(
                    "Aa",
                    color = if (caseSensitive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (caseSensitive) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = replace,
                onValueChange = onReplace,
                placeholder = { Text("استبدال بـ") },
                singleLine = true,
                modifier = Modifier.width(150.dp),
            )
            TextButton(onClick = onReplaceOne) { Text("استبدال") }
            TextButton(onClick = onReplaceAll) { Text("الكل") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
        }
    }
}

/** عدد الكلمات في النص. */
fun wordCount(text: String): Int =
    text.split(Regex("\\s+")).count { it.isNotBlank() }

/** شريط القوائم الكلاسيكي (ملف/تحرير/تنسيق/…) كلٌّ يفتح قائمة أوامر منسدلة. */
@Composable
private fun EditorMenuBar(
    openMenu: String?,
    onOpenMenu: (String?) -> Unit,
    page: PageSettings,
    showRuler: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit,
    onFindReplace: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onExportPdf: () -> Unit,
    onPageSetup: () -> Unit,
    onTogglePageNumber: () -> Unit,
    onToggleRuler: () -> Unit,
    showPreview: Boolean,
    onTogglePreview: () -> Unit,
    onStats: () -> Unit,
    formatBarOpen: Boolean,
    onToggleFormatBar: () -> Unit,
    onDelete: () -> Unit,
    onInsertText: (String) -> Unit,
    onInsertTable: () -> Unit,
    onInsertImage: () -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MenuBarMenu("ملف", openMenu, onOpenMenu) { close ->
            MenuRow("فتح", Icons.Default.FolderOpen) { close(); onOpen() }
            MenuRow("حفظ", Icons.Default.Save) { close(); onSave() }
            MenuRow("حفظ باسم…", Icons.Default.SaveAs) { close(); onSaveAs() }
            HorizontalDivider()
            MenuRow("تصدير PDF", Icons.Default.PictureAsPdf) { close(); onExportPdf() }
            HorizontalDivider()
            MenuRow("إعداد الصفحة", Icons.Default.AspectRatio) { close(); onPageSetup() }
            MenuRow(if (page.showPageNumber) "إخفاء ترقيم الصفحات" else "إظهار ترقيم الصفحات", Icons.Default.Numbers) { close(); onTogglePageNumber() }
            HorizontalDivider()
            MenuRow("مستنداتي", Icons.Default.Folder) { close(); onClose() }
        }
        MenuBarMenu("تحرير", openMenu, onOpenMenu) { close ->
            MenuRow("تراجع", Icons.AutoMirrored.Filled.Undo, enabled = canUndo) { close(); onUndo() }
            MenuRow("إعادة", Icons.AutoMirrored.Filled.Redo, enabled = canRedo) { close(); onRedo() }
            HorizontalDivider()
            MenuRow("قص", Icons.Default.ContentCut) { close(); onCut() }
            MenuRow("نسخ", Icons.Default.ContentCopy) { close(); onCopy() }
            MenuRow("لصق", Icons.Default.ContentPaste) { close(); onPaste() }
            MenuRow("حذف المحدّد", Icons.Default.Delete) { close(); onDelete() }
            HorizontalDivider()
            MenuRow("تحديد الكل", Icons.Default.SelectAll) { close(); onSelectAll() }
            MenuRow("بحث واستبدال", Icons.Default.Search) { close(); onFindReplace() }
        }
        MenuBarMenu("تنسيق", openMenu, onOpenMenu) { close ->
            MenuRow("غامق", Icons.Default.FormatBold) { close(); onChange(RichTextOps.toggleBold(value)) }
            MenuRow("مائل", Icons.Default.FormatItalic) { close(); onChange(RichTextOps.toggleItalic(value)) }
            MenuRow("تسطير", Icons.Default.FormatUnderlined) { close(); onChange(RichTextOps.toggleUnderline(value)) }
            MenuRow("يتوسطه خط", Icons.Default.StrikethroughS) { close(); onChange(RichTextOps.toggleStrike(value)) }
            MenuRow("مرتفع X²", Icons.Default.Superscript) { close(); onChange(RichTextOps.toggleScript(value, 1)) }
            MenuRow("منخفض X₂", Icons.Default.Subscript) { close(); onChange(RichTextOps.toggleScript(value, 2)) }
            HorizontalDivider()
            MenuRow("محاذاة يمين", Icons.AutoMirrored.Filled.FormatAlignRight) { close(); onChange(RichTextOps.setAlign(value, TextAlign.Right)) }
            MenuRow("توسيط", Icons.Default.FormatAlignCenter) { close(); onChange(RichTextOps.setAlign(value, TextAlign.Center)) }
            MenuRow("محاذاة يسار", Icons.AutoMirrored.Filled.FormatAlignLeft) { close(); onChange(RichTextOps.setAlign(value, TextAlign.Left)) }
            MenuRow("ضبط", Icons.Default.FormatAlignJustify) { close(); onChange(RichTextOps.setAlign(value, TextAlign.Justify)) }
            HorizontalDivider()
            MenuRow("لون أحمر", Icons.Default.FormatColorText) { close(); onChange(RichTextOps.setColor(value, 0xFFD32F2F.toInt())) }
            MenuRow("لون أزرق", Icons.Default.FormatColorText) { close(); onChange(RichTextOps.setColor(value, 0xFF1565C0.toInt())) }
            MenuRow("لون أسود", Icons.Default.FormatColorText) { close(); onChange(RichTextOps.setColor(value, 0xFF000000.toInt())) }
            MenuRow("تظليل أصفر", Icons.Default.FormatColorFill) { close(); onChange(RichTextOps.setHighlight(value, 0xFFFFEB3B.toInt())) }
            MenuRow("إزالة التظليل", Icons.Default.FormatColorReset) { close(); onChange(RichTextOps.setHighlight(value, 0)) }
            HorizontalDivider()
            MenuRow("قائمة نقطية", Icons.AutoMirrored.Filled.FormatListBulleted) { close(); onChange(RichTextOps.applyList(value, RichTextOps.ListSpec(numbered = false, glyph = "•"))) }
            MenuRow("قائمة مرقّمة", Icons.Default.FormatListNumbered) { close(); onChange(RichTextOps.applyList(value, RichTextOps.ListSpec(numbered = true, sep = "."))) }
        }
        MenuBarMenu("إدراج", openMenu, onOpenMenu) { close ->
            MenuRow(if (page.showPageNumber) "إخفاء رقم الصفحة" else "رقم الصفحة", Icons.Default.Numbers) { close(); onTogglePageNumber() }
            MenuRow("إدراج التاريخ", Icons.Default.DateRange) { close(); onInsertText(currentDateString()) }
            MenuRow("إدراج الوقت", Icons.Default.Schedule) { close(); onInsertText(currentTimeString()) }
            HorizontalDivider()
            MenuRow("جدول", Icons.Default.TableChart) { close(); onInsertTable() }
            MenuRow("صورة", Icons.Default.Image) { close(); onInsertImage() }
        }
        MenuBarMenu("تخطيط", openMenu, onOpenMenu) { close ->
            MenuRow("الهوامش / الحجم / الاتجاه", Icons.Default.AspectRatio) { close(); onPageSetup() }
            HorizontalDivider()
            MenuRow("زيادة المسافة البادئة", Icons.AutoMirrored.Filled.FormatIndentIncrease) { close(); onChange(RichTextOps.changeIndent(value, +1)) }
            MenuRow("إنقاص المسافة البادئة", Icons.AutoMirrored.Filled.FormatIndentDecrease) { close(); onChange(RichTextOps.changeIndent(value, -1)) }
            HorizontalDivider()
            MenuRow("تباعد مفرد", Icons.Default.FormatLineSpacing) { close(); onChange(RichTextOps.setLineSpacing(value, 1.0f)) }
            MenuRow("تباعد ١٫٥", Icons.Default.FormatLineSpacing) { close(); onChange(RichTextOps.setLineSpacing(value, 1.5f)) }
            MenuRow("تباعد مزدوج", Icons.Default.FormatLineSpacing) { close(); onChange(RichTextOps.setLineSpacing(value, 2.0f)) }
        }
        MenuBarMenu("عرض", openMenu, onOpenMenu) { close ->
            MenuRow(if (showPreview) "وضع التحرير" else "معاينة الطباعة", Icons.Default.Straighten) { close(); onTogglePreview() }
            MenuRow(if (showRuler) "إخفاء المسطرة" else "إظهار المسطرة", Icons.Default.Straighten) { close(); onToggleRuler() }
        }
        MenuBarMenu("أدوات", openMenu, onOpenMenu) { close ->
            MenuRow("إحصائيات المستند", Icons.AutoMirrored.Filled.Notes) { close(); onStats() }
        }
        // مبدّل شريط التنسيق (يُفتح عند الطلب لتوفير المساحة)
        Row(
            Modifier.clickable { onToggleFormatBar() }.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.FormatSize,
                contentDescription = null,
                tint = if (formatBarOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "تنسيق",
                color = if (formatBarOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = if (formatBarOpen) "إخفاء شريط التنسيق" else "إظهار شريط التنسيق",
                modifier = Modifier.rotate(if (formatBarOpen) 180f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
    }
}

/** التاريخ الحالي بصيغة عربية. */
fun currentDateString(): String =
    java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale("ar")).format(java.util.Date())

/** الوقت الحالي. */
fun currentTimeString(): String =
    java.text.SimpleDateFormat("hh:mm a", java.util.Locale("ar")).format(java.util.Date())

@Composable
private fun MenuBarMenu(
    name: String,
    openMenu: String?,
    onOpenMenu: (String?) -> Unit,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    Box {
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable { onOpenMenu(name) }.padding(horizontal = 14.dp, vertical = 12.dp),
        )
        DropdownMenu(expanded = openMenu == name, onDismissRequest = { onOpenMenu(null) }) {
            content { onOpenMenu(null) }
        }
    }
}

@Composable
private fun MenuRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        enabled = enabled,
        onClick = onClick,
    )
}

/** مربّع اختيار عدد صفوف/أعمدة الجدول عند الإدراج (متغيّر). */
@Composable
private fun InsertTableDialog(onDismiss: () -> Unit, onInsert: (rows: Int, cols: Int) -> Unit) {
    var rows by remember { mutableStateOf(2) }
    var cols by remember { mutableStateOf(2) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدراج جدول") },
        text = {
            Column {
                StepperRow("عدد الصفوف", rows, { rows = (rows - 1).coerceAtLeast(1) }, { rows = (rows + 1).coerceAtMost(50) })
                Spacer(Modifier.height(8.dp))
                StepperRow("عدد الأعمدة", cols, { cols = (cols - 1).coerceAtLeast(1) }, { cols = (cols + 1).coerceAtMost(20) })
            }
        },
        confirmButton = { TextButton(onClick = { onInsert(rows, cols) }) { Text("إدراج") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun StepperRow(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        IconButton(onClick = onMinus) { Icon(Icons.Default.Close, "−") }
        Text("$value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
        IconButton(onClick = onPlus) { Icon(Icons.Default.Add, "+") }
    }
}

/** يعرض كتل ما بعد المتن بالترتيب (نص قابل للتحرير/جدول/صورة) — المحرّك الكتلي. */
@Composable
private fun BlockItem(
    block: BlockUi,
    bgColor: Color,
    onTextChange: (TextFieldValue) -> Unit,
    onTextFocus: () -> Unit,
    onTableChange: (TableData) -> Unit,
    onImageResize: (DocImage) -> Unit,
    onDelete: () -> Unit,
) {
    when (block) {
        is TextBlockUi -> BasicTextField(
            value = block.value,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(bgColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .onFocusChanged { if (it.isFocused) onTextFocus() },
            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF1A1A1A)),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (block.value.text.isEmpty()) {
                    Text("تابع الكتابة…", color = Color(0xFFBBBBBB), textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                }
                inner()
            },
        )
        is TableBlockUi -> TableBlockEditor(block.data, onTableChange, onDelete)
        is ImageBlockUi -> ImageBlockCard(block.img, onImageResize, onDelete)
    }
}

/** بطاقة صورة مُدرَجة: عرض + تكبير/تصغير (بحفظ النسبة) + حذف. */
@Composable
private fun ImageBlockCard(im: DocImage, onResize: (DocImage) -> Unit, onDelete: () -> Unit) {
    val bmp = remember(im.path) { ImageStore.decode(im.path)?.asImageBitmap() }
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, "صورة", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                val ratio = if (im.widthPt > 0) im.heightPt / im.widthPt else 0.75f
                TextButton(onClick = {
                    val w = (im.widthPt - 40f).coerceAtLeast(60f)
                    onResize(im.copy(widthPt = w, heightPt = w * ratio))
                }) { Text("− تصغير") }
                TextButton(onClick = {
                    val w = (im.widthPt + 40f).coerceAtMost(1200f)
                    onResize(im.copy(widthPt = w, heightPt = w * ratio))
                }) { Text("+ تكبير") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(6.dp))
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = "صورة مُدرَجة", modifier = Modifier.width(im.widthPt.dp).height(im.heightPt.dp))
            } else {
                Text("تعذّر عرض الصورة", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** محرّر جدول واحد: خلايا قابلة للتحرير + محاذاة/عرض/دمج/تنسيق لكل خلية. */
@Composable
private fun TableBlockEditor(table: TableData, onChange: (TableData) -> Unit, onDelete: () -> Unit) {
    var sel by remember { mutableStateOf(Pair(-1, -1)) } // صف، عمود
    val valid = sel.first >= 0 && sel.second >= 0
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(8.dp)) {
            // شريط تحكم الجدول
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onChange(TableOps.addRow(table)) }) { Text("+ صف") }
                TextButton(onClick = { onChange(TableOps.addColumn(table)) }) { Text("+ عمود") }
                TextButton(onClick = { onChange(TableOps.deleteRow(table, table.rowCount - 1)) }) { Text("− صف") }
                TextButton(onClick = { onChange(TableOps.deleteColumn(table, table.colCount - 1)) }) { Text("− عمود") }
                TextButton(onClick = onDelete) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            }
            // محاذاة + عرض العمود + دمج/فصل
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                val applyAlign: (Int) -> Unit = { a -> if (valid) onChange(TableOps.setCellAlign(table, sel.first, sel.second, a)) }
                IconButton(onClick = { applyAlign(3) }) { Icon(Icons.AutoMirrored.Filled.FormatAlignRight, "يمين") }
                IconButton(onClick = { applyAlign(1) }) { Icon(Icons.Default.FormatAlignCenter, "توسيط") }
                IconButton(onClick = { applyAlign(2) }) { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, "يسار") }
                Box(Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant))
                TextButton(onClick = { if (sel.second >= 0) onChange(TableOps.widenColumn(table, sel.second, -0.2f)) }) { Text("− عرض") }
                TextButton(onClick = { if (sel.second >= 0) onChange(TableOps.widenColumn(table, sel.second, 0.2f)) }) { Text("+ عرض") }
                Box(Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant))
                TextButton(onClick = { if (valid) onChange(TableOps.mergeRight(table, sel.first, sel.second)) }) { Text("دمج ←") }
                TextButton(onClick = { if (valid) onChange(TableOps.splitCell(table, sel.first, sel.second)) }) { Text("فصل") }
            }
            // تنسيق الخلية: غامق + حجم + لون
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                val selCell = if (valid && sel.first in table.rows.indices) table.rows[sel.first].getOrNull(sel.second) else null
                IconButton(onClick = { selCell?.let { onChange(TableOps.setCellBold(table, sel.first, sel.second, !it.bold)) } }) {
                    Icon(Icons.Default.FormatBold, "غامق", tint = if (selCell?.bold == true) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                }
                TextButton(onClick = { selCell?.let { onChange(TableOps.setCellSize(table, sel.first, sel.second, (if (it.size > 0) it.size else 14) - 2)) } }) { Text("ص−") }
                TextButton(onClick = { selCell?.let { onChange(TableOps.setCellSize(table, sel.first, sel.second, (if (it.size > 0) it.size else 14) + 2)) } }) { Text("ص+") }
                Spacer(Modifier.width(4.dp))
                listOf(0L, 0xFFD32F2FL, 0xFF1565C0L, 0xFF2E7D32L, 0xFF000000L).forEach { col ->
                    Box(
                        Modifier.padding(2.dp).size(22.dp)
                            .background(if (col == 0L) Color.Transparent else Color(col), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { if (valid) onChange(TableOps.setCellColor(table, sel.first, sel.second, col)) },
                        contentAlignment = Alignment.Center,
                    ) { if (col == 0L) Text("∅", fontSize = 12.sp) }
                }
            }
            // الخلايا (تخطّي الخلايا المغطّاة بالدمج + امتداد الوزن)
            table.rows.forEachIndexed { r, row ->
                Row(Modifier.fillMaxWidth()) {
                    var c = 0
                    while (c < row.size) {
                        val cell = row[c]
                        val span = cell.span.coerceIn(1, row.size - c)
                        val wsum = (c until c + span).sumOf { table.weightOf(it).toDouble() }.toFloat()
                        val ta = when (cell.align) {
                            1 -> TextAlign.Center
                            2 -> TextAlign.Left
                            3 -> TextAlign.Right
                            else -> TextAlign.Right
                        }
                        val cc = c
                        val selected = sel == Pair(r, cc)
                        OutlinedTextField(
                            value = cell.text,
                            onValueChange = { onChange(TableOps.setCell(table, r, cc, it)) },
                            singleLine = false,
                            textStyle = TextStyle(
                                fontSize = (if (cell.size > 0) cell.size else 14).sp,
                                textAlign = ta,
                                fontWeight = if (cell.bold) FontWeight.Bold else null,
                                color = if (cell.color != 0L) Color(cell.color) else Color.Unspecified,
                            ),
                            label = if (selected) ({ Text(if (span > 1) "محدّدة (مدموجة ×$span)" else "محدّدة") }) else null,
                            modifier = Modifier
                                .weight(wsum)
                                .padding(2.dp)
                                .onFocusChanged { if (it.isFocused) sel = Pair(r, cc) },
                        )
                        c += span
                    }
                }
            }
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
    var showPn by remember { mutableStateOf(page.showPageNumber) }
    var pnAlign by remember { mutableStateOf(page.pageNumberAlign) }

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
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("ترقيم الصفحات تلقائياً (رقم/إجمالي)", Modifier.weight(1f))
                    Switch(checked = showPn, onCheckedChange = { showPn = it })
                }
                if (showPn) {
                    Text("موضع رقم الصفحة", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(0 to "يمين", 1 to "وسط", 2 to "يسار").forEach { (v, lbl) ->
                            Row(
                                Modifier.weight(1f).clickable { pnAlign = v },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = pnAlign == v, onClick = { pnAlign = v })
                                Text(lbl)
                            }
                        }
                    }
                    Text(
                        "الأرقام تتبع لغة الصفحة: عربية عند «عربي»، وإنجليزية خلاف ذلك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(
                    page.withSize(pageSizeById(sizeId), landscape)
                        .copy(rtlPage = rtlPage, showPageNumber = showPn, pageNumberAlign = pnAlign),
                )
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
    var textColorPicker by remember { mutableStateOf(false) }
    var highlightPicker by remember { mutableStateOf(false) }
    var alignMenu by remember { mutableStateOf(false) }
    var dirMenu by remember { mutableStateOf(false) }
    var spacingMenu by remember { mutableStateOf(false) }
    var listDialog by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var copiedFormat by remember { mutableStateOf<CharAttrs?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolToggle(Icons.Default.FormatBold, "غامق", cur.bold) { onChange(RichTextOps.toggleBold(value)) }
        ToolToggle(Icons.Default.FormatItalic, "مائل", cur.italic) { onChange(RichTextOps.toggleItalic(value)) }
        ToolToggle(Icons.Default.FormatUnderlined, "تسطير", cur.underline) { onChange(RichTextOps.toggleUnderline(value)) }
        ToolDivider()

        // عائلة الخط (قائمة)
        Box {
            Row(
                Modifier
                    .toolChip()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { fontMenu = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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
                Modifier
                    .toolChip()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { sizeMenu = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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

        // لون النص (منتقي كامل)
        IconButton(onClick = { textColorPicker = true }) {
            Icon(Icons.Default.FormatColorText, "لون النص", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // تظليل (منتقي كامل)
        IconButton(onClick = { highlightPicker = true }) {
            Icon(Icons.Default.FormatColorFill, "تظليل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // مسح التنسيق
        IconButton(onClick = { onChange(RichTextOps.clearFormatting(value)) }) {
            Icon(Icons.Default.FormatColorReset, "مسح التنسيق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                MenuChoice("مرتفع X²", Icons.Default.Superscript, cur.script == 1) { onChange(RichTextOps.toggleScript(value, 1)); moreMenu = false }
                MenuChoice("منخفض X₂", Icons.Default.Subscript, cur.script == 2) { onChange(RichTextOps.toggleScript(value, 2)); moreMenu = false }
                MenuChoice("تظليل أصفر", Icons.Default.FormatColorFill, cur.highlightArgb != 0) { onChange(RichTextOps.setHighlight(value, 0xFFFFEB3B.toInt())); moreMenu = false }
                MenuChoice("إزالة التظليل", Icons.Default.FormatColorReset, false) { onChange(RichTextOps.setHighlight(value, 0)); moreMenu = false }
                HorizontalDivider()
                MenuChoice("نسخ التنسيق (فرشاة)", Icons.Default.ContentCopy, copiedFormat != null) {
                    copiedFormat = cur; moreMenu = false
                }
                if (copiedFormat != null) {
                    MenuChoice("لصق التنسيق على التحديد", Icons.Default.ContentPaste, false) {
                        onChange(RichTextOps.applyCharAttrs(value, copiedFormat!!)); moreMenu = false
                    }
                }
            }
        }
    }
    }

    if (listDialog) {
        ListPickerDialog(
            onPick = { spec -> onChange(RichTextOps.applyList(value, spec)); listDialog = false },
            onDismiss = { listDialog = false },
        )
    }
    if (textColorPicker) {
        ColorPickerDialog(
            title = "لون النص",
            initialArgb = cur.colorArgb,
            presets = SWATCHES,
            allowNone = false,
            onPick = { argb -> onChange(RichTextOps.setColor(value, argb)); textColorPicker = false },
            onDismiss = { textColorPicker = false },
        )
    }
    if (highlightPicker) {
        ColorPickerDialog(
            title = "لون التظليل",
            initialArgb = cur.highlightArgb,
            presets = HIGHLIGHTS,
            allowNone = true,
            onPick = { argb -> onChange(RichTextOps.setHighlight(value, argb)); highlightPicker = false },
            onDismiss = { highlightPicker = false },
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
    // الحالة المفعّلة تظهر كزرّ ممتلئ (Material 3) لوضوح أعلى
    val tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Box(
        Modifier
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
    ) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = desc, tint = tint) }
    }
}

/** خلفية «شريحة» خفيفة لمشغّلات القوائم (الخط/الحجم) لتبدو كعناصر تحكّم. */
private fun Modifier.toolChip(): Modifier = this
    .padding(horizontal = 2.dp)
    .clip(RoundedCornerShape(10.dp))

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

/**
 * منتقي ألوان احترافي: شبكة ألوان جاهزة + منزلقات RGB + معاينة، مع خيار «بلا لون».
 * يُرجِع 0 عند اختيار «بلا لون».
 */
@Composable
private fun ColorPickerDialog(
    title: String,
    initialArgb: Int,
    presets: List<Int>,
    allowNone: Boolean,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = if (initialArgb != 0) initialArgb else 0xFF000000.toInt()
    var r by remember { mutableStateOf(((start shr 16) and 0xFF).toFloat()) }
    var g by remember { mutableStateOf(((start shr 8) and 0xFF).toFloat()) }
    var b by remember { mutableStateOf((start and 0xFF).toFloat()) }
    val current = (0xFF000000.toInt()) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // شبكة الألوان الجاهزة (٦ لكل صف)
                presets.chunked(6).forEach { rowColors ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        rowColors.forEach { argb ->
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .background(Color(argb), RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                    .clickable {
                                        r = ((argb shr 16) and 0xFF).toFloat()
                                        g = ((argb shr 8) and 0xFF).toFloat()
                                        b = (argb and 0xFF).toFloat()
                                    },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // معاينة + منزلقات RGB
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp)
                            .background(Color(current), RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("لون مخصّص (RGB)", style = MaterialTheme.typography.labelLarge)
                }
                Text("أحمر ${r.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(value = r, onValueChange = { r = it }, valueRange = 0f..255f)
                Text("أخضر ${g.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(value = g, onValueChange = { g = it }, valueRange = 0f..255f)
                Text("أزرق ${b.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(value = b, onValueChange = { b = it }, valueRange = 0f..255f)
            }
        },
        confirmButton = { TextButton(onClick = { onPick(current) }) { Text("تطبيق") } },
        dismissButton = {
            Row {
                if (allowNone) {
                    TextButton(onClick = { onPick(0) }) { Text("بلا لون") }
                }
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            }
        },
    )
}

/** إحصائيات المستند: الكلمات والأحرف والصفحات. */
@Composable
private fun StatsDialog(words: Int, chars: Int, charsNoSpace: Int, pages: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إحصائيات المستند") },
        text = {
            Column {
                StatRow("عدد الكلمات", arabicDigits(words))
                StatRow("عدد الأحرف (مع المسافات)", arabicDigits(chars))
                StatRow("عدد الأحرف (بلا مسافات)", arabicDigits(charsNoSpace))
                StatRow("عدد الصفحات", arabicDigits(pages))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("حسناً") } },
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
