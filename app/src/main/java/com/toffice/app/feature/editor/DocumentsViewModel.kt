package com.toffice.app.feature.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toffice.app.core.settings.SettingsRepository
import com.toffice.app.data.document.DocumentDao
import com.toffice.app.data.document.DocumentEntity
import com.toffice.app.feature.editor.io.DocReader
import com.toffice.app.feature.editor.model.DocBundle
import com.toffice.app.feature.editor.model.DocSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DocumentsUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val dao: DocumentDao,
    private val settings: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState = dao.observeAll()
        .map { DocumentsUiState(it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentsUiState())

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        // تنظيف عميق عند البدء: احذف صور المستندات المتبقّية غير المستخدمة (بقايا استيراد متكرّر)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val docs = dao.observeAll().first()
                val used = HashSet<String>()
                val re = Regex("\"p\":\"([^\"]+)\"")
                docs.forEach { d -> re.findAll(d.contentJson).forEach { used.add(it.groupValues[1]) } }
                com.toffice.app.feature.editor.io.ImageStore.cleanupOrphans(context, used)
            } catch (_: Exception) {
            }
        }
    }

    fun createNew(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val page = settings.defaultPageSettings()
            val json = DocSerializer.serialize(DocBundle(AnnotatedString(""), page))
            val id = dao.insert(DocumentEntity(title = "مستند جديد", contentJson = json))
            onCreated(id)
        }
    }

    fun importDocx(uri: Uri, onImported: (Long) -> Unit) {
        viewModelScope.launch {
            // الاحتفاظ بإذن دائم للقراءة والكتابة (للحفظ بنفس الملف لاحقاً)
            val canWriteBack = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }.isSuccess

            val json = withContext(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes == null) null else {
                        val bundle = saveImages(DocReader.readAny(bytes))
                        DocSerializer.serialize(bundle)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (json == null) {
                _events.emit("تعذّر فتح ملف Word")
                return@launch
            }
            val name = displayName(uri)
            val id = dao.insert(
                DocumentEntity(
                    title = name,
                    contentJson = json,
                    sourceUri = if (canWriteBack) uri.toString() else null,
                )
            )
            _events.emit("تم فتح الملف")
            onImported(id)
        }
    }

    fun delete(doc: DocumentEntity) {
        viewModelScope.launch {
            dao.delete(doc)
            // احذف صور هذا المستند من التخزين الداخلي
            withContext(Dispatchers.IO) {
                Regex("\"p\":\"([^\"]+)\"").findAll(doc.contentJson).forEach {
                    com.toffice.app.feature.editor.io.ImageStore.delete(it.groupValues[1])
                }
            }
        }
    }

    /** يحفظ بايتات صور Word المستخرجة داخلياً ويحوّلها إلى كتل صور. */
    private fun saveImages(b: DocBundle): DocBundle {
        if (b.imageData.isEmpty()) return b
        val saved = b.imageData.mapNotNull { com.toffice.app.feature.editor.io.ImageStore.importBytes(context, it) }
        return b.copy(images = b.images + saved, imageData = emptyList())
    }

    private fun displayName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) {
                    c.getString(idx).substringBeforeLast(".").ifBlank { "مستند مستورد" }
                } else "مستند مستورد"
            } ?: "مستند مستورد"
        } catch (e: Exception) {
            "مستند مستورد"
        }
    }
}
