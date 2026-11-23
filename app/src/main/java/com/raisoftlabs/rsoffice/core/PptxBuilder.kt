package com.raisoftlabs.rsoffice.core

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Minimal, dependency-free OOXML (.pptx) writer.
 *
 * A python-pptx generated, slide-less template supplies the parts that are
 * painful to hand-write correctly (slide master, layouts, theme). Everything
 * else — presentation.xml, relationships, content types and the slides
 * themselves — is generated here so any document can be turned into a real,
 * editable PowerPoint file on device.
 */
object PptxBuilder {

    data class Palette(val bg: String, val fg: String, val accent: String, val dim: String)

    val NEON = Palette("0E1014", "F5F7FA", "43D95C", "9AA0A6")
    val PAPER = Palette("F7F4EE", "1A1A1A", "C8452A", "6B6B6B")
    val SLATE = Palette("0F172A", "F8FAFC", "60A5FA", "94A3B8")

    sealed interface Slide {
        data class Cover(val title: String, val subtitle: String) : Slide
        data class Text(
            val title: String,
            val bullets: List<String>,
            val pageLabel: String = ""
        ) : Slide

        data class Page(val bytes: ByteArray, val ext: String = "jpg", val caption: String? = null) : Slide
    }

    private const val NS = "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" " +
            "xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\""

    private const val XML_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"

    fun build(
        template: ByteArray,
        slides: List<Slide>,
        palette: Palette,
        widthEmu: Long = 12_192_000L,
        heightEmu: Long = 6_858_000L,
        out: OutputStream
    ) {
        require(slides.isNotEmpty()) { "deck needs at least one slide" }

        val parts = linkedMapOf<String, ByteArray>()
        ZipInputStream(template.inputStream()).use { zin ->
            var e = zin.nextEntry
            while (e != null) {
                val buf = ByteArrayOutputStream()
                zin.copyTo(buf)
                parts[e.name] = buf.toByteArray()
                zin.closeEntry()
                e = zin.nextEntry
            }
        }

        // Drop the stale thumbnail that shipped with the template.
        parts.remove("docProps/thumbnail.jpeg")
        parts["_rels/.rels"]?.let {
            val s = String(it, Charsets.UTF_8)
                .replace(Regex("<Relationship[^>]*thumbnail[^>]*/>"), "")
            parts["_rels/.rels"] = s.toByteArray(Charsets.UTF_8)
        }

        // --- presentation.xml : slide size + slide id list -----------------
        var pres = String(parts["ppt/presentation.xml"]!!, Charsets.UTF_8)
        pres = Regex("<p:sldSz[^/]*/>").replace(pres, "<p:sldSz cx=\"$widthEmu\" cy=\"$heightEmu\"/>")

        val relsXml = String(parts["ppt/_rels/presentation.xml.rels"]!!, Charsets.UTF_8)
        var maxRid = 0
        Regex("Id=\"rId(\\d+)\"").findAll(relsXml).forEach {
            maxRid = maxOf(maxRid, it.groupValues[1].toIntOrNull() ?: 0)
        }
        val firstSlideRid = maxRid + 1

        val sldIds = StringBuilder()
        val slideRels = StringBuilder()
        val contentOverrides = StringBuilder()
        for (i in slides.indices) {
            val rid = firstSlideRid + i
            sldIds.append("<p:sldId id=\"${256 + i}\" r:id=\"rId$rid\"/>")
            slideRels.append(
                "<Relationship Id=\"rId$rid\" " +
                        "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" " +
                        "Target=\"slides/slide${i + 1}.xml\"/>"
            )
            contentOverrides.append(
                "<Override PartName=\"/ppt/slides/slide${i + 1}.xml\" " +
                        "ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>"
            )
        }
        pres = Regex("<p:sldIdLst>.*?</p:sldIdLst>", RegexOption.DOT_MATCHES_ALL).replace(pres, "")
        pres = pres.replace("</p:sldMasterIdLst>", "</p:sldMasterIdLst><p:sldIdLst>$sldIds</p:sldIdLst>")
        parts["ppt/presentation.xml"] = pres.toByteArray(Charsets.UTF_8)

        val keptRels = Regex("<Relationship [^>]*/>").findAll(relsXml)
            .map { it.value }
            .filter { !it.contains("slides/slide") }
            .joinToString("")
        parts["ppt/_rels/presentation.xml.rels"] = (
                XML_HEAD +
                        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                        keptRels + slideRels + "</Relationships>"
                ).toByteArray(Charsets.UTF_8)

        // --- [Content_Types].xml ------------------------------------------
        var ct = String(parts["[Content_Types].xml"]!!, Charsets.UTF_8)
        if (!ct.contains("Extension=\"png\"")) {
            ct = ct.replace(
                "<Default Extension=\"xml\"",
                "<Default Extension=\"png\" ContentType=\"image/png\"/><Default Extension=\"xml\""
            )
        }
        if (!ct.contains("Extension=\"jpeg\"")) {
            ct = ct.replace(
                "<Default Extension=\"xml\"",
                "<Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/><Default Extension=\"xml\""
            )
        }
        ct = ct.replace("</Types>", "$contentOverrides</Types>")
        parts["[Content_Types].xml"] = ct.toByteArray(Charsets.UTF_8)

        // --- slides -------------------------------------------------------
        slides.forEachIndexed { i, slide ->
            val n = i + 1
            when (slide) {
                is Slide.Cover -> {
                    parts["ppt/slides/slide$n.xml"] =
                        coverXml(slide.title, slide.subtitle, palette).toByteArray(Charsets.UTF_8)
                    parts["ppt/slides/_rels/slide$n.xml.rels"] = slideLayoutRels(n)
                }

                is Slide.Text -> {
                    parts["ppt/slides/slide$n.xml"] =
                        textXml(slide.title, slide.bullets, slide.pageLabel, palette).toByteArray(Charsets.UTF_8)
                    parts["ppt/slides/_rels/slide$n.xml.rels"] = slideLayoutRels(n)
                }

                is Slide.Page -> {
                    parts["ppt/slides/slide$n.xml"] =
                        pageXml(n, slide.caption, palette, widthEmu, heightEmu).toByteArray(Charsets.UTF_8)
                    val ext = slide.ext.lowercase().removePrefix(".")
                    parts["ppt/slides/_rels/slide$n.xml.rels"] = imageRels(n, ext)
                    parts["ppt/media/image$n.$ext"] = slide.bytes
                }
            }
        }

        ZipOutputStream(out).use { zos ->
            // content types first — friendliest for streaming readers
            val ordered = linkedMapOf<String, ByteArray>()
            parts["[Content_Types].xml"]?.let { ordered["[Content_Types].xml"] = it }
            parts.forEach { (k, v) -> if (k != "[Content_Types].xml") ordered[k] = v }
            ordered.forEach { (name, bytes) ->
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }
        }
    }

    /* ------------------------------------------------------------------ */

    private fun slideLayoutRels(n: Int): ByteArray = (
            XML_HEAD +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" " +
                    "Target=\"../slideLayouts/slideLayout7.xml\"/></Relationships>"
            ).toByteArray(Charsets.UTF_8)

    private fun imageRels(n: Int, ext: String): ByteArray = (
            XML_HEAD +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" " +
                    "Target=\"../media/image$n.$ext\"/></Relationships>"
            ).toByteArray(Charsets.UTF_8)

    private const val SPTREE_HEAD =
        "<p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>" +
                "<p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/>" +
                "<a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>"

    private fun esc(s: String): String {
        val sb = StringBuilder(s.length + 16)
        for (c in s) {
            when {
                c == '&' -> sb.append("&amp;")
                c == '<' -> sb.append("&lt;")
                c == '>' -> sb.append("&gt;")
                c == '"' -> sb.append("&quot;")
                c == '\'' -> sb.append("&apos;")
                c.code < 0x20 && c != '\n' && c != '\t' -> Unit
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun xfrm(x: Long, y: Long, cx: Long, cy: Long) =
        "<a:xfrm><a:off x=\"$x\" y=\"$y\"/><a:ext cx=\"$cx\" cy=\"$cy\"/></a:xfrm>"

    private fun srgb(c: String) = "<a:solidFill><a:srgbClr val=\"$c\"/></a:solidFill>"

    private fun solidShape(id: Int, name: String, x: Long, y: Long, cx: Long, cy: Long, color: String) =
        "<p:sp><p:nvSpPr><p:cNvPr id=\"$id\" name=\"${esc(name)}\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>" +
                "<p:spPr>${xfrm(x, y, cx, cy)}<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom>" +
                srgb(color) + "<a:ln><a:noFill/></a:ln></p:spPr>" +
                "<p:txBody><a:bodyPr/><a:lstStyle/><a:p/></p:txBody></p:sp>"

    private fun textBox(
        id: Int,
        name: String,
        x: Long,
        y: Long,
        cx: Long,
        cy: Long,
        anchor: String,
        paragraphs: String
    ) = "<p:sp><p:nvSpPr><p:cNvPr id=\"$id\" name=\"${esc(name)}\"/><p:cNvSpPr txBox=\"1\"/><p:nvPr/></p:nvSpPr>" +
            "<p:spPr>${xfrm(x, y, cx, cy)}<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:noFill/></p:spPr>" +
            "<p:txBody><a:bodyPr wrap=\"square\" lIns=\"0\" rIns=\"0\" tIns=\"0\" bIns=\"0\" anchor=\"$anchor\">" +
            "<a:normAutofit/></a:bodyPr><a:lstStyle/>$paragraphs</p:txBody></p:sp>"

    private fun run(text: String, size: Int, color: String, bold: Boolean, font: String = "+mn-lt") =
        "<a:r><a:rPr lang=\"en-US\" sz=\"$size\" b=\"${if (bold) 1 else 0}\" dirty=\"0\">" +
                srgb(color) + "<a:latin typeface=\"$font\"/><a:cs typeface=\"$font\"/></a:rPr>" +
                "<a:t>${esc(text)}</a:t></a:r>"

    private fun para(
        runs: String,
        align: String = "l",
        spaceBefore: Int = 0,
        linePct: Int = 100_000,
        bullet: Boolean = false,
        marL: Int = 0,
        indent: Int = 0
    ): String {
        val ppr = StringBuilder("<a:pPr algn=\"$align\" marL=\"$marL\" indent=\"$indent\">")
        ppr.append("<a:lnSpc><a:spcPct val=\"$linePct\"/></a:lnSpc>")
        ppr.append("<a:spcBef><a:spcPts val=\"$spaceBefore\"/></a:spcBef>")
        ppr.append("<a:spcAft><a:spcPts val=\"0\"/></a:spcAft>")
        if (bullet) ppr.append("<a:buFont typeface=\"Arial\"/><a:buChar char=\"&#8226;\"/>")
        else ppr.append("<a:buNone/>")
        ppr.append("</a:pPr>")
        return "<a:p>$ppr$runs</a:p>"
    }

    private fun coverXml(title: String, subtitle: String, p: Palette): String {
        val t = fitSize(title, 4400, 2600, 34)
        val shapes = solidShape(2, "Rule", 838200, 2400000, 914400, 54864, p.accent) +
                textBox(
                    3, "Kicker", 838200, 1700000, 9_000_000, 400000, "t",
                    para(run(subtitle.uppercase(), 1400, p.accent, true), linePct = 100_000)
                ) +
                textBox(
                    4, "Title", 838200, 2600000, 10_515_600, 2_400_000, "t",
                    para(run(title, t, p.fg, true), linePct = 95_000)
                ) +
                textBox(
                    5, "Footer", 838200, 6_000_000, 8_000_000, 400000, "t",
                    para(run("Made with RS Office", 1100, p.dim, false), linePct = 100_000)
                )
        return "$XML_HEAD<p:sld $NS><p:cSld>${bgXml(p.bg)}<p:spTree>$SPTREE_HEAD$shapes</p:spTree></p:cSld>" +
                "<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>"
    }

    private fun textXml(title: String, bullets: List<String>, page: String, p: Palette): String {
        val safeBullets = bullets.take(9).map { it.trim() }.filter { it.isNotEmpty() }
        val chars = safeBullets.sumOf { it.length }
        val bodySize = when {
            chars <= 190 -> 2100
            chars <= 330 -> 1900
            chars <= 480 -> 1700
            chars <= 640 -> 1550
            else -> 1400
        }
        val titleSize = fitSize(title, 3400, 2200, 34)

        val sb = StringBuilder()
        safeBullets.forEach { b ->
            val text = if (b.length > 190) b.substring(0, 187).trimEnd() + "…" else b
            sb.append(
                para(
                    run(text, bodySize, p.fg, false),
                    spaceBefore = if (bodySize >= 1900) 1100 else 800,
                    linePct = 112_000,
                    bullet = true,
                    marL = 285750,
                    indent = -285750
                )
            )
        }

        val shapes = solidShape(2, "Rule", 838200, 1180000, 640080, 45720, p.accent) +
                textBox(
                    3, "Title", 838200, 1350000, 10_515_600, 1_500_000, "t",
                    para(run(title, titleSize, p.fg, true), linePct = 95_000)
                ) +
                (if (safeBullets.isNotEmpty()) textBox(
                    4, "Body", 838200, 2_900_000, 10_515_600, 3_400_000, "t", sb.toString()
                ) else "") +
                textBox(
                    5, "Page", 10_515_600, 6_250_000, 838200, 300000, "t",
                    para(run(page, 1100, p.dim, false), align = "r")
                )
        return "$XML_HEAD<p:sld $NS><p:cSld>${bgXml(p.bg)}<p:spTree>$SPTREE_HEAD$shapes</p:spTree></p:cSld>" +
                "<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>"
    }

    private fun pageXml(n: Int, caption: String?, p: Palette, sw: Long, sh: Long): String {
        val shapes: String = if (caption.isNullOrBlank()) {
            picXml(2, n, sw, sh)
        } else {
            val imgH = (sh * 0.9).toLong()
            picXml(2, n, sw, imgH) +
                    solidShape(3, "Bar", 0, imgH, sw, sh - imgH, p.bg) +
                    textBox(
                        4, "Caption", 320040, imgH + 60_000, sw - 640_080, 320_000, "t",
                        para(run(caption, 1200, p.dim, false))
                    )
        }
        return "$XML_HEAD<p:sld $NS><p:cSld><p:spTree>$SPTREE_HEAD$shapes</p:spTree></p:cSld>" +
                "<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>"
    }

    private fun picXml(id: Int, n: Int, cx: Long, cy: Long) =
        "<p:pic><p:nvPicPr><p:cNvPr id=\"$id\" name=\"Page $n\"/>" +
                "<p:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></p:cNvPicPr><p:nvPr/></p:nvPicPr>" +
                "<p:blipFill><a:blip r:embed=\"rId1\"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>" +
                "<p:spPr>${xfrm(0, 0, cx, cy)}<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom>" +
                "<a:ln><a:noFill/></a:ln></p:spPr></p:pic>"

    private fun bgXml(color: String) =
        "<p:bg><p:bgPr>${srgb(color)}<a:effectLst/></p:bgPr></p:bg>"

    /** Rough shrink-to-fit: long titles get a smaller point size. */
    private fun fitSize(text: String, base: Int, min: Int, widthChars: Int): Int {
        val len = text.length
        val factor = when {
            len <= widthChars -> 1f
            len <= widthChars * 2 -> 0.78f
            len <= widthChars * 3 -> 0.62f
            else -> 0.52f
        }
        return maxOf(min, (base * factor).toInt())
    }
}
