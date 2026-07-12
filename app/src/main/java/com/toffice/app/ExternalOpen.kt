package com.toffice.app

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * حامل بسيط لملفٍ خارجي طُلب فتحه عبر «فتح بواسطة» (ACTION_VIEW).
 * تلتقطه MainActivity وتستهلكه شاشة قائمة المستندات لفتحه/استيراده.
 */
object ExternalOpen {
    data class Pending(val uri: Uri, val mime: String?)

    var pending by mutableStateOf<Pending?>(null)
        private set

    fun set(uri: Uri, mime: String?) {
        pending = Pending(uri, mime)
    }

    fun consume(): Pending? {
        val p = pending
        pending = null
        return p
    }
}
