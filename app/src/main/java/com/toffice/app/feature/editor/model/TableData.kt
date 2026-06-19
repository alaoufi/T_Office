package com.toffice.app.feature.editor.model

import org.json.JSONArray
import org.json.JSONObject

/** بيانات جدول بسيط: صفوف من الخلايا النصّية (كل صف بنفس عدد الأعمدة). */
data class TableData(val rows: List<List<String>>) {
    val rowCount: Int get() = rows.size
    val colCount: Int get() = rows.firstOrNull()?.size ?: 0
}

/** عمليات نقيّة على الجدول (قابلة للاختبار). */
object TableOps {

    fun newTable(rowCount: Int, colCount: Int): TableData {
        val r = rowCount.coerceIn(1, 50)
        val c = colCount.coerceIn(1, 20)
        return TableData(List(r) { List(c) { "" } })
    }

    fun setCell(t: TableData, row: Int, col: Int, text: String): TableData {
        if (row !in t.rows.indices || col < 0 || col >= t.colCount) return t
        val rows = t.rows.mapIndexed { ri, r ->
            if (ri == row) r.mapIndexed { ci, cell -> if (ci == col) text else cell } else r
        }
        return TableData(rows)
    }

    fun addRow(t: TableData): TableData =
        TableData(t.rows + listOf(List(t.colCount.coerceAtLeast(1)) { "" }))

    fun addColumn(t: TableData): TableData =
        TableData(t.rows.map { it + "" })

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
                for (cell in row) rowArr.put(cell)
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
            val rows = mutableListOf<List<String>>()
            for (r in 0 until rowsArr.length()) {
                val rowArr = rowsArr.getJSONArray(r)
                val cells = mutableListOf<String>()
                for (c in 0 until rowArr.length()) cells.add(rowArr.optString(c, ""))
                rows.add(cells)
            }
            if (rows.isNotEmpty()) result.add(TableData(rows))
        }
        return result
    }
}
