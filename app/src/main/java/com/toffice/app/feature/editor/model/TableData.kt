package com.toffice.app.feature.editor.model

import org.json.JSONArray
import org.json.JSONObject

/** خلية جدول: نص + محاذاة + امتداد أفقي + تنسيق (غامق/لون/حجم). */
data class TableCell(
    val text: String = "",
    val align: Int = 0,
    val span: Int = 1,
    val bold: Boolean = false,
    val color: Long = 0L, // 0 = اللون الافتراضي
    val size: Int = 0,    // 0 = الحجم الافتراضي (14)
)

/** بيانات جدول: صفوف من الخلايا + أوزان عرض الأعمدة (مرونة كأنها Excel). */
data class TableData(
    val rows: List<List<TableCell>>,
    val colWeights: List<Float> = emptyList(),
) {
    val rowCount: Int get() = rows.size
    val colCount: Int get() = rows.firstOrNull()?.size ?: 0
    fun weightOf(col: Int): Float = colWeights.getOrElse(col) { 1f }.coerceIn(0.4f, 3f)
}

/** عمليات نقيّة على الجدول (قابلة للاختبار). */
object TableOps {

    fun newTable(rowCount: Int, colCount: Int): TableData {
        val r = rowCount.coerceIn(1, 50)
        val c = colCount.coerceIn(1, 20)
        return TableData(List(r) { List(c) { TableCell() } }, List(c) { 1f })
    }

    fun setColWeight(t: TableData, col: Int, weight: Float): TableData {
        if (col < 0 || col >= t.colCount) return t
        val w = MutableList(t.colCount) { t.weightOf(it) }
        w[col] = weight.coerceIn(0.4f, 3f)
        return t.copy(colWeights = w)
    }

    fun widenColumn(t: TableData, col: Int, delta: Float): TableData =
        setColWeight(t, col, t.weightOf(col) + delta)

    private fun map(t: TableData, row: Int, col: Int, f: (TableCell) -> TableCell): TableData {
        if (row !in t.rows.indices || col < 0 || col >= t.colCount) return t
        return t.copy(
            rows = t.rows.mapIndexed { ri, r ->
                if (ri == row) r.mapIndexed { ci, cell -> if (ci == col) f(cell) else cell } else r
            },
        )
    }

    fun setCell(t: TableData, row: Int, col: Int, text: String): TableData =
        map(t, row, col) { it.copy(text = text) }

    fun setCellAlign(t: TableData, row: Int, col: Int, align: Int): TableData =
        map(t, row, col) { it.copy(align = align) }

    fun setCellBold(t: TableData, row: Int, col: Int, bold: Boolean): TableData =
        map(t, row, col) { it.copy(bold = bold) }

    fun setCellColor(t: TableData, row: Int, col: Int, color: Long): TableData =
        map(t, row, col) { it.copy(color = color) }

    fun setCellSize(t: TableData, row: Int, col: Int, size: Int): TableData =
        map(t, row, col) { it.copy(size = size.coerceIn(8, 72)) }

    /** دمج الخلية مع العمود التالي (امتداد أفقي). */
    fun mergeRight(t: TableData, row: Int, col: Int): TableData {
        if (row !in t.rows.indices || col < 0) return t
        val cell = t.rows[row].getOrNull(col) ?: return t
        val newSpan = cell.span + 1
        if (col + newSpan > t.colCount) return t
        return map(t, row, col) { it.copy(span = newSpan) }
    }

    /** فصل الخلية المدموجة (إرجاع الامتداد إلى ١). */
    fun splitCell(t: TableData, row: Int, col: Int): TableData =
        map(t, row, col) { it.copy(span = 1) }

    fun addRow(t: TableData): TableData =
        t.copy(rows = t.rows + listOf(List(t.colCount.coerceAtLeast(1)) { TableCell() }))

    fun addColumn(t: TableData): TableData =
        TableData(t.rows.map { it + TableCell() }, MutableList(t.colCount) { t.weightOf(it) } + 1f)

    fun deleteRow(t: TableData, row: Int): TableData {
        if (t.rowCount <= 1 || row !in t.rows.indices) return t
        return t.copy(rows = t.rows.filterIndexed { i, _ -> i != row })
    }

    fun deleteColumn(t: TableData, col: Int): TableData {
        if (t.colCount <= 1 || col < 0 || col >= t.colCount) return t
        val weights = MutableList(t.colCount) { t.weightOf(it) }.filterIndexed { i, _ -> i != col }
        return TableData(t.rows.map { r -> r.filterIndexed { i, _ -> i != col } }, weights)
    }

    fun toJson(tables: List<TableData>): JSONArray {
        val arr = JSONArray()
        for (t in tables) {
            val rowsArr = JSONArray()
            for (row in t.rows) {
                val rowArr = JSONArray()
                for (cell in row) {
                    rowArr.put(
                        JSONObject().put("t", cell.text).put("a", cell.align).put("s", cell.span)
                            .put("b", cell.bold).put("c", cell.color).put("z", cell.size),
                    )
                }
                rowsArr.put(rowArr)
            }
            val wArr = JSONArray()
            for (i in 0 until t.colCount) wArr.put(t.weightOf(i).toDouble())
            arr.put(JSONObject().put("rows", rowsArr).put("w", wArr))
        }
        return arr
    }

    fun fromJson(arr: JSONArray?): List<TableData> {
        if (arr == null) return emptyList()
        val result = mutableListOf<TableData>()
        for (i in 0 until arr.length()) {
            val rowsArr = arr.getJSONObject(i).optJSONArray("rows") ?: JSONArray()
            val rows = mutableListOf<List<TableCell>>()
            for (r in 0 until rowsArr.length()) {
                val rowArr = rowsArr.getJSONArray(r)
                val cells = mutableListOf<TableCell>()
                for (c in 0 until rowArr.length()) {
                    val cellObj = rowArr.optJSONObject(c)
                    if (cellObj != null) {
                        cells.add(
                            TableCell(
                                cellObj.optString("t", ""), cellObj.optInt("a", 0), cellObj.optInt("s", 1),
                                cellObj.optBoolean("b", false), cellObj.optLong("c", 0L), cellObj.optInt("z", 0),
                            ),
                        )
                    } else {
                        cells.add(TableCell(rowArr.optString(c, "")))
                    }
                }
                rows.add(cells)
            }
            val wArr = arr.getJSONObject(i).optJSONArray("w")
            val weights = if (wArr != null) (0 until wArr.length()).map { wArr.optDouble(it, 1.0).toFloat() } else emptyList()
            if (rows.isNotEmpty()) result.add(TableData(rows, weights))
        }
        return result
    }
}
