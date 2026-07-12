package com.toffice.app.feature.editor.io

import androidx.compose.ui.text.AnnotatedString
import com.toffice.app.feature.editor.model.DocBundle
import java.io.ByteArrayOutputStream

/**
 * قارئ مبسّط لصيغة Word الثنائية القديمة (.doc / OLE2 Compound File).
 * يستخرج النص فقط (بلا تنسيق غني) عبر حاوية CFB + جدول القطع (piece table) في FIB.
 * ليس تطابقاً كاملاً — لكنه يفتح ملفات .doc التي كانت تظهر فارغة.
 */
object DocReader {

    /** التوقيع الثنائي لملفات OLE2. */
    val MAGIC = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte())

    fun isDoc(head: ByteArray): Boolean =
        head.size >= 4 && head[0] == MAGIC[0] && head[1] == MAGIC[1] && head[2] == MAGIC[2] && head[3] == MAGIC[3]

    fun read(bytes: ByteArray): DocBundle {
        val text = runCatching { extractText(bytes) }.getOrNull()?.trim().orEmpty()
        return DocBundle(body = AnnotatedString(text))
    }

    /** يقرأ المستند تلقائياً: docx (ZIP) أو doc (OLE2) حسب التوقيع الثنائي. */
    fun readAny(bytes: ByteArray): DocBundle =
        if (isDoc(bytes)) read(bytes)
        else DocxReader.read(bytes)

    /** يستخرج النص الخام للمتن الرئيسي من ملف .doc. */
    internal fun extractText(data: ByteArray): String {
        val cfb = Cfb(data)
        val wd = cfb.readStream("WordDocument") ?: return ""
        if (u16(wd, 0) != 0xA5EC) return "" // wIdent

        val flags = u16(wd, 0x0A)
        val whichTable = if (flags and 0x0200 != 0) "1Table" else "0Table"
        val ccpText = i32(wd, 0x4C).coerceAtLeast(0)
        val fcClx = i32(wd, 0x01A2)
        val lcbClx = i32(wd, 0x01A6)

        val table = cfb.readStream(whichTable) ?: cfb.readStream("1Table") ?: cfb.readStream("0Table")

        // مسار CLX / جدول القطع (الأدقّ) — يدعم الملفات البسيطة والمعقّدة
        if (table != null && lcbClx > 0 && fcClx >= 0 && fcClx + lcbClx <= table.size) {
            val fromPieces = runCatching { readViaPieceTable(wd, table, fcClx, lcbClx, ccpText) }.getOrNull()
            if (!fromPieces.isNullOrBlank()) return cleanup(fromPieces)
        }
        // احتياطي: نص UTF-16 مباشر بعد رأس FIB (للملفات البسيطة جداً)
        val fcMin = i32(wd, 0x18)
        val fcMac = i32(wd, 0x1C)
        if (fcMin in 0 until fcMac && fcMac <= wd.size) {
            val raw = String(wd.copyOfRange(fcMin, fcMac), Charsets.UTF_16LE)
            if (raw.isNotBlank()) return cleanup(raw)
        }
        return ""
    }

    private fun readViaPieceTable(wd: ByteArray, table: ByteArray, fcClx: Int, lcbClx: Int, ccpText: Int): String {
        var pos = fcClx
        val end = fcClx + lcbClx
        // تخطّي عناصر Prc (تبدأ بـ 0x01)
        while (pos < end && table[pos].toInt() and 0xFF == 0x01) {
            val cb = u16(table, pos + 1)
            pos += 3 + cb
        }
        if (pos >= end || table[pos].toInt() and 0xFF != 0x02) return ""
        val lcbPlcfpcd = i32(table, pos + 1)
        val plcStart = pos + 5
        if (plcStart + lcbPlcfpcd > table.size) return ""
        val n = (lcbPlcfpcd - 4) / 12
        if (n <= 0) return ""
        val cps = IntArray(n + 1) { i32(table, plcStart + it * 4) }
        val pcdStart = plcStart + (n + 1) * 4
        val sb = StringBuilder()
        for (i in 0 until n) {
            val pcd = pcdStart + i * 8
            val fcRaw = i32(table, pcd + 2)
            val compressed = fcRaw and 0x40000000 != 0
            val fc = fcRaw and 0x3FFFFFFF
            val cpLen = cps[i + 1] - cps[i]
            if (cpLen <= 0) continue
            if (compressed) {
                val off = fc / 2
                if (off in 0..wd.size - cpLen) {
                    sb.append(String(wd.copyOfRange(off, off + cpLen), charset("windows-1256")))
                }
            } else {
                val off = fc
                if (off in 0..wd.size - cpLen * 2) {
                    sb.append(String(wd.copyOfRange(off, off + cpLen * 2), Charsets.UTF_16LE))
                }
            }
        }
        return sb.toString()
    }

    /** يحوّل علامات وورد الخاصة إلى أسطر ويزيل رموز التحكّم. */
    private fun cleanup(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                '\r', '\u000B', '\u000C', '\u0007' -> sb.append('\n') // فقرة/خلية/كسر سطر/كسر صفحة
                '\n', '\t' -> sb.append(c)
                else -> if (c.code >= 0x20 && c.code != 0xFFFF) sb.append(c)
            }
        }
        // دمج الأسطر الفارغة المتتالية (ناتجة عن علامات فقرات فارغة) وتشذيب الأطراف
        return sb.toString().replace(Regex("\n{3,}"), "\n\n").trim()
    }

    // ---- حاوية CFB (Compound File Binary) ----

    private class Cfb(val data: ByteArray) {
        val sectorSize: Int
        val miniCutoff: Int
        val fat: IntArray
        val miniFat: IntArray
        val dir: List<DirEntry>
        val miniStream: ByteArray

        init {
            val sectorShift = u16(data, 0x1E)
            sectorSize = 1 shl sectorShift
            val firstDirSector = i32(data, 0x30)
            miniCutoff = i32(data, 0x38)
            val firstMiniFat = i32(data, 0x3C)
            val firstDifat = i32(data, 0x44)
            val numDifat = i32(data, 0x48)

            // DIFAT: 109 مدخلاً في الرأس ثم قطاعات إضافية
            val difat = ArrayList<Int>()
            for (i in 0 until 109) { val s = i32(data, 0x4C + i * 4); if (s >= 0) difat.add(s) }
            var ds = firstDifat; var c = 0
            while (ds >= 0 && c < numDifat) {
                val base = off(ds)
                val perSec = sectorSize / 4 - 1
                for (i in 0 until perSec) { val s = i32(data, base + i * 4); if (s >= 0) difat.add(s) }
                ds = i32(data, base + sectorSize - 4); c++
            }
            // بناء FAT
            val f = IntArray(difat.size * (sectorSize / 4))
            var idx = 0
            for (fs in difat) {
                val base = off(fs)
                for (i in 0 until sectorSize / 4) f[idx++] = i32(data, base + i * 4)
            }
            fat = f

            val dirBytes = readChainFat(firstDirSector)
            val entries = ArrayList<DirEntry>()
            var p = 0
            while (p + 128 <= dirBytes.size) {
                val nameLen = u16(dirBytes, p + 0x40)
                val type = dirBytes[p + 0x42].toInt() and 0xFF
                if (type == 1 || type == 2 || type == 5) {
                    val chars = (nameLen / 2 - 1).coerceAtLeast(0)
                    val name = String(dirBytes.copyOfRange(p, p + chars * 2), Charsets.UTF_16LE)
                    val start = i32(dirBytes, p + 0x74)
                    val size = i32(dirBytes, p + 0x78)
                    entries.add(DirEntry(name, type, start, size))
                }
                p += 128
            }
            dir = entries

            val root = entries.firstOrNull { it.type == 5 }
            miniStream = if (root != null) readChainFat(root.start).let { it.copyOf(minOf(root.size.coerceAtLeast(0), it.size)) } else ByteArray(0)
            val mfBytes = if (firstMiniFat >= 0) readChainFat(firstMiniFat) else ByteArray(0)
            miniFat = IntArray(mfBytes.size / 4) { i32(mfBytes, it * 4) }
        }

        fun off(sector: Int) = (sector + 1) * sectorSize

        fun readChainFat(startSector: Int): ByteArray {
            val out = ByteArrayOutputStream()
            var s = startSector; var guard = 0
            while (s >= 0 && guard++ < fat.size + 16) {
                val base = off(s)
                if (base < 0 || base >= data.size) break
                out.write(data, base, minOf(sectorSize, data.size - base))
                s = if (s < fat.size) fat[s] else -1
            }
            return out.toByteArray()
        }

        fun readStream(name: String): ByteArray? {
            val e = dir.firstOrNull { it.name == name && it.type == 2 } ?: return null
            if (e.size >= miniCutoff) return readChainFat(e.start).copyOf(minOf(e.size, readChainFat(e.start).size))
            val out = ByteArrayOutputStream()
            var s = e.start; var guard = 0
            while (s >= 0 && guard++ < miniFat.size + 16) {
                val base = s * 64
                if (base < 0 || base >= miniStream.size) break
                out.write(miniStream, base, minOf(64, miniStream.size - base))
                s = if (s < miniFat.size) miniFat[s] else -1
            }
            val bytes = out.toByteArray()
            return bytes.copyOf(minOf(e.size.coerceAtLeast(0), bytes.size))
        }
    }

    private data class DirEntry(val name: String, val type: Int, val start: Int, val size: Int)

    private fun u16(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun i32(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)
}
