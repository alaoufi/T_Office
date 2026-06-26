package com.uts.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uts.editor.editor.SyntaxColors
import com.uts.editor.editor.SyntaxHighlighter
import com.uts.editor.model.SyntaxLanguage

/**
 * The scrollable editor surface: an optional line-number gutter plus a
 * [BasicTextField] whose text uses [TextDirection.Content] so Arabic lines
 * render RTL and Latin lines LTR (mixed lines handled by the platform BiDi
 * algorithm — digits, brackets and punctuation stay correct).
 */
@Composable
fun EditorArea(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: SyntaxLanguage,
    syntaxEnabled: Boolean,
    syntaxColors: SyntaxColors,
    fontSizeSp: Float,
    showLineNumbers: Boolean,
    wordWrap: Boolean,
    readOnly: Boolean,
    matches: List<IntRange>,
    currentMatch: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scroll = rememberScrollState()
    val hScroll = rememberScrollState()

    val gutterColor = MaterialTheme.colorScheme.surfaceVariant
    val gutterText = MaterialTheme.colorScheme.onSurfaceVariant
    val matchBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
    val currentBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    // Use a monospace face only for code; plain text (where Arabic documents
    // live) uses the default family, whose Arabic shaping is correct on all
    // devices — the system monospace font garbles Arabic on some OEM ROMs.
    val editorFont = if (language == SyntaxLanguage.PLAIN) FontFamily.Default else FontFamily.Monospace
    val baseStyle = LocalTextStyle.current.merge(
        TextStyle(
            fontFamily = editorFont,
            fontSize = fontSizeSp.sp,
            // Comfortable line spacing so lines are clearly separated and readable.
            lineHeight = (fontSizeSp * 1.6f).sp,
            color = MaterialTheme.colorScheme.onSurface,
            textDirection = TextDirection.Content,
        )
    )

    // Pre-compute logical line start offsets for the gutter (cheap, on text change).
    val lineStarts = remember(value.text) { computeLineStarts(value.text) }

    val transformation = remember(language, syntaxEnabled, syntaxColors, matches, currentMatch) {
        VisualTransformation { input ->
            val annotated = if (syntaxEnabled) {
                SyntaxHighlighter.highlight(input.text, language, syntaxColors)
            } else {
                androidx.compose.ui.text.AnnotatedString(input.text)
            }
            val withMatches = buildAnnotatedString {
                append(annotated)
                matches.forEachIndexed { i, r ->
                    val bg = if (i == currentMatch) currentBg else matchBg
                    val start = r.first.coerceIn(0, input.text.length)
                    val end = r.last.coerceIn(0, input.text.length)
                    if (start < end) addStyle(SpanStyle(background = bg), start, end)
                }
            }
            TransformedText(withMatches, androidx.compose.ui.text.input.OffsetMapping.Identity)
        }
    }

    val scrollModifier = if (wordWrap) {
        Modifier.verticalScroll(scroll)
    } else {
        Modifier.verticalScroll(scroll).horizontalScroll(hScroll)
    }

    Row(modifier = modifier.fillMaxSize().then(scrollModifier)) {
        if (showLineNumbers && lineStarts.size <= MAX_GUTTER_LINES) {
            val gutterWidthDp = (12 + lineStarts.size.toString().length * 9).dp
            Box(
                Modifier
                    .width(gutterWidthDp)
                    .background(gutterColor)
                    .drawBehind {
                        val lr = layout ?: return@drawBehind
                        val paint = android.graphics.Paint().apply {
                            color = gutterText.toArgb()
                            textSize = fontSizeSp * density.density
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                        for (i in lineStarts.indices) {
                            val offset = lineStarts[i]
                            if (offset > lr.layoutInput.text.length) break
                            val lineTop = lr.getLineTop(lr.getLineForOffset(offset))
                            val baseline = lineTop + (fontSizeSp * density.density)
                            drawContext.canvas.nativeCanvas.drawText(
                                (i + 1).toString(),
                                size.width - 6f,
                                baseline,
                                paint,
                            )
                        }
                    }
            ) {}
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            textStyle = baseStyle,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = transformation,
            onTextLayout = { layout = it },
            modifier = Modifier
                .padding(start = 6.dp)
                .then(if (wordWrap) Modifier.fillMaxWidth() else Modifier),
        )
    }
}

private const val MAX_GUTTER_LINES = 50_000

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)

private fun computeLineStarts(text: String): List<Int> {
    val starts = ArrayList<Int>()
    starts.add(0)
    var i = 0
    while (i < text.length) {
        if (text[i] == '\n') starts.add(i + 1)
        i++
    }
    return starts
}
