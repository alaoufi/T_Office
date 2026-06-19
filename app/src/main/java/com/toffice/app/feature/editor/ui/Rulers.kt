package com.toffice.app.feature.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.toffice.app.feature.editor.model.PT_PER_CM
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val RULER_THICK_DP = 22

/** المسطرة العلوية: أرقام بالسنتيمتر + مقبضان لسحب الهامش الأيسر والأيمن. */
@Composable
fun HorizontalRuler(
    pageWidthPt: Float,
    marginLeftPt: Float,
    marginRightPt: Float,
    scale: Float, // dp لكل نقطة
    rtl: Boolean = false,
    firstIndentPt: Float = 0f,
    leftIndentPt: Float = 0f,
    maxIndentPt: Float = 0f,
    onIndentChange: (firstPt: Float, leftPt: Float) -> Unit = { _, _ -> },
    onChange: (left: Float, right: Float) -> Unit,
) {
    val density = LocalDensity.current.density
    val pxPerPt = scale * density
    val widthDp = pageWidthPt * scale

    val tick = MaterialTheme.colorScheme.outline
    val shade = MaterialTheme.colorScheme.surfaceVariant
    val rulerBg = MaterialTheme.colorScheme.surface
    val handleColor = MaterialTheme.colorScheme.primary

    val curLeft by rememberUpdatedState(marginLeftPt)
    val curRight by rememberUpdatedState(marginRightPt)
    val onChangeState by rememberUpdatedState(onChange)
    val halfHandlePx = (HANDLE_DP * density / 2f).roundToInt()

    // نُثبّت اتجاه المسطرة على LTR داخلياً حتى تكون إحداثيات المقابض فيزيائية،
    // ونتولّى عكس الأرقام يدوياً عبر mapX حسب rtl (لتفادي الانعكاس المزدوج).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Box(Modifier.width(widthDp.dp).height(RULER_THICK_DP.dp).background(rulerBg)) {
        Canvas(Modifier.size(widthDp.dp, RULER_THICK_DP.dp)) {
            val h = size.height
            val leftX = marginLeftPt * pxPerPt
            val rightX = (pageWidthPt - marginRightPt) * pxPerPt
            // تظليل منطقة الهوامش (فيزيائي)
            drawRect(shade, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(leftX, h))
            drawRect(shade, topLeft = androidx.compose.ui.geometry.Offset(rightX, 0f),
                size = androidx.compose.ui.geometry.Size(size.width - rightX, h))

            // الترقيم بالسنتيمتر يبدأ من هامش البداية (مثل WPS):
            // البداية = الهامش الأيسر (LTR) أو الأيمن (RTL)، والاتجاه نحو المتن.
            val originPt = if (rtl) pageWidthPt - marginRightPt else marginLeftPt
            val sign = if (rtl) -1f else 1f
            val maxCm = ceil(pageWidthPt / PT_PER_CM).toInt()
            for (d in -maxCm..maxCm) {
                val x = (originPt + sign * d * PT_PER_CM) * pxPerPt
                if (x in 0f..size.width) {
                    drawLine(tick, androidx.compose.ui.geometry.Offset(x, h * 0.45f),
                        androidx.compose.ui.geometry.Offset(x, h), strokeWidth = 1.5f)
                }
                val xHalf = (originPt + sign * (d + 0.5f) * PT_PER_CM) * pxPerPt
                if (xHalf in 0f..size.width) {
                    drawLine(tick, androidx.compose.ui.geometry.Offset(xHalf, h * 0.7f),
                        androidx.compose.ui.geometry.Offset(xHalf, h), strokeWidth = 1f)
                }
            }
            // الأرقام (القيمة = المسافة عن الهامش بالسنتيمتر، والصفر لا يُكتب)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = tick.toArgb()
                    textSize = 9f * density
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                for (d in -maxCm..maxCm) {
                    if (d == 0) continue
                    val x = (originPt + sign * d * PT_PER_CM) * pxPerPt
                    if (x in 0f..size.width) drawText(abs(d).toString(), x, h * 0.42f, paint)
                }
            }
        }
        // مقبض الهامش الأيسر (فيزيائي)
        DragHandle(
            color = handleColor,
            offsetXProvider = { (curLeft * pxPerPt).roundToInt() - halfHandlePx },
        ) { dxPx ->
            val deltaPt = dxPx / pxPerPt
            onChangeState((curLeft + deltaPt).coerceIn(0f, pageWidthPt * 0.45f), curRight)
        }
        // مقبض الهامش الأيمن (فيزيائي)
        DragHandle(
            color = handleColor,
            offsetXProvider = { ((pageWidthPt - curRight) * pxPerPt).roundToInt() - halfHandlePx },
        ) { dxPx ->
            val deltaPt = dxPx / pxPerPt
            onChangeState(curLeft, (curRight - deltaPt).coerceIn(0f, pageWidthPt * 0.45f))
        }
        // علامتا المسافة البادئة للفقرة المركّزة: السطر الأول (أعلى) وبقية الأسطر (أسفل)
        if (maxIndentPt > 0f) {
            val originPt = if (rtl) pageWidthPt - marginRightPt else marginLeftPt
            val sign = if (rtl) -1f else 1f
            val curFirst by rememberUpdatedState(firstIndentPt)
            val curLeft2 by rememberUpdatedState(leftIndentPt)
            val onIndentState by rememberUpdatedState(onIndentChange)
            val halfTriPx = (TRI_DP * density / 2f).roundToInt()
            val bottomYPx = ((RULER_THICK_DP - TRI_DP) * density).roundToInt()
            // علامة السطر الأول (مثلث للأسفل، أعلى المسطرة)
            IndentHandle(
                color = handleColor,
                pointDown = true,
                yPx = 0,
                offsetXProvider = { ((originPt + sign * curFirst) * pxPerPt).roundToInt() - halfTriPx },
            ) { dxPx ->
                val d = sign * dxPx / pxPerPt
                onIndentState((curFirst + d).coerceIn(0f, maxIndentPt), curLeft2)
            }
            // علامة بقية الأسطر (مثلث للأعلى، أسفل المسطرة)
            IndentHandle(
                color = handleColor,
                pointDown = false,
                yPx = bottomYPx,
                offsetXProvider = { ((originPt + sign * curLeft2) * pxPerPt).roundToInt() - halfTriPx },
            ) { dxPx ->
                val d = sign * dxPx / pxPerPt
                onIndentState(curFirst, (curLeft2 + d).coerceIn(0f, maxIndentPt))
            }
        }
    }
    }
}

/** المسطرة الجانبية: أرقام بالسنتيمتر + مقبضان لسحب الهامش العلوي والسفلي. */
@Composable
fun VerticalRuler(
    pageHeightPt: Float,
    heightDp: Float,
    marginTopPt: Float,
    marginBottomPt: Float,
    scale: Float,
    onChange: (top: Float, bottom: Float) -> Unit,
) {
    val density = LocalDensity.current.density
    val pxPerPt = scale * density

    val tick = MaterialTheme.colorScheme.outline
    val shade = MaterialTheme.colorScheme.surfaceVariant
    val rulerBg = MaterialTheme.colorScheme.surface
    val handleColor = MaterialTheme.colorScheme.primary

    val curTop by rememberUpdatedState(marginTopPt)
    val curBottom by rememberUpdatedState(marginBottomPt)
    val onChangeState by rememberUpdatedState(onChange)
    val halfHandlePx = (HANDLE_DP * density / 2f).roundToInt()

    Box(Modifier.width(RULER_THICK_DP.dp).height(heightDp.dp).background(rulerBg)) {
        Canvas(Modifier.size(RULER_THICK_DP.dp, heightDp.dp)) {
            val w = size.width
            val pageHeightPx = pageHeightPt * pxPerPt
            val pages = if (pageHeightPx > 0f) ceil(size.height / pageHeightPx).toInt().coerceAtLeast(1) else 1
            val maxCm = ceil(pageHeightPt / PT_PER_CM).toInt()
            val paint = android.graphics.Paint().apply {
                color = tick.toArgb()
                textSize = 9f * density
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            // الترقيم يعيد البدء من الهامش العلوي لكل صفحة (مثل WPS)
            for (p in 0 until pages) {
                val pageTop = p * pageHeightPx
                // تظليل الهامش العلوي والسفلي لكل صفحة
                drawRect(shade, topLeft = androidx.compose.ui.geometry.Offset(0f, pageTop),
                    size = androidx.compose.ui.geometry.Size(w, (marginTopPt * pxPerPt).coerceAtLeast(0f)))
                val pageBottom = (pageTop + pageHeightPx).coerceAtMost(size.height)
                val botStart = pageTop + pageHeightPx - marginBottomPt * pxPerPt
                if (botStart < pageBottom) {
                    drawRect(shade, topLeft = androidx.compose.ui.geometry.Offset(0f, botStart),
                        size = androidx.compose.ui.geometry.Size(w, pageBottom - botStart))
                }
                for (d in -maxCm..maxCm) {
                    val y = pageTop + (marginTopPt + d * PT_PER_CM) * pxPerPt
                    if (y < pageTop || y > pageBottom || y > size.height) continue
                    drawLine(tick, androidx.compose.ui.geometry.Offset(w * 0.45f, y),
                        androidx.compose.ui.geometry.Offset(w, y), strokeWidth = 1.5f)
                    if (d != 0) drawContext.canvas.nativeCanvas.drawText(abs(d).toString(), w * 0.25f, y + 3f * density, paint)
                }
            }
        }
        DragHandleVertical(
            color = handleColor,
            offsetYProvider = { (curTop * pxPerPt).roundToInt() - halfHandlePx },
        ) { dyPx ->
            val deltaPt = dyPx / pxPerPt
            onChangeState((curTop + deltaPt).coerceIn(0f, pageHeightPt * 0.4f), curBottom)
        }
        DragHandleVertical(
            color = handleColor,
            offsetYProvider = { ((pageHeightPt - curBottom) * pxPerPt).roundToInt() - halfHandlePx },
        ) { dyPx ->
            val deltaPt = dyPx / pxPerPt
            onChangeState(curTop, (curBottom - deltaPt).coerceIn(0f, pageHeightPt * 0.4f))
        }
    }
}

private const val HANDLE_DP = 14
private const val TRI_DP = 11

/** علامة مسافة بادئة مثلّثة قابلة للسحب (للأسفل = سطر أول، للأعلى = بقية الأسطر). */
@Composable
private fun IndentHandle(
    color: Color,
    pointDown: Boolean,
    yPx: Int,
    offsetXProvider: () -> Int,
    onDrag: (dxPx: Float) -> Unit,
) {
    val shape = remember(pointDown) {
        GenericShape { s, _ ->
            if (pointDown) {
                moveTo(0f, 0f); lineTo(s.width, 0f); lineTo(s.width / 2f, s.height)
            } else {
                moveTo(s.width / 2f, 0f); lineTo(s.width, s.height); lineTo(0f, s.height)
            }
            close()
        }
    }
    Box(
        Modifier
            .offset { IntOffset(offsetXProvider(), yPx) }
            .size(TRI_DP.dp)
            .clip(shape)
            .background(color)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            },
    )
}

@Composable
private fun DragHandle(
    color: Color,
    offsetXProvider: () -> Int,
    onDrag: (dxPx: Float) -> Unit,
) {
    Box(
        Modifier
            .offset { IntOffset(offsetXProvider(), 0) }
            .size(HANDLE_DP.dp)
            .background(color, RoundedCornerShape(3.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            },
    )
}

@Composable
private fun DragHandleVertical(
    color: Color,
    offsetYProvider: () -> Int,
    onDrag: (dyPx: Float) -> Unit,
) {
    Box(
        Modifier
            .offset { IntOffset(0, offsetYProvider()) }
            .size(HANDLE_DP.dp)
            .background(color, RoundedCornerShape(3.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y)
                }
            },
    )
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt(),
)
