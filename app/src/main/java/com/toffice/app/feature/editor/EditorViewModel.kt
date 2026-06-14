package com.toffice.app.feature.editor

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toffice.app.data.document.DocumentDao
import com.toffice.app.feature.editor.io.DocxWriter
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
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val dao: DocumentDao,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val docId: Long = savedStateHandle.get<String>("docId")?.toLongOrNull() ?: -1L

    private val _ui = MutableStateFlow(EditorUiState())
    val ui = _ui.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val doc = if (docId > 0) dao.getById(docId) else null
            _ui.value = EditorUiState(
                isLoading = false,
                title = doc?.title ?: "مستند جديد",
                json = doc?.contentJson ?: "",
            )
        }
    }

    fun save(title: String, json: String) {
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
                _events.emit("تم الحفظ")
            }
        }
    }

    fun exportDocx(uri: Uri, annotated: AnnotatedString) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    DocxWriter.write(it, annotated.toParagraphsOut())
                }
                _events.emit("تم تصدير ملف Word بنجاح")
            } catch (e: Exception) {
                _events.emit("تعذّر التصدير: ${e.message}")
            }
        }
    }
}
