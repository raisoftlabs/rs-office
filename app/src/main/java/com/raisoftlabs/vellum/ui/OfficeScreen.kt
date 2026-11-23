package com.raisoftlabs.vellum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raisoftlabs.vellum.core.DocxEditor
import com.raisoftlabs.vellum.core.PptxEditor
import com.raisoftlabs.vellum.core.Store
import com.raisoftlabs.vellum.core.XlsxEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class OfficeKind(val label: String, val ext: String) {
    WORD("Word", "docx"),
    SHEET("Excel", "xlsx"),
    SLIDES("PowerPoint", "pptx"),
    OTHER("File", "bin")
}

private fun kindOf(file: File): OfficeKind = when (file.extension.lowercase()) {
    "docx" -> OfficeKind.WORD
    "xlsx" -> OfficeKind.SHEET
    "pptx" -> OfficeKind.SLIDES
    else -> OfficeKind.OTHER
}

/** Ink palette offered by the format bar. */
private val INKS = listOf("", "C62828", "1A73E8", "2E7D32", "F9A825", "6A1B9A")

/**
 * In-app Office editor: opens .docx / .xlsx / .pptx, lets the user change text
 * and basic formatting, then writes the file back with every untouched part of
 * the package preserved.
 */
@Composable
fun OfficeScreen(file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kind = remember(file.path) { kindOf(file) }

    var loading by remember(file.path) { mutableStateOf(true) }
    var error by remember(file.path) { mutableStateOf<String?>(null) }
    var saving by remember(file.path) { mutableStateOf(false) }
    var savedFile by remember(file.path) { mutableStateOf<File?>(null) }
    var dirty by remember(file.path) { mutableStateOf(false) }

    var docx by remember(file.path) { mutableStateOf<DocxEditor?>(null) }
    var xlsx by remember(file.path) { mutableStateOf<XlsxEditor?>(null) }
    var pptx by remember(file.path) { mutableStateOf<PptxEditor?>(null) }

    val wordHistory = remember(file.path) { mutableStateListOf<List<DocxEditor.Para>>() }
    val wordFuture = remember(file.path) { mutableStateListOf<List<DocxEditor.Para>>() }
    val sheetHistory = remember(file.path) { mutableStateListOf<Pair<Int, List<List<String>>>>() }
    val sheetFuture = remember(file.path) { mutableStateListOf<Pair<Int, List<List<String>>>>() }
    val slideHistory = remember(file.path) { mutableStateListOf<List<List<PptxEditor.Para>>>() }
    val slideFuture = remember(file.path) { mutableStateListOf<List<List<PptxEditor.Para>>>() }

    var selected by remember(file.path) { mutableIntStateOf(-1) }
    var sheetIndex by remember(file.path) { mutableIntStateOf(0) }
    var slideIndex by remember(file.path) { mutableIntStateOf(0) }

    LaunchedEffect(file.path) {
        loading = true
        error = null
        withContext(Dispatchers.IO) {
            when (kind) {
                OfficeKind.WORD -> docx = DocxEditor.load(file)
                OfficeKind.SHEET -> xlsx = XlsxEditor.load(file)
                OfficeKind.SLIDES -> pptx = PptxEditor.load(file)
                OfficeKind.OTHER -> Unit
            }
        }
        error = when (kind) {
            OfficeKind.WORD -> if (docx == null || docx!!.paragraphs.isEmpty()) "This document has no readable paragraphs."
            OfficeKind.SHEET -> if (xlsx == null || xlsx!!.sheets.isEmpty()) "This spreadsheet has no readable cells."
            OfficeKind.SLIDES -> if (pptx == null || pptx!!.slides.isEmpty()) "This presentation has no readable slides."
            OfficeKind.OTHER -> "Open a .docx, .xlsx or .pptx file to edit it."
        }
        loading = false
    }

    fun pushWord(paras: List<DocxEditor.Para>) {
        docx?.paragraphs?.let { wordHistory.add(it) }
        wordFuture.clear()
        docx = docx?.withParagraphs(paras)
        dirty = true
    }

    fun pushSheet(index: Int, rows: List<List<String>>) {
        xlsx?.sheets?.getOrNull(index)?.rows?.let { sheetHistory.add(index to it) }
        sheetFuture.clear()
        xlsx = xlsx?.let { editor ->
            editor.withSheets(
                editor.sheets.mapIndexed { i, sheet ->
                    if (i == index) XlsxEditor.Sheet(sheet.name, rows) else sheet
                }
            )
        }
        dirty = true
    }

    fun pushSlides(slides: List<PptxEditor.Slide>) {
        pptx?.slides?.map { it.paragraphs }?.let { slideHistory.add(it) }
        slideFuture.clear()
        pptx = pptx?.withSlides(slides)
        dirty = true
    }

    fun pushSheetSilently(index: Int, rows: List<List<String>>) {
        xlsx = xlsx?.let { editor ->
            editor.withSheets(
                editor.sheets.mapIndexed { i, sheet ->
                    if (i == index) XlsxEditor.Sheet(sheet.name, rows) else sheet
                }
            )
        }
    }

    fun undo() {
        when (kind) {
            OfficeKind.WORD -> {
                val prev = wordHistory.removeLastOrNull() ?: return
                docx?.paragraphs?.let { wordFuture.add(it) }
                docx = docx?.withParagraphs(prev)
                dirty = true
            }

            OfficeKind.SHEET -> {
                val prev = sheetHistory.removeLastOrNull() ?: return
                val index = prev.first
                xlsx?.sheets?.getOrNull(index)?.rows?.let { sheetFuture.add(index to it) }
                pushSheetSilently(index, prev.second)
                dirty = true
            }

            OfficeKind.SLIDES -> {
                val prev = slideHistory.removeLastOrNull() ?: return
                pptx?.slides?.map { it.paragraphs }?.let { slideFuture.add(it) }
                pptx = pptx?.withSlides(prev.mapIndexed { i, paras -> PptxEditor.Slide(i + 1, paras) })
                dirty = true
            }

            OfficeKind.OTHER -> Unit
        }
    }

    fun redo() {
        when (kind) {
            OfficeKind.WORD -> {
                val next = wordFuture.removeLastOrNull() ?: return
                docx?.paragraphs?.let { wordHistory.add(it) }
                docx = docx?.withParagraphs(next)
                dirty = true
            }

            OfficeKind.SHEET -> {
                val next = sheetFuture.removeLastOrNull() ?: return
                val index = next.first
                xlsx?.sheets?.getOrNull(index)?.rows?.let { sheetHistory.add(index to it) }
                pushSheetSilently(index, next.second)
                dirty = true
            }

            OfficeKind.SLIDES -> {
                val next = slideFuture.removeLastOrNull() ?: return
                pptx?.slides?.map { it.paragraphs }?.let { slideHistory.add(it) }
                pptx = pptx?.withSlides(next.mapIndexed { i, paras -> PptxEditor.Slide(i + 1, paras) })
                dirty = true
            }

            OfficeKind.OTHER -> Unit
        }
    }

    fun save() {
        if (saving) return
        saving = true
        scope.launch {
            val out = Store.uniqueFile(Store.root(context), "${file.nameWithoutExtension}-edited", kind.ext)
            val ok = withContext(Dispatchers.IO) {
                when (kind) {
                    OfficeKind.WORD -> docx?.save(out) ?: false
                    OfficeKind.SHEET -> xlsx?.save(out) ?: false
                    OfficeKind.SLIDES -> pptx?.save(out) ?: false
                    OfficeKind.OTHER -> false
                }
            }
            saving = false
            if (ok) {
                savedFile = out
                dirty = false
                context.toast("Saved · ${out.name}")
            } else {
                context.toast("Could not save this file")
            }
        }
    }

    val canUndo = when (kind) {
        OfficeKind.WORD -> wordHistory.isNotEmpty()
        OfficeKind.SHEET -> sheetHistory.isNotEmpty()
        OfficeKind.SLIDES -> slideHistory.isNotEmpty()
        OfficeKind.OTHER -> false
    }
    val canRedo = when (kind) {
        OfficeKind.WORD -> wordFuture.isNotEmpty()
        OfficeKind.SHEET -> sheetFuture.isNotEmpty()
        OfficeKind.SLIDES -> slideFuture.isNotEmpty()
        OfficeKind.OTHER -> false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = file.name,
            subtitle = when (kind) {
                OfficeKind.WORD -> "${docx?.paragraphs?.size ?: 0} paragraphs${if (dirty) " · edited" else ""}"
                OfficeKind.SHEET -> "${xlsx?.sheets?.getOrNull(sheetIndex)?.name ?: "Sheet"}${if (dirty) " · edited" else ""}"
                OfficeKind.SLIDES -> "${pptx?.slides?.size ?: 0} slides${if (dirty) " · edited" else ""}"
                OfficeKind.OTHER -> "Document"
            },
            onBack = onBack,
            actions = {
                if (canUndo) IconButton(onClick = { undo() }) {
                    Icon(Icons.Rounded.Undo, "Undo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (canRedo) IconButton(onClick = { redo() }) {
                    Icon(Icons.Rounded.Redo, "Redo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { save() }) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Download, "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                savedFile?.let { out ->
                    IconButton(onClick = { Store.share(context, out) }) {
                        Icon(Icons.Rounded.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            }

            error != null -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(radius = 22.dp, padding = 20.dp) {
                    Text(
                        "Cannot open this file",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Supported now: .docx, .xlsx, .pptx",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            kind == OfficeKind.WORD && docx != null -> WordEditor(
                doc = docx!!,
                selected = selected,
                onSelect = { selected = it },
                onEdit = { index, para ->
                    val list = docx!!.paragraphs.toMutableList()
                    if (index in list.indices) list[index] = para
                    pushWord(list)
                },
                onSnapshot = { docx?.paragraphs?.let { wordHistory.add(it); wordFuture.clear() } },
                onAdd = {
                    docx = docx?.let { it.withParagraphs(it.paragraphs + it.newParagraph(it.paragraphs.lastOrNull())) }
                    selected = (docx?.paragraphs?.size ?: 1) - 1
                    dirty = true
                },
                onDelete = { index ->
                    val list = docx!!.paragraphs.toMutableList()
                    if (index in list.indices) list.removeAt(index)
                    pushWord(list)
                    selected = -1
                }
            )

            kind == OfficeKind.SHEET && xlsx != null -> SheetEditor(
                editor = xlsx!!,
                sheetIndex = sheetIndex,
                onPickSheet = { sheetIndex = it },
                onEditCell = { row, col, value ->
                    val sheet = xlsx!!.sheets[sheetIndex]
                    val rows = sheet.rows.map { it.toMutableList() }.toMutableList()
                    while (rows.size <= row) rows.add(mutableListOf())
                    val line = rows[row]
                    while (line.size <= col) line.add("")
                    line[col] = value
                    pushSheet(sheetIndex, rows.map { it.toList() })
                },
                onAddRow = {
                    val sheet = xlsx!!.sheets[sheetIndex]
                    val cols = (sheet.rows.maxOfOrNull { it.size } ?: 0).coerceAtLeast(3)
                    pushSheet(sheetIndex, sheet.rows + List(1) { List(cols) { "" } })
                },
                onAddColumn = {
                    val sheet = xlsx!!.sheets[sheetIndex]
                    pushSheet(sheetIndex, sheet.rows.map { it + "" })
                }
            )

            kind == OfficeKind.SLIDES && pptx != null -> SlideEditor(
                editor = pptx!!,
                slideIndex = slideIndex,
                selected = selected,
                onPickSlide = { slideIndex = it; selected = -1 },
                onSelect = { selected = it },
                onEdit = { para ->
                    val slides = pptx!!.slides.toMutableList()
                    val slide = slides.getOrNull(slideIndex) ?: return@SlideEditor
                    slides[slideIndex] = PptxEditor.Slide(
                        slide.number,
                        slide.paragraphs.map { if (it.src == para.src) para else it }
                    )
                    pushSlides(slides)
                },
                onSnapshot = {
                    pptx?.slides?.map { it.paragraphs }?.let { slideHistory.add(it); slideFuture.clear() }
                }
            )
        }
    }
}

/* ---------------------------------- Word ---------------------------------- */

@Composable
private fun WordEditor(
    doc: DocxEditor,
    selected: Int,
    onSelect: (Int) -> Unit,
    onEdit: (Int, DocxEditor.Para) -> Unit,
    onSnapshot: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit
) {
    val para = doc.paragraphs.getOrNull(selected)
    Column(modifier = Modifier.fillMaxSize()) {
        FormatBar(
            bold = para?.bold == true,
            italic = para?.italic == true,
            underline = para?.underline == true,
            size = para?.sizePt ?: 0,
            color = para?.color ?: "",
            alignCenter = para?.align == DocxEditor.Align.CENTER,
            heading = para?.heading == true,
            enabled = para != null && !para.locked,
            onBold = { para?.let { onEdit(selected, it.copy(bold = !it.bold)) } },
            onItalic = { para?.let { onEdit(selected, it.copy(italic = !it.italic)) } },
            onUnderline = { para?.let { onEdit(selected, it.copy(underline = !it.underline)) } },
            onSize = { delta ->
                para?.let {
                    val base = if (it.sizePt > 0) it.sizePt else if (it.heading) 16 else 11
                    onEdit(selected, it.copy(sizePt = (base + delta).coerceIn(8, 72)))
                }
            },
            onColor = { ink -> para?.let { onEdit(selected, it.copy(color = ink)) } },
            onAlign = {
                para?.let {
                    val next = if (it.align == DocxEditor.Align.CENTER) DocxEditor.Align.LEFT else DocxEditor.Align.CENTER
                    onEdit(selected, it.copy(align = next))
                }
            },
            onHeading = { para?.let { onEdit(selected, it.copy(heading = !it.heading)) } },
            onAdd = onAdd,
            onDelete = { if (selected >= 0) onDelete(selected) }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
        ) {
            itemsIndexed(doc.paragraphs) { index, item ->
                WordBlock(
                    para = item,
                    selectedIndex = selected,
                    index = index,
                    onSelect = { onSelect(index) },
                    onSnapshot = onSnapshot,
                    onChange = { text -> onEdit(index, item.copy(text = text)) }
                )
            }
            item {
                Text(
                    "Tap a paragraph, then use the bar above · layout, images and tables stay untouched",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun WordBlock(
    para: DocxEditor.Para,
    selectedIndex: Int,
    index: Int,
    onSelect: () -> Unit,
    onSnapshot: () -> Unit,
    onChange: (String) -> Unit
) {
    val active = selectedIndex == index
    val size = when {
        para.sizePt > 0 -> para.sizePt.sp
        para.heading -> 17.sp
        else -> 14.sp
    }
    val style = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = size,
        fontWeight = if (para.bold || para.heading) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (para.italic) androidx.compose.ui.text.font.FontStyle.Italic
        else androidx.compose.ui.text.font.FontStyle.Normal,
        textAlign = when (para.align) {
            DocxEditor.Align.CENTER -> TextAlign.Center
            DocxEditor.Align.RIGHT -> TextAlign.End
            DocxEditor.Align.JUSTIFY -> TextAlign.Justify
            DocxEditor.Align.LEFT -> TextAlign.Start
        },
        textDecoration = if (para.underline) androidx.compose.ui.text.style.TextDecoration.Underline
        else null,
        lineHeight = (size.value * 1.45f).sp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = !para.locked) { onSelect() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (para.locked) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp).padding(top = 3.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                para.text.ifBlank { "…" },
                style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.weight(1f)
            )
        } else {
            val ink = if (para.color.isBlank()) MaterialTheme.colorScheme.onSurface else inkOf(para.color)
            BasicTextField(
                value = para.text,
                onValueChange = onChange,
                textStyle = style.copy(color = ink),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { if (it.isFocused) { onSelect(); onSnapshot() } }
            )
        }
    }
}

/* --------------------------------- Sheets --------------------------------- */

@Composable
private fun SheetEditor(
    editor: XlsxEditor,
    sheetIndex: Int,
    onPickSheet: (Int) -> Unit,
    onEditCell: (Int, Int, String) -> Unit,
    onAddRow: () -> Unit,
    onAddColumn: () -> Unit
) {
    val sheet = editor.sheets.getOrNull(sheetIndex) ?: return
    var editTarget by remember(sheetIndex) { mutableStateOf<Pair<Int, Int>?>(null) }
    val scroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${sheet.rows.size} rows · ${sheet.rows.maxOfOrNull { it.size } ?: 0} columns",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAddRow) { Text("+ Row", fontSize = 12.sp) }
            TextButton(onClick = onAddColumn) { Text("+ Column", fontSize = 12.sp) }
        }
        if (editor.sheets.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(editor.sheets.size) { i ->
                    SelectChip(text = editor.sheets[i].name, selected = i == sheetIndex, onClick = { onPickSheet(i) })
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(sheet.rows) { rowIndex, cells ->
                    Row(modifier = Modifier.horizontalScroll(scroll)) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${rowIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val width = if (cells.isEmpty()) 1 else cells.size
                        for (col in 0 until maxOf(width, 4)) {
                            val value = cells.getOrNull(col) ?: ""
                            Box(
                                modifier = Modifier
                                    .width(104.dp)
                                    .height(40.dp)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    .clickable { editTarget = rowIndex to col }
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (value.startsWith("="))
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(90.dp))
                }
            }
        }
    }

    editTarget?.let { (row, col) ->
        val current = sheet.rows.getOrNull(row)?.getOrNull(col) ?: ""
        var draft by remember(row, col) { mutableStateOf(current) }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Cell ${XlsxCellRef.of(row, col)}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        label = { Text("Value or =FORMULA") }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Numbers are stored as numbers, =SUM(A1:A9) stays a live formula.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditCell(row, col, draft)
                    editTarget = null
                }) { Text("Apply") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        onEditCell(row, col, "")
                        editTarget = null
                    }) { Text("Clear") }
                    TextButton(onClick = { editTarget = null }) { Text("Cancel") }
                }
            }
        )
    }
}

private object XlsxCellRef {
    fun of(row: Int, col: Int): String {
        var value = col + 1
        val sb = StringBuilder()
        while (value > 0) {
            val rem = (value - 1) % 26
            sb.insert(0, ('A' + rem))
            value = (value - 1) / 26
        }
        return "$sb${row + 1}"
    }
}

/* --------------------------------- Slides --------------------------------- */

@Composable
private fun SlideEditor(
    editor: PptxEditor,
    slideIndex: Int,
    selected: Int,
    onPickSlide: (Int) -> Unit,
    onSelect: (Int) -> Unit,
    onEdit: (PptxEditor.Para) -> Unit,
    onSnapshot: () -> Unit
) {
    val slide = editor.slides.getOrNull(slideIndex)
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(editor.slides.size) { i ->
                SelectChip(
                    text = "Slide ${editor.slides[i].number}",
                    selected = i == slideIndex,
                    onClick = { onPickSlide(i) }
                )
            }
        }
        val para = slide?.paragraphs?.getOrNull(selected)
        FormatBar(
            bold = para?.bold == true,
            italic = para?.italic == true,
            underline = false,
            size = para?.sizePt ?: 0,
            color = para?.color ?: "",
            alignCenter = para?.center == true,
            heading = false,
            enabled = para != null,
            onBold = { para?.let { onEdit(it.copy(bold = !it.bold)) } },
            onItalic = { para?.let { onEdit(it.copy(italic = !it.italic)) } },
            onUnderline = {},
            onSize = { delta ->
                para?.let {
                    val base = if (it.sizePt > 0) it.sizePt else 18
                    onEdit(it.copy(sizePt = (base + delta).coerceIn(8, 96)))
                }
            },
            onColor = { ink -> para?.let { onEdit(it.copy(color = ink)) } },
            onAlign = { para?.let { onEdit(it.copy(center = !it.center)) } },
            onHeading = {},
            onAdd = null,
            onDelete = null
        )
        if (slide == null) return@Column
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(slide.paragraphs) { index, item ->
                GlassCard(radius = 18.dp, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(PptOrange, RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        val style = TextStyle(
                            color = if (item.color.isBlank()) MaterialTheme.colorScheme.onSurface else inkOf(item.color),
                            fontSize = (if (item.sizePt > 0) item.sizePt else 18).sp,
                            fontWeight = if (item.bold) FontWeight.Bold else FontWeight.Medium,
                            fontStyle = if (item.italic) androidx.compose.ui.text.font.FontStyle.Italic
                            else androidx.compose.ui.text.font.FontStyle.Normal,
                            textAlign = if (item.center) TextAlign.Center else TextAlign.Start
                        )
                        BasicTextField(
                            value = item.text,
                            onValueChange = { onEdit(item.copy(text = it)) },
                            textStyle = style,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { if (it.isFocused) { onSelect(index); onSnapshot() } }
                        )
                    }
                }
            }
            item {
                Text(
                    "Images and layout stay exactly as they were · text only is rewritten",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 90.dp)
                )
            }
        }
    }
}

/* -------------------------------- format bar ------------------------------- */

@Composable
private fun FormatBar(
    bold: Boolean,
    italic: Boolean,
    underline: Boolean,
    size: Int,
    color: String,
    alignCenter: Boolean,
    heading: Boolean,
    enabled: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onSize: (Int) -> Unit,
    onColor: (String) -> Unit,
    onAlign: () -> Unit,
    onHeading: () -> Unit,
    onAdd: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    var showInk by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToneChip(active = bold, enabled = enabled, onClick = onBold) {
                Icon(Icons.Rounded.FormatBold, "Bold", modifier = Modifier.size(17.dp))
            }
            ToneChip(active = italic, enabled = enabled, onClick = onItalic) {
                Icon(Icons.Rounded.FormatItalic, "Italic", modifier = Modifier.size(17.dp))
            }
            ToneChip(active = underline, enabled = enabled, onClick = onUnderline) {
                Icon(Icons.Rounded.FormatUnderlined, "Underline", modifier = Modifier.size(17.dp))
            }
            ToneChip(active = alignCenter, enabled = enabled, onClick = onAlign) {
                Icon(
                    if (alignCenter) Icons.Rounded.FormatAlignCenter else Icons.Rounded.FormatAlignLeft,
                    "Align",
                    modifier = Modifier.size(17.dp)
                )
            }
            if (onHeading != null) {
                ToneChip(active = heading, enabled = enabled, onClick = onHeading) {
                    Icon(Icons.Rounded.AutoAwesome, "Heading", modifier = Modifier.size(16.dp))
                }
            }
            ToneChip(active = false, enabled = enabled, onClick = { onSize(-1) }) {
                Text("A-", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                if (size > 0) "$size" else "–",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Center
            )
            ToneChip(active = false, enabled = enabled, onClick = { onSize(+1) }) {
                Text("A+", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            ToneChip(active = color.isNotBlank(), enabled = enabled, onClick = { showInk = !showInk }) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(if (color.isBlank()) Color(0xFF1C1C1E) else inkOf(color), CircleShape)
                )
            }
            if (onAdd != null) {
                ToneChip(active = false, enabled = true, onClick = onAdd) {
                    Icon(Icons.Rounded.Add, "Add paragraph", modifier = Modifier.size(17.dp))
                }
            }
            if (onDelete != null) {
                ToneChip(active = false, enabled = enabled, onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, "Delete paragraph", modifier = Modifier.size(16.dp))
                }
            }
        }
        if (showInk && enabled) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                INKS.forEach { ink ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(if (ink.isBlank()) Color(0xFF1C1C1E) else inkOf(ink), CircleShape)
                            .border(
                                if (color == ink) 2.dp else 0.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                            .clickable { onColor(ink) }
                    )
                }
                Text(
                    if (color.isBlank()) "default" else "#$color",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToneChip(
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(34.dp)
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides
                if (enabled) {
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
        ) {
            content()
        }
    }
}

private fun inkOf(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor("#$hex")) }
    .getOrDefault(Color(0xFF1C1C1E))
