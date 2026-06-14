package com.toffice.app.feature.editor

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toffice.app.data.document.DocumentDao
import com.toffice.app.feature.editor.io.DocxWriter
import com.toffice.app.feature.editor.io.PdfExporter
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
                val ok = writeDocxTo(Uri.parse(src), annotated, page, header, footer)
                _events.emit(if (ok) "تم الحفظ في ملف Word الأصلي" else "تم الحفظ داخلياً (تعذّر الكتابة على الملف الأصلي)")
            } else {
                _events.emit("تم الحفظ")
            }
        }
    }

    fun exportDocx(uri: Uri, annotated: AnnotatedString, page: PageSettings, header: AnnotatedString, footer: AnnotatedString) {
        viewModelScope.launch {
            val ok = writeDocxTo(uri, annotated, page, header, footer)
            _events.emit(if (ok) "تم تصدير ملف Word بنجاح" else "تعذّر التصدير")
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
    ): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            // "wt" يقتطع الملف قبل الكتابة لتفادي بقايا قديمة
            val mode = if (uri.scheme == "content") "wt" else "w"
            context.contentResolver.openOutputStream(uri, mode)?.use {
                DocxWriter.write(it, annotated.toParagraphsOut(), page, header.toParagraphsOut(), footer.toParagraphsOut())
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }
}
