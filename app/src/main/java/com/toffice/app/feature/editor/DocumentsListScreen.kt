package com.toffice.app.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toffice.app.R
import com.toffice.app.data.document.DocumentEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsListScreen(
    onBack: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importDocx(uri) { id -> onOpenDocument(id) }
    }

    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pdfUri = uri }

    // فتح ملف خارجي جاء عبر «فتح بواسطة» (VIEW): PDF في القارئ، وWord عبر الاستيراد
    val extContext = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(com.toffice.app.ExternalOpen.pending) {
        val p = com.toffice.app.ExternalOpen.consume() ?: return@LaunchedEffect
        val mime = (p.mime ?: runCatching { extContext.contentResolver.getType(p.uri) }.getOrNull()).orEmpty()
        if (mime.contains("pdf") || p.uri.toString().lowercase(Locale.ROOT).endsWith(".pdf")) {
            pdfUri = p.uri
        } else {
            viewModel.importDocx(p.uri) { id -> onOpenDocument(id) }
        }
    }

    val currentPdf = pdfUri
    if (currentPdf != null) {
        PdfViewerScreen(uri = currentPdf, title = "قارئ PDF", onBack = { pdfUri = null })
        return
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_editor) + " (Word)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "فتح PDF")
                    }
                    IconButton(onClick = {
                        importLauncher.launch(arrayOf(MIME_DOCX, "application/msword", "*/*"))
                    }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "فتح ملف Word")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.createNew { id -> onOpenDocument(id) } },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("مستند جديد") },
            )
        },
    ) { padding ->
        if (state.documents.isEmpty() && !state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("لا توجد مستندات — أنشئ مستنداً جديداً أو افتح ملف Word")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp, padding.calculateTopPadding() + 12.dp, 12.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.documents, key = { it.id }) { doc ->
                    DocumentRow(
                        doc = doc,
                        onOpen = { onOpenDocument(doc.id) },
                        onDelete = { viewModel.delete(doc) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(doc: DocumentEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault()) }
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = fmt.format(Date(doc.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
