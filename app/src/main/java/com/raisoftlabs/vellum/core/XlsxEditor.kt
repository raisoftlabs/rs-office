package com.raisoftlabs.vellum.core

import java.io.File

/**
 * Read/write editor for .xlsx spreadsheets.
 *
 * Cells are patched inside their own `<c>` element, so a cell the user never
 * touched keeps its style index, number format and cached value. New cells and
 * rows are inserted in reference order (Excel is strict about that); formulas
 * are written as live `<f>` elements and the workbook is flagged for a full
 * recalculation on load.
 */
class XlsxEditor private constructor(
    private val parts: LinkedHashMap<String, ByteArray>,
    private val paths: List<String>,
    private val original: List<List<List<String>>>,
    val sheets: List<Sheet>
) {

    class Sheet(
        val name: String,
        val rows: List<List<String>>
    )

    fun withSheets(list: List<Sheet>): XlsxEditor =
        XlsxEditor(parts, paths, original, list)

    fun save(out: File): Boolean = runCatching {
        sheets.forEachIndexed { index, sheet ->
            val path = paths.getOrNull(index) ?: return@forEachIndexed
            val xml = Ooxml.text(parts, path) ?: return@forEachIndexed
            val patched = patchSheet(xml, sheet, original.getOrNull(index) ?: emptyList())
            Ooxml.putText(parts, path, patched)
        }
        forceRecalc()
        Ooxml.write(parts, out)
        true
    }.getOrDefault(false)

    private fun forceRecalc() {
        val xml = Ooxml.text(parts, "xl/workbook.xml") ?: return
        if (xml.contains("<calcPr")) return
        val idx = xml.lastIndexOf("</workbook>")
        if (idx < 0) return
        val patched = xml.substring(0, idx) + "<calcPr calcId=\"191029\" fullCalcOnLoad=\"1\"/>" + xml.substring(idx)
        Ooxml.putText(parts, "xl/workbook.xml", patched)
    }

    /* -------------------------------- patching ------------------------------ */

    private fun patchSheet(xml: String, sheet: Sheet, before: List<List<String>>): String {
        val rowRanges = Ooxml.ranges(xml, "row")
        if (rowRanges.isEmpty()) return xml

        val modelRows = sheet.rows
        val builder = StringBuilder()
        var cursor = 0
        var maxRow = 1
        var maxCol = 0

        rowRanges.forEachIndexed { rowIndex, range ->
            val rowXml = xml.substring(range.first, range.last + 1)
            val ref = Regex("<row[^>]*\\br=\"(\\d+)\"").find(rowXml)?.groupValues?.get(1)?.toIntOrNull()
                ?: (rowIndex + 1)
            maxRow = maxOf(maxRow, ref)

            builder.append(xml, cursor, range.first)
            builder.append(patchRow(rowXml, ref, modelRows.getOrNull(ref - 1), before.getOrNull(ref - 1)))
            cursor = range.last + 1
        }
        builder.append(xml, cursor, xml.length)

        // Rows that do not exist in the original file yet.
        val existing = rowRanges.mapIndexed { i, r ->
            Regex("<row[^>]*\\br=\"(\\d+)\"")
                .find(xml.substring(r.first, r.last + 1))?.groupValues?.get(1)?.toIntOrNull() ?: (i + 1)
        }.toSet()
        val extra = StringBuilder()
        modelRows.forEachIndexed { rowPos, cells ->
            val rowNo = rowPos + 1
            if (rowNo in existing) return@forEachIndexed
            if (cells.all { it.isBlank() }) return@forEachIndexed
            maxRow = maxOf(maxRow, rowNo)
            extra.append("<row r=\"$rowNo\">")
            cells.forEachIndexed { col, text ->
                if (text.isNotBlank()) {
                    maxCol = maxOf(maxCol, col + 1)
                    extra.append(XlsxXml.cell(XlsxXml.ref(col, rowNo), text, ""))
                }
            }
            extra.append("</row>")
        }
        modelRows.forEach { row -> row.forEachIndexed { col, text -> if (text.isNotBlank()) maxCol = maxOf(maxCol, col + 1) } }

        var out = builder.toString()
        if (extra.isNotEmpty()) {
            val insertAt = out.lastIndexOf("</sheetData>")
            if (insertAt > 0) {
                out = out.substring(0, insertAt) + extra + out.substring(insertAt)
            }
        }
        if (maxCol > 0) {
            val dim = "A1:${XlsxXml.letters(maxCol)}${maxOf(maxRow, 1)}"
            out = Regex("<dimension ref=\"[^\"]*\"/>").replaceFirst(out, "<dimension ref=\"$dim\"/>")
        }
        return out
    }

    private fun patchRow(rowXml: String, rowNo: Int, cells: List<String>?, before: List<String>?): String {
        if (cells == null) return rowXml
        val openEnd = rowXml.indexOf('>')
        if (openEnd < 0) return rowXml
        if (rowXml[openEnd - 1] == '/') {
            // <row .../> — expand into an element that can hold cells.
            val open = rowXml.substring(0, openEnd - 1) + ">"
            val body = cells.mapIndexedNotNull { col, text ->
                if (text.isBlank()) null else XlsxXml.cell(XlsxXml.ref(col, rowNo), text, "")
            }.joinToString("")
            return open + body + "</row>"
        }
        val open = rowXml.substring(0, openEnd + 1)
        val close = rowXml.lastIndexOf("</row>")
        if (close < openEnd) return rowXml
        val inner = rowXml.substring(openEnd + 1, close)

        data class Item(val col: Int, val xml: String)
        val items = ArrayList<Item>()
        Ooxml.ranges(inner, "c").forEach { range ->
            val cellXml = inner.substring(range.first, range.last + 1)
            val ref = Regex("<c[^>]*\\br=\"([A-Z]+)\\d+\"").find(cellXml)?.groupValues?.get(1) ?: return@forEach
            val col = XlsxXml.colIndex(ref)
            val text = cells.getOrNull(col)
            if (text == null) {
                items.add(Item(col, cellXml))
                return@forEach
            }
            val was = before?.getOrNull(col) ?: ""
            if (text == was) {
                items.add(Item(col, cellXml))
            } else if (text.isBlank()) {
                // cleared cell: drop it
            } else {
                val style = Regex("<c[^>]*\\bs=\"([^\"]*)\"").find(cellXml)?.groupValues?.get(1) ?: ""
                items.add(Item(col, XlsxXml.cell(XlsxXml.ref(col, rowNo), text, style)))
            }
        }
        cells.forEachIndexed { col, text ->
            if (text.isBlank()) return@forEachIndexed
            if (items.any { it.col == col }) return@forEachIndexed
            items.add(Item(col, XlsxXml.cell(XlsxXml.ref(col, rowNo), text, "")))
        }
        val body = items.sortedBy { it.col }.joinToString("") { it.xml }
        return open + body + "</row>"
    }

    companion object {

        fun load(file: File): XlsxEditor? = runCatching {
            val parts = Ooxml.read(file)
            val shared = sharedStrings(parts)
            val rels = workbookRels(parts)
            val entries = sheetEntries(parts, rels)
            if (entries.isEmpty()) return null
            val sheets = ArrayList<Sheet>()
            val paths = ArrayList<String>()
            val originals = ArrayList<List<List<String>>>()
            entries.take(6).forEach { (name, path) ->
                val xml = Ooxml.text(parts, path) ?: return@forEach
                val rows = parseRows(xml, shared)
                sheets.add(Sheet(name, rows))
                paths.add(path)
                originals.add(rows)
            }
            XlsxEditor(parts, paths, originals, sheets)
        }.getOrNull()

        private fun sharedStrings(parts: Map<String, ByteArray>): List<String> {
            val xml = Ooxml.text(parts, "xl/sharedStrings.xml") ?: return emptyList()
            return Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL).findAll(xml).map { m ->
                Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(m.groupValues[1])
                    .joinToString("") { XmlText.unescape(it.groupValues[1]) }
            }.toList()
        }

        private fun workbookRels(parts: Map<String, ByteArray>): Map<String, String> {
            val xml = Ooxml.text(parts, "xl/_rels/workbook.xml.rels") ?: return emptyMap()
            val map = HashMap<String, String>()
            Regex("<Relationship([^>]*)/>").findAll(xml).forEach { m ->
                val attrs = m.groupValues[1]
                val id = Regex("Id=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1) ?: return@forEach
                val target = Regex("Target=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1) ?: return@forEach
                map[id] = if (target.startsWith("/")) target.trimStart('/') else "xl/" + target.removePrefix("./")
            }
            return map
        }

        private fun sheetEntries(
            parts: Map<String, ByteArray>,
            rels: Map<String, String>
        ): List<Pair<String, String>> {
            val xml = Ooxml.text(parts, "xl/workbook.xml") ?: return emptyList()
            val list = ArrayList<Pair<String, String>>()
            Regex("<sheet([^>]*)/>").findAll(xml).forEach { m ->
                val attrs = m.groupValues[1]
                val name = Regex("name=\"([^\"]*)\"").find(attrs)?.groupValues?.get(1) ?: return@forEach
                val rid = Regex("r:id=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1) ?: return@forEach
                val path = rels[rid] ?: return@forEach
                list.add(XmlText.unescape(name) to path)
            }
            return list
        }

        private const val MAX_ROWS = 400
        private const val MAX_COLS = 30

        private fun parseRows(xml: String, shared: List<String>): List<List<String>> {
            val rows = ArrayList<List<String>>()
            var index = 0
            Ooxml.ranges(xml, "row").forEach { range ->
                if (index >= MAX_ROWS) return@forEach
                index++
                val rowXml = xml.substring(range.first, range.last + 1)
                val cells = HashMap<Int, String>()
                var maxCol = -1
                Ooxml.ranges(rowXml, "c").forEach { cr ->
                    val cellXml = rowXml.substring(cr.first, cr.last + 1)
                    val ref = Regex("<c[^>]*\\br=\"([A-Z]+)\\d+\"").find(cellXml)?.groupValues?.get(1) ?: return@forEach
                    val col = XlsxXml.colIndex(ref)
                    if (col >= MAX_COLS) return@forEach
                    val type = Regex("<c[^>]*\\bt=\"([^\"]+)\"").find(cellXml)?.groupValues?.get(1) ?: "n"
                    val formula = Regex("<f[^>]*>(.*?)</f>", RegexOption.DOT_MATCHES_ALL)
                        .find(cellXml)?.groupValues?.get(1)?.let { XmlText.unescape(it) }
                    val raw = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL)
                        .find(cellXml)?.groupValues?.get(1)?.let { XmlText.unescape(it) }
                    val inline = Regex("<is>.*?<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                        .find(cellXml)?.groupValues?.get(1)?.let { XmlText.unescape(it) }
                    val text = when {
                        formula != null -> "=$formula"
                        type == "s" -> raw?.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                        type == "inlineStr" -> inline ?: ""
                        type == "b" -> if (raw == "1") "TRUE" else "FALSE"
                        else -> raw ?: ""
                    }
                    cells[col] = tidy(text)
                    if (col > maxCol) maxCol = col
                }
                val row = ArrayList<String>(maxCol + 1)
                for (i in 0..maxCol) row.add(cells[i] ?: "")
                rows.add(row)
            }
            return rows
        }

        private fun tidy(value: String): String {
            val v = value.trim()
            if (v.length == 1 || !v.contains('.')) return v
            val trimmed = v.trimEnd('0').trimEnd('.')
            return if (trimmed.isNotEmpty() && trimmed.matches(Regex("-?\\d+"))) trimmed else v
        }
    }
}

/** Cell/reference helpers for the xlsx writer. */
internal object XlsxXml {

    fun cell(ref: String, text: String, style: String): String {
        val s = if (style.isBlank()) "" else " s=\"$style\""
        val raw = text.trim()
        return when {
            raw.startsWith("=") ->
                "<c r=\"$ref\"$s><f>${Ooxml.escape(raw.substring(1))}</f></c>"
            raw.equals("TRUE", true) || raw.equals("FALSE", true) ->
                "<c r=\"$ref\"$s t=\"b\"><v>${if (raw.equals("TRUE", true)) 1 else 0}</v></c>"
            raw.toDoubleOrNull() != null -> "<c r=\"$ref\"$s><v>${raw.replace(",", "")}</v></c>"
            else -> "<c r=\"$ref\"$s t=\"inlineStr\"><is><t xml:space=\"preserve\">${Ooxml.escape(raw)}</t></is></c>"
        }
    }

    fun ref(col: Int, row: Int): String = "${letters(col + 1)}$row"

    fun letters(n: Int): String {
        var value = n.coerceAtLeast(1)
        val sb = StringBuilder()
        while (value > 0) {
            val rem = (value - 1) % 26
            sb.insert(0, ('A' + rem))
            value = (value - 1) / 26
        }
        return sb.toString()
    }

    fun colIndex(letters: String): Int {
        var n = 0
        for (ch in letters) {
            if (ch in 'A'..'Z') n = n * 26 + (ch - 'A' + 1) else break
        }
        return (n - 1).coerceAtLeast(0)
    }
}
