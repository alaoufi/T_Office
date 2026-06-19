package com.toffice.app.feature.editor

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** قارئ PDF داخلي يعرض الصفحات صفحةً صفحةً (تحميل كسول لتوفير الذاكرة). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(uri: Uri, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val mutex = remember { Mutex() }
    var renderer by remember(uri) { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember(uri) { mutableIntStateOf(0) }
    var error by remember(uri) { mutableStateOf<String?>(null) }

    val targetWidthPx = with(LocalDensity.current) {
        (LocalConfiguration.current.screenWidthDp.dp.toPx() - 24f).toInt().coerceIn(200, 2000)
    }

    DisposableEffect(uri) {
        var pfd: ParcelFileDescriptor? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val r = PdfRenderer(pfd)
                renderer = r
                pageCount = r.pageCount
            } else {
                error = "تعذّر فتح الملف"
            }
        } catch (e: Exception) {
            error = "تعذّر فتح PDF: ${e.message}"
        }
        onDispose {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        val err = error
        if (err != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(err) }
            return@Scaffold
        }
        val r = renderer
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFE0E0E0)),
            contentPadding = PaddingValues(12.dp, padding.calculateTopPadding() + 12.dp, 12.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(pageCount) { index ->
                var bitmap by remember(index, r) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(index, r) {
                    val rend = r ?: return@LaunchedEffect
                    bitmap = withContext(Dispatchers.IO) {
                        try {
                            mutex.withLock {
                                val p = rend.openPage(index)
                                val h = (targetWidthPx.toFloat() * p.height / p.width).toInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(targetWidthPx, h, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(android.graphics.Color.WHITE)
                                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                p.close()
                                bmp
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "صفحة ${index + 1}",
                        modifier = Modifier.fillMaxWidth().shadow(2.dp),
                        contentScale = ContentScale.FillWidth,
                    )
                } else {
                    Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
