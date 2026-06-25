package com.uts.editor.data

import com.uts.editor.model.TextEncoding
import org.mozilla.universalchardet.UniversalDetector
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Result of automatic encoding detection.
 *
 * @param encoding best guess
 * @param confidence 0..100
 * @param needsConfirmation true when the app should prompt the user to pick
 *        an encoding manually (with live preview) instead of opening silently.
 */
data class DetectionResult(
    val encoding: TextEncoding,
    val confidence: Int,
    val needsConfirmation: Boolean,
)

/**
 * Multi-strategy charset detection tuned so Arabic text is never silently
 * corrupted. Order of evidence, strongest first:
 *
 *  1. Byte-order mark (definitive).
 *  2. Strict UTF-8 validation (extremely low false-positive rate).
 *  3. Mozilla universalchardet statistical guess.
 *  4. Decode-and-score across the single-byte Arabic/Latin candidates, scoring
 *     by how many decoded characters land in expected Unicode ranges.
 *
 * When no strategy is confident, we still return a best guess but flag
 * [DetectionResult.needsConfirmation] so the UI asks the user.
 */
object EncodingDetector {

    private const val CONFIDENT = 90
    private const val PROMPT_BELOW = 65

    fun detect(sample: ByteArray): DetectionResult {
        if (sample.isEmpty()) {
            return DetectionResult(TextEncoding.UTF_8, 100, needsConfirmation = false)
        }

        detectBom(sample)?.let { return DetectionResult(it, 100, needsConfirmation = false) }

        // Pure ASCII is a valid UTF-8 subset — open as UTF-8.
        if (sample.all { it >= 0 }) {
            return DetectionResult(TextEncoding.UTF_8, 99, needsConfirmation = false)
        }

        // Strict UTF-8: if it decodes cleanly and uses multibyte sequences, trust it.
        if (isStrictUtf8(sample)) {
            return DetectionResult(TextEncoding.UTF_8, 97, needsConfirmation = false)
        }

        val universal = universalGuess(sample)
        val arabicBest = scoreCandidates(sample)

        // Prefer a strong Arabic single-byte score when universalchardet is unsure
        // or disagrees on Arabic content — this is the project's top priority.
        val candidates = buildList {
            universal?.let { add(it) }
            arabicBest?.let { add(it) }
        }.sortedByDescending { it.confidence }

        val best = candidates.firstOrNull()
            ?: ScoredEncoding(TextEncoding.WINDOWS_1252, 40)

        return DetectionResult(
            encoding = best.encoding,
            confidence = best.confidence,
            needsConfirmation = best.confidence < PROMPT_BELOW,
        )
    }

    private fun detectBom(b: ByteArray): TextEncoding? {
        fun starts(vararg bytes: Int): Boolean =
            b.size >= bytes.size && bytes.indices.all { (b[it].toInt() and 0xFF) == bytes[it] }
        return when {
            // UTF-32 must be checked before UTF-16 (shared FF FE prefix).
            starts(0x00, 0x00, 0xFE, 0xFF) -> TextEncoding.UTF_32BE
            starts(0xFF, 0xFE, 0x00, 0x00) -> TextEncoding.UTF_32LE
            starts(0xEF, 0xBB, 0xBF) -> TextEncoding.UTF_8_BOM
            starts(0xFE, 0xFF) -> TextEncoding.UTF_16BE
            starts(0xFF, 0xFE) -> TextEncoding.UTF_16LE
            else -> null
        }
    }

    private fun isStrictUtf8(b: ByteArray): Boolean {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(b))
            true
        } catch (_: Exception) {
            false
        }
    }

    private data class ScoredEncoding(val encoding: TextEncoding, val confidence: Int)

    private fun universalGuess(sample: ByteArray): ScoredEncoding? {
        val detector = UniversalDetector(null)
        detector.handleData(sample, 0, sample.size)
        detector.dataEnd()
        val name = detector.detectedCharset
        detector.reset()
        if (name.isNullOrBlank()) return null
        val enc = mapName(name) ?: return null
        // universalchardet doesn't expose a numeric confidence; treat a concrete
        // hit as moderately strong, but never as strong as UTF-8 validation.
        return ScoredEncoding(enc, 80)
    }

    private fun mapName(raw: String): TextEncoding? {
        val n = raw.trim().uppercase().replace("_", "-")
        TextEncoding.ALL.firstOrNull { it.charset().name().uppercase().replace("_", "-") == n }
            ?.let { return it }
        return when {
            n.contains("UTF-8") -> TextEncoding.UTF_8
            n.contains("1256") -> TextEncoding.WINDOWS_1256
            n.contains("8859-6") -> TextEncoding.ISO_8859_6
            n.contains("1252") -> TextEncoding.WINDOWS_1252
            n.contains("8859-1") || n == "LATIN1" -> TextEncoding.ISO_8859_1
            n.contains("SHIFT") || n.contains("SJIS") -> TextEncoding.SHIFT_JIS
            n.contains("EUC-KR") -> TextEncoding.EUC_KR
            n.contains("GB") -> TextEncoding.GBK
            else -> {
                // Unknown but platform-supported charset (e.g. windows-1251): wrap it.
                runCatching {
                    if (java.nio.charset.Charset.isSupported(raw)) {
                        TextEncoding(raw.lowercase(), raw, raw)
                    } else null
                }.getOrNull()
            }
        }
    }

    /**
     * Decode the sample with each single-byte candidate and choose between them.
     *
     * The decisive, direction-safe rule: a charset that produces a *substantial
     * density* of Arabic letters is almost certainly the right Arabic codepage,
     * because Latin codepages produce ~zero Arabic from the same bytes. If no
     * candidate yields meaningful Arabic, the text is Latin/other and we pick the
     * cleanest Latin decoding (fewest replacement / C1-control characters). This
     * prevents both failure modes: Arabic mis-read as Latin, and Latin text with
     * a few accents mis-read as Arabic.
     */
    private fun scoreCandidates(sample: ByteArray): ScoredEncoding? {
        val arabicCandidates = listOf(
            TextEncoding.WINDOWS_1256,
            TextEncoding.ISO_8859_6,
            TextEncoding.CP720,
        ).filter { it.isAvailable() }
        val latinCandidates = listOf(
            TextEncoding.WINDOWS_1252,
            TextEncoding.ISO_8859_1,
        ).filter { it.isAvailable() }

        val scans = (arabicCandidates + latinCandidates).mapNotNull { enc ->
            decodeLossy(sample, enc)?.let { enc to scan(it) }
        }
        if (scans.isEmpty()) return null

        // Best Arabic decoding by absolute Arabic-letter count.
        val bestArabic = scans
            .filter { it.first in arabicCandidates }
            .maxByOrNull { it.second.arabic }

        bestArabic?.let { (enc, s) ->
            val arabicRatio = if (s.total == 0) 0.0 else s.arabic.toDouble() / s.total
            // Genuine Arabic forms multi-letter words (contiguous runs); European
            // accents mis-decoded as Arabic appear as isolated single characters
            // wedged between Latin letters. Require most Arabic chars to be clustered.
            val clustered = if (s.arabic == 0) 0.0 else s.arabicInRuns.toDouble() / s.arabic
            // Significant, clustered Arabic content -> confidently an Arabic codepage.
            if (s.arabic > 0 && arabicRatio >= 0.12 && clustered >= 0.5) {
                val confidence = (70 + (arabicRatio * 60)).toInt().coerceIn(70, 95)
                // Penalise candidates that needed replacements (e.g. ISO-8859-6 gaps).
                val cleanest = scans
                    .filter { it.first in arabicCandidates && it.second.arabic >= s.arabic * 0.9 }
                    .minByOrNull { it.second.replacement }!!
                return ScoredEncoding(cleanest.first, confidence)
            }
        }

        // No meaningful Arabic -> the text is Latin/other. Rank only the Latin
        // candidates so a stray accent decoded as Arabic can't hijack the result;
        // windows-1252 (a superset of ISO-8859-1) is the safe default on a tie.
        val latinScans = scans.filter { it.first in latinCandidates }
        val best = (latinScans.ifEmpty { scans }).maxByOrNull { weighted(it.second) } ?: return null
        // Latin/ASCII text is only moderately certain (1252 vs 8859-1 ambiguity).
        val conf = if (best.second.replacement == 0 && best.second.c1 == 0) 72 else 55
        return ScoredEncoding(best.first, conf)
    }

    private fun decodeLossy(b: ByteArray, enc: TextEncoding): String? = runCatching {
        val decoder = enc.charset().newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        decoder.decode(ByteBuffer.wrap(b)).toString()
    }.getOrNull()

    private data class Scan(
        val arabic: Int,
        val arabicInRuns: Int,  // Arabic chars belonging to a run of length >= 2
        val ascii: Int,
        val highLatin: Int,
        val replacement: Int,
        val c1: Int,
        val control: Int,
        val total: Int,
    )

    private fun isArabic(code: Int) = code in 0x0600..0x06FF || code in 0x0750..0x077F

    private fun scan(text: String): Scan {
        var arabic = 0; var ascii = 0; var highLatin = 0
        var replacement = 0; var c1 = 0; var control = 0
        var arabicInRuns = 0; var run = 0
        for (ch in text) {
            val code = ch.code
            if (isArabic(code)) {
                arabic++; run++
            } else {
                if (run >= 2) arabicInRuns += run
                run = 0
                when {
                    ch == '�' -> replacement++
                    code == 0x09 || code == 0x0A || code == 0x0D -> ascii++
                    code in 0x20..0x7E -> ascii++
                    code in 0x80..0x9F -> c1++             // C1 controls: strong "wrong charset" signal
                    code in 0xA0..0x24F -> highLatin++
                    code < 0x20 -> control++
                    else -> { /* other scripts: neutral */ }
                }
            }
        }
        if (run >= 2) arabicInRuns += run
        return Scan(arabic, arabicInRuns, ascii, highLatin, replacement, c1, control, text.length)
    }

    /** Absolute readability weight (Arabic-agnostic): used only as a fallback ranker. */
    private fun weighted(s: Scan): Int =
        s.arabic * 3 + s.ascii + s.highLatin - s.replacement * 5 - s.c1 * 4 - s.control * 3
}
