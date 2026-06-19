package com.toffice.app.feature.editor.model

import org.json.JSONArray
import org.json.JSONObject

/** خلية جدول: نص + محاذاة (0=حسب الاتجاه، 1=توسيط، 2=يسار، 3=يمين). */
data class TableCell(val text: String = "", val align: Int = 0)

/** بيانات جدول بسيط: صفوف من الخلايا (كل صف بنفس عدد الأعمدة). */
data class TableData(val rows: List<List<TableCell>>) {
    val rowCount: Int get() = rows.size
    val colCount: Int get() = rows.firstOrNull()?.size ?: 0
}

/** عمليات نقيّة على الجدول (قابلة للاختبار). */
object TableOps {

    fun newTable(rowCount: Int, colCount: Int): TableData {
        val r = rowCount.coerceIn(1, 50)
        val c = colCount.coerceIn(1, 20)
        return TableData(List(r) { List(c) { TableCell() } })
    }

    private fun map(t: TableData, row: Int, col: Int, f: (TableCell) -> TableCell): TableData {
        if (row !in t.rows.indices || col < 0 || col >= t.colCount) return t
        return TableData(
            t.rows.mapIndexed { ri, r ->
                if (ri == row) r.mapIndexed { ci, cell -> if (ci == col) f(cell) else cell } else r
            },
        )
    }

    fun setCell(t: TableData, row: Int, col: Int, text: String): TableData =
        map(t, row, col) { it.copy(text = text) }

    fun setCellAlign(t: TableData, row: Int, col: Int, align: Int): TableData =
        map(t, row, col) { it.copy(align = align) }

    fun addRow(t: TableData): TableData =
        TableData(t.rows + listOf(List(t.colCount.coerceAtLeast(1)) { TableCell() }))

    fun addColumn(t: TableData): TableData =
        TableData(t.rows.map { it + TableCell() })

    fun deleteRow(t: TableData, row: Int): TableData {
        if (t.rowCount <= 1 || row !in t.rows.indices) return t
        return TableData(t.rows.filterIndexed { i, _ -> i != row })
    }

    fun deleteColumn(t: TableData, col: Int): TableData {
        if (t.colCount <= 1 || col < 0 || col >= t.colCount) return t
        return TableData(t.rows.map { r -> r.filterIndexed { i, _ -> i != col } })
    }

    fun toJson(tables: List<TableData>): JSONArray {
        val arr = JSONArray()
        for (t in tables) {
            val rowsArr = JSONArray()
            for (row in t.rows) {
                val rowArr = JSONArray()
                for (cell in row) {
                    rowArr.put(JSONObject().put("t", cell.text).put("a", cell.align))
                }
                rowsArr.put(rowArr)
            }
            arr.put(JSONObject().put("rows", rowsArr))
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
                        cells.add(TableCell(cellObj.optString("t", ""), cellObj.optInt("a", 0)))
                    } else {
                        cells.add(TableCell(rowArr.optString(c, "")))
                    }
                }
                rows.add(cells)
            }
            if (rows.isNotEmpty()) result.add(TableData(rows))
        }
        return result
    }
}
