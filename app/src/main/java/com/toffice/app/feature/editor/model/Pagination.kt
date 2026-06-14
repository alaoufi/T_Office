package com.toffice.app.feature.editor.model

import kotlin.math.ceil
import kotlin.math.max

/**
 * عدد الصفحات المرئية بناءً على ارتفاع المحتوى الفعلي وارتفاع الصفحة الواحدة.
 * يُستخدم لرسم فواصل الصفحات ومدّ المسطرة الجانبية. (دالة نقيّة قابلة للاختبار.)
 */
fun paginationPageCount(contentHeight: Float, pageHeight: Float): Int {
    if (pageHeight <= 0f) return 1
    return max(1, ceil((contentHeight - 0.5f) / pageHeight).toInt())
}

/** مواضع فواصل الصفحات (بنفس وحدة الارتفاع) للصفحات الداخلية فقط. */
fun pageBreakOffsets(pageCount: Int, pageHeight: Float): List<Float> =
    (1 until pageCount).map { it * pageHeight }
