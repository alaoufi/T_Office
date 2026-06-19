package com.toffice.app.feature.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toffice.app.data.document.DocumentDao
import com.toffice.app.feature.editor.io.DocxReader
import com.toffice.app.feature.editor.io.DocxWriter
import com.toffice.app.feature.editor.io.PdfExporter
import com.toffice.app.feature.editor.model.DocBundle
import com.toffice.app.feature.editor.model.DocSerializer
import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.toParagraphsOut
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val json: String = "",
    val hasSource: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val dao: DocumentDao,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val docId: Long = savedStateHandle.get<String>("docId")?.toLongOrNull() ?: -1L
    private var sourceUri: String? = null

    private val _ui = MutableStateFlow(EditorUiState())
    val ui = _ui.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val doc = if (docId > 0) dao.getById(docId) else null
            sourceUri = doc?.sourceUri
            _ui.value = EditorUiState(
                isLoading = false,
                title = doc?.title ?: "مستند جديد",
                json = doc?.contentJson ?: "",
                hasSource = sourceUri != null,
            )
        }
    }

    /** حفظ داخلي + حفظ بنفس ملف DOCX الأصلي إن وُجد. */
    fun save(
        title: String,
        json: String,
        annotated: AnnotatedString,
        page: PageSettings,
        header: AnnotatedString,
        footer: AnnotatedString,
        tables: List<com.toffice.app.feature.editor.model.TableData> = emptyList(),
        afterBody: AnnotatedString = AnnotatedString(""),
    ) {
        viewModelScope.launch {
            val existing = if (docId > 0) dao.getById(docId) else null
            if (existing != null) {
                dao.update(
                    existing.copy(
                        title = title.ifBlank { "مستند" },
                        contentJson = json,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
            val src = sourceUri
            if (src != null) {
                val ok = writeDocxTo(Uri.parse(src), annotated, page, header, footer, tables, afterBody)
                _events.emit(if (ok) "تم الحفظ في ملف Word الأصلي" else "تم الحفظ داخلياً (تعذّر الكتابة على الملف الأصلي)")
            } else {
                _events.emit("تم الحفظ")
            }
        }
    }

    /** يفتح ملف DOCX من الجهاز ويحمّله في المحرر الحالي. */
    fun openDocx(uri: Uri, onLoaded: (title: String, bundle: DocBundle) -> Unit) {
        viewModelScope.launch {
            val canWriteBack = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }.isSuccess
            val bundle = kotlinx.coroutines.withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { DocxReader.read(it) }
                } catch (e: Exception) {
                    null
                }
            }
            if (bundle == null) {
                _events.emit("تعذّر فتح الملف")
                return@launch
            }
            sourceUri = if (canWriteBack) uri.toString() else null
            // حفظ نسخة داخلية في قاعدة البيانات لنفس المستند
            if (docId > 0) {
                val existing = dao.getById(docId)
                if (existing != null) {
                    dao.update(
                        existing.copy(
                            title = fileName(uri),
                            contentJson = DocSerializer.serialize(bundle),
                            sourceUri = sourceUri,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                }
            }
            onLoaded(fileName(uri), bundle)
            _events.emit("تم فتح الملف")
        }
    }

    private fun fileName(uri: Uri): String = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx).substringBeforeLast(".").ifBlank { "مستند" } else "مستند"
        } ?: "مستند"
    } catch (e: Exception) {
        "مستند"
    }

    fun exportDocx(uri: Uri, annotated: AnnotatedString, page: PageSettings, header: AnnotatedString, footer: AnnotatedString, tables: List<com.toffice.app.feature.editor.model.TableData> = emptyList(), afterBody: AnnotatedString = AnnotatedString("")) {
        viewModelScope.launch {
            val ok = writeDocxTo(uri, annotated, page, header, footer, tables, afterBody)
            _events.emit(if (ok) "تم تصدير ملف Word بنجاح" else "تعذّر التصدير")
        }
    }

    /** يعرض رسالة قصيرة للمستخدم. */
    fun notify(msg: String) {
        viewModelScope.launch { _events.emit(msg) }
    }

    /** حفظ نصّ عادي (TXT) بترميز UTF-8. */
    fun exportText(uri: Uri, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                _events.emit("تم الحفظ كنص (TXT)")
            } catch (e: Exception) {
                _events.emit("تعذّر الحفظ: ${e.message}")
            }
        }
    }

    fun exportPdf(uri: Uri, annotated: AnnotatedString, page: PageSettings, header: AnnotatedString, footer: AnnotatedString) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    PdfExporter.export(it, annotated, page, header, footer)
                }
                _events.emit("تم تصدير PDF بنجاح")
            } catch (e: Exception) {
                _events.emit("تعذّر تصدير PDF: ${e.message}")
            }
        }
    }

    private suspend fun writeDocxTo(
        uri: Uri,
        annotated: AnnotatedString,
        page: PageSettings,
        header: AnnotatedString,
        footer: AnnotatedString,
        tables: List<com.toffice.app.feature.editor.model.TableData> = emptyList(),
        afterBody: AnnotatedString = AnnotatedString(""),
    ): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            // "wt" يقتطع الملف قبل الكتابة لتفادي بقايا قديمة
            val mode = if (uri.scheme == "content") "wt" else "w"
            context.contentResolver.openOutputStream(uri, mode)?.use {
                DocxWriter.write(it, annotated.toParagraphsOut(), page, header.toParagraphsOut(), footer.toParagraphsOut(), tables, afterBody.toParagraphsOut())
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }
}
