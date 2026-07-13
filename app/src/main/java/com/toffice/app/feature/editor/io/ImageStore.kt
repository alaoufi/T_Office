package com.toffice.app.feature.editor.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.toffice.app.feature.editor.model.DocImage
import java.io.File
import java.util.UUID

/** يدير صور المستند: نسخ داخلي أوفلاين + فكّ ترميز آمن بأبعاد محدودة. */
object ImageStore {

    private const val DEFAULT_WIDTH_PT = 240f
    private const val MAX_DECODE_PX = 1280

    /** ينسخ الصورة المختارة إلى تخزين التطبيق الداخلي ويعيد وصفها (أوفلاين دائماً). */
    fun importImage(context: Context, uri: Uri): DocImage? = try {
        val dir = File(context.filesDir, "doc_images").apply { mkdirs() }
        val ext = extOf(context, uri)
        val file = File(dir, "${UUID.randomUUID()}.$ext")
        val copied = context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
            true
        } ?: false
        if (!copied) {
            null
        } else {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, opts)
            val iw = opts.outWidth.coerceAtLeast(1)
            val ih = opts.outHeight.coerceAtLeast(1)
            val w = DEFAULT_WIDTH_PT
            val h = (w * ih / iw).coerceAtLeast(20f)
            DocImage(file.path, w, h)
        }
    } catch (e: Exception) {
        null
    }

    /** يحفظ صورة من بايتات (مستخرجة من Word) داخلياً ويعيد وصفها. */
    fun importBytes(context: Context, bytes: ByteArray, ext: String = "jpg"): DocImage? = try {
        val dir = File(context.filesDir, "doc_images").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.$ext")
        file.outputStream().use { it.write(bytes) }
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, opts)
        val iw = opts.outWidth.coerceAtLeast(1)
        val ih = opts.outHeight.coerceAtLeast(1)
        if (opts.outWidth <= 0) { file.delete(); null } // ليست صورة صالحة (مثل EMF)
        else {
            val w = DEFAULT_WIDTH_PT
            val h = (w * ih / iw).coerceAtLeast(20f)
            DocImage(file.path, w, h)
        }
    } catch (e: Exception) {
        null
    }

    /** يفكّ ترميز ملف الصورة بأبعاد محدودة (حماية الذاكرة). */
    fun decode(path: String, maxPx: Int = MAX_DECODE_PX): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        val larger = maxOf(bounds.outWidth, bounds.outHeight)
        while (larger / sample > maxPx) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (e: Exception) {
        null
    }

    /** يحذف ملف الصورة الداخلي (نسخة خاصة بنا فقط). */
    fun delete(path: String) {
        runCatching { val f = File(path); if (f.exists() && f.path.contains("doc_images")) f.delete() }
    }

    private fun extOf(context: Context, uri: Uri): String {
        val type = context.contentResolver.getType(uri)
        return when {
            type?.contains("png") == true -> "png"
            type?.contains("webp") == true -> "webp"
            else -> "jpg"
        }
    }
}
