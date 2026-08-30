package com.toffice.app.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.toffice.app.ExternalOpen

/**
 * شاشة البدء: تفتح آخر مستند مباشرةً في المحرّر (أو مستنداً جديداً)،
 * إلا إذا كان هناك ملف خارجي قادم عبر «فتح بواسطة» فتحوّل إلى قائمة المستندات لمعالجته.
 */
@Composable
fun LaunchScreen(
    onReady: (Long) -> Unit,
    onExternal: () -> Unit,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        if (ExternalOpen.pending != null) {
            onExternal()
        } else {
            viewModel.openLatestOrNew(onReady)
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
