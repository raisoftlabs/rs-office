package com.raisoftlabs.rsoffice.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.util.zip.ZipFile

/**
 * Reader and PDF exporter for .xlsx spreadsheets.
 *
 * An xlsx is a zip of XML parts, so no library is needed: shared strings are
 * read once, then every sheet's cells are mapped by their A1 reference so
 * columns stay aligned even when the file skips empty cells.
 */
object XlsxEngine {

    data class Sheet(val name: String, val rows: List<List<String>>)

    private const val MAX_ROWS = 300
    private const val MAX_COLS = 24

    fun read(file: File): List<Sheet> {
        val sheets = ArrayList<Sheet>()
        try {
            ZipFile(file).use { zip ->
                val shared = sharedStrings(zip)
                val rels = workbookRels(zip)
                sheetEntries(zip, rels).take(8).forEach { (name, path) ->
                    val entry = zip.getEntry(path) ?: return@forEach
                    if (entry.size > 12_000_000L) return@forEach
                    val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                    val rows = parseSheet(xml, shared)
                    if (rows.isNotEmpty()) sheets.add(Sheet(name, rows))
                }
            }
        } catch (_: Exception) {
        }
        return sheets
    }

    fun toText(sheets: List<Sheet>): String = sheets.joinToString("\n\n") { sheet ->
        buildString {
            append(sheet.name.ifBlank { "Sheet" }).append('\n')
            sheet.rows.forEach { row ->
                append(row.joinToString("\t").trimEnd()).append('\n')
            }
        }
    }

    /* -------------------------------- parsing ------------------------------- */

    private fun sharedStrings(zip: ZipFile): List<String> {
        val entry = zip.getEntry("xl/sharedStrings.xml") ?: return emptyList()
        val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
        return Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL).findAll(xml).map { m ->
            Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                .findAll(m.groupValues[1])
                .joinToString("") { XmlText.unescape(it.groupValues[1]) }
        }.toList()
    }

    private fun workbookRels(zip: ZipFile): Map<String, String> {
        val entry = zip.getEntry("xl/_rels/workbook.xml.rels") ?: return emptyMap()
        val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
        val map = HashMap<String, String>()
        Regex("<Relationship([^>]*)/>").findAll(xml).forEach { m ->
            val attrs = m.groupValues[1]
            val id = Regex("Id=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1) ?: return@forEach
            val target = Regex("Target=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1) ?: return@forEach
            map[id] = if (target.startsWith("/")) {
                target.trimStart('/')
            } else {
                "xl/" + target.removePrefix("./")
            }
        }
        return map
    }

    private fun sheetEntries(zip: ZipFile, rels: Map<String, String>): List<Pair<String, String>> {
        val entry = zip.getEntry("xl/workbook.xml") ?: return emptyList()
        val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
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

    private fun parseSheet(xml: String, shared: List<String>): List<List<String>> {
        val rows = ArrayList<List<String>>()
        Regex("<row([^>]*)>(.*?)</row>", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { rowMatch ->
            if (rows.size >= MAX_ROWS) return@forEach
            val cells = HashMap<Int, String>()
            var maxCol = -1
            Regex("<c([^>]*)>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
                .findAll(rowMatch.groupValues[2])
                .forEach { c ->
                    val attrs = c.groupValues[1]
                    val body = c.groupValues[2]
                    val ref = Regex("r=\"([A-Z]+)\\d+\"").find(attrs)?.groupValues?.get(1) ?: ""
                    val col = colIndex(ref)
                    if (col >= MAX_COLS) return@forEach
                    val type = Regex("t=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1) ?: "n"
                    val raw = if (body.contains("<is>")) {
                        Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                            .findAll(body)
                            .joinToString("") { XmlText.unescape(it.groupValues[1]) }
                    } else {
                        Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL)
                            .find(body)?.groupValues?.get(1)?.let { XmlText.unescape(it) } ?: ""
                    }
                    val value = when (type) {
                        "s" -> raw.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                        "b" -> if (raw == "1") "TRUE" else "FALSE"
                        else -> raw
                    }
                    if (value.isBlank()) return@forEach
                    cells[col] = tidy(value)
                    if (col > maxCol) maxCol = col
                }
            if (maxCol >= 0) {
                val row = ArrayList<String>(maxCol + 1)
                for (i in 0..maxCol) row.add(cells[i] ?: "")
                rows.add(row)
            }
        }
        return rows
    }

    private fun colIndex(letters: String): Int {
        var n = 0
        for (ch in letters) {
            if (ch in 'A'..'Z') n = n * 26 + (ch - 'A' + 1) else break
        }
        return (n - 1).coerceAtLeast(0)
    }

    private fun tidy(value: String): String {
        val v = value.trim()
        if (v.length == 1 || !v.contains('.')) return v.take(90)
        val trimmed = v.trimEnd('0').trimEnd('.')
        return if (trimmed.isNotEmpty() && trimmed.matches(Regex("-?\\d+"))) trimmed else v.take(90)
    }

    /* ------------------------------ pdf export ------------------------------ */

    fun toPdf(sheets: List<Sheet>, out: File) {
        val doc = PdfDocument()
        val pageW = 842
        val pageH = 595
        val margin = 24f
        val body = Paint().apply {
            textSize = 9f
            color = Color.parseColor("#1C1C1E")
            isAntiAlias = true
        }
        val bold = Paint().apply {
            textSize = 9.5f
            color = Color.parseColor("#0B3A66")
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = Paint().apply {
            textSize = 13f
            color = Color.parseColor("#0A84FF")
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bandPaint = Paint().apply { color = Color.parseColor("#EDF3FF") }
        val linePaint = Paint().apply {
            color = Color.parseColor("#D5DDEA")
            strokeWidth = 0.7f
        }

        sheets.forEach { sheet ->
            if (sheet.rows.isEmpty()) return@forEach
            val cols = sheet.rows.maxOf { it.size }.coerceAtMost(MAX_COLS)
            if (cols == 0) return@forEach

            val weights = FloatArray(cols)
            for (c in 0 until cols) {
                var maxChars = 4
                sheet.rows.take(60).forEach { row ->
                    maxChars = maxOf(maxChars, (row.getOrNull(c) ?: "").length.coerceAtMost(26))
                }
                weights[c] = maxChars.toFloat()
            }
            val available = pageW - margin * 2
            val totalWeight = weights.sum().coerceAtLeast(1f)
            val widths = FloatArray(cols) { available * weights[it] / totalWeight }

            var page = doc.startPage(
                PdfDocument.PageInfo.Builder(pageW, pageH, doc.pages.size + 1).create()
            )
            var y = margin + 20f
            page.canvas.drawText(sheet.name.ifBlank { "Sheet" }, margin, y, titlePaint)
            y += 15f

            fun drawHeader(canvas: Canvas, startY: Float) {
                canvas.drawRect(margin, startY - 11f, margin + available, startY + 7f, bandPaint)
                var hx = margin + 4f
                for (c in 0 until cols) {
                    canvas.drawText(
                        fit(sheet.rows.first().getOrNull(c) ?: "", bold, widths[c] - 8f),
                        hx,
                        startY,
                        bold
                    )
                    hx += widths[c]
                }
            }

            drawHeader(page.canvas, y)
            y += 12f
            page.canvas.drawLine(margin, y, pageW - margin, y, linePaint)
            y += 12f

            for (rowIndex in 1 until sheet.rows.size) {
                val row = sheet.rows[rowIndex]
                val cellLines = (0 until cols).map { c ->
                    wrap(row.getOrNull(c) ?: "", body, widths[c] - 8f)
                }
                val height = cellLines.maxOf { it.size } * 11f + 6f

                if (y + height > pageH - margin - 8f) {
                    doc.finishPage(page)
                    page = doc.startPage(
                        PdfDocument.PageInfo.Builder(pageW, pageH, doc.pages.size + 1).create()
                    )
                    y = margin + 16f
                    drawHeader(page.canvas, y)
                    y += 12f
                    page.canvas.drawLine(margin, y, pageW - margin, y, linePaint)
                    y += 12f
                }

                var x = margin + 4f
                cellLines.forEachIndexed { c, lines ->
                    lines.forEachIndexed { i, text -> page.canvas.drawText(text, x, y + i * 11f, body) }
                    x += widths[c]
                }
                y += height
            }
            doc.finishPage(page)
        }

        if (doc.pages.size == 0) {
            val blank = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            blank.canvas.drawText("This spreadsheet has no readable cells.", 40f, 60f, body)
            doc.finishPage(blank)
        }
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (text.isEmpty() || paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val lines = ArrayList<String>()
        var current = StringBuilder()
        for (word in text.split(' ')) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                var rest = word
                while (paint.measureText(rest) > maxWidth && rest.length > 1) {
                    var cut = rest.length
                    while (cut > 1 && paint.measureText(rest.substring(0, cut)) > maxWidth) cut--
                    lines.add(rest.substring(0, cut))
                    rest = rest.substring(cut)
                }
                current = StringBuilder(rest)
            }
            if (lines.size > 27) break
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return if (lines.isEmpty()) listOf("") else lines
    }
}
