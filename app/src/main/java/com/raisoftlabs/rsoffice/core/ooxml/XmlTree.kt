package com.raisoftlabs.rsoffice.core.ooxml

/**
 * Minimal, namespace-preserving XML tree used by our own OOXML mappers.
 *
 * The goal is not to be a general XML library. It is to let us edit the parts
 * we understand while keeping element order, attributes and unknown nodes from
 * files produced by other office suites.
 */
sealed interface XmlNode {
    data class Element(
        val name: String,
        val attributes: LinkedHashMap<String, String> = LinkedHashMap(),
        val children: MutableList<XmlNode> = mutableListOf(),
        var selfClosing: Boolean = false
    ) : XmlNode

    data class Text(val value: String) : XmlNode
    data class Comment(val value: String) : XmlNode
    data class CData(val value: String) : XmlNode
    data class Instruction(val value: String) : XmlNode
}

data class XmlDocument(
    val root: XmlNode.Element,
    val preamble: String = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
) {
    fun serialize(): String = preamble + root.serialize()
}

fun XmlNode.Element.child(name: String): XmlNode.Element? =
    children.firstOrNull { it is XmlNode.Element && it.name == name } as? XmlNode.Element

fun XmlNode.Element.children(name: String): List<XmlNode.Element> =
    children.filterIsInstance<XmlNode.Element>().filter { it.name == name }

fun XmlNode.Element.descendants(name: String): List<XmlNode.Element> {
    val result = mutableListOf<XmlNode.Element>()
    fun walk(node: XmlNode.Element) {
        node.children.filterIsInstance<XmlNode.Element>().forEach { child ->
            if (child.name == name) result += child
            walk(child)
        }
    }
    walk(this)
    return result
}

fun XmlNode.Element.textValue(): String = buildString {
    fun walk(node: XmlNode) {
        when (node) {
            is XmlNode.Text -> append(node.value)
            is XmlNode.CData -> append(node.value)
            is XmlNode.Element -> node.children.forEach { walk(it) }
            else -> Unit
        }
    }
    children.forEach { walk(it) }
}

fun XmlNode.Element.setText(value: String) {
    children.clear()
    if (value.isNotEmpty()) children += XmlNode.Text(value)
    selfClosing = false
}

fun XmlNode.Element.setAttribute(name: String, value: String) {
    attributes[name] = value
}

fun XmlNode.Element.removeChild(child: XmlNode) {
    children.remove(child)
}

private const val DECL = "<?xml"

object Xml {

    fun parse(source: String): XmlDocument {
        var pos = 0
        var preamble = DECL + " version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        while (pos < source.length) {
            when {
                source.startsWith(DECL, pos) -> {
                    val end = source.indexOf("?>", pos)
                    if (end < 0) break
                    preamble = source.substring(pos, end + 2)
                    pos = end + 2
                }
                source.startsWith("<!--", pos) -> pos = skipTo(source, "-->", pos + 4)
                source.startsWith("<!DOCTYPE", pos) -> pos = skipTo(source, ">", pos + 9)
                source[pos].isWhitespace() -> pos++
                else -> break
            }
        }
        val root = parseElement(source, pos).first
        return XmlDocument(root, preamble)
    }

    private fun skipTo(source: String, marker: String, from: Int): Int {
        val end = source.indexOf(marker, from)
        return if (end < 0) source.length else end + marker.length
    }

    private fun parseElement(source: String, start: Int): Pair<XmlNode.Element, Int> {
        var pos = start
        require(pos < source.length && source[pos] == '<') { "Expected '<' at $pos" }
        pos++
        val nameStart = pos
        while (pos < source.length && !source[pos].isWhitespace() && source[pos] != '/' && source[pos] != '>') pos++
        val name = source.substring(nameStart, pos)
        require(name.isNotEmpty()) { "Empty element name at $nameStart" }

        val attributes = LinkedHashMap<String, String>()
        while (pos < source.length) {
            while (pos < source.length && source[pos].isWhitespace()) pos++
            if (pos >= source.length) break
            if (source.startsWith("/>", pos)) {
                return XmlNode.Element(name, attributes, selfClosing = true) to pos + 2
            }
            if (source[pos] == '>') {
                pos++
                break
            }
            val attrStart = pos
            while (pos < source.length && !source[pos].isWhitespace() && source[pos] != '=' && source[pos] != '>') pos++
            val attrName = source.substring(attrStart, pos)
            while (pos < source.length && source[pos].isWhitespace()) pos++
            require(pos < source.length && source[pos] == '=') { "Expected '=' for $attrName" }
            pos++
            while (pos < source.length && source[pos].isWhitespace()) pos++
            require(pos < source.length && (source[pos] == '"' || source[pos] == '\'')) {
                "Expected quote for $attrName"
            }
            val quote = source[pos++]
            val valueStart = pos
            while (pos < source.length && source[pos] != quote) pos++
            attributes[attrName] = decode(source.substring(valueStart, pos))
            pos++
        }

        val children = mutableListOf<XmlNode>()
        while (pos < source.length) {
            when {
                source.startsWith("</", pos) -> {
                    val closeEnd = source.indexOf('>', pos)
                    require(closeEnd >= 0) { "Unclosed element $name" }
                    val closeName = source.substring(pos + 2, closeEnd).trim()
                    require(closeName == name) { "Mismatched </$closeName> for <$name>" }
                    return XmlNode.Element(name, attributes, children) to closeEnd + 1
                }
                source.startsWith("<!--", pos) -> {
                    val end = source.indexOf("-->", pos + 4)
                    require(end >= 0) { "Unclosed comment" }
                    children += XmlNode.Comment(source.substring(pos + 4, end))
                    pos = end + 3
                }
                source.startsWith("<![CDATA[", pos) -> {
                    val end = source.indexOf("]]>", pos + 9)
                    require(end >= 0) { "Unclosed CDATA" }
                    children += XmlNode.CData(source.substring(pos + 9, end))
                    pos = end + 3
                }
                source.startsWith("<?", pos) -> {
                    val end = source.indexOf("?>", pos + 2)
                    require(end >= 0) { "Unclosed instruction" }
                    children += XmlNode.Instruction(source.substring(pos + 2, end))
                    pos = end + 2
                }
                source[pos] == '<' -> {
                    val child = parseElement(source, pos)
                    children += child.first
                    pos = child.second
                }
                else -> {
                    val end = source.indexOf('<', pos)
                    val stop = if (end < 0) source.length else end
                    val raw = source.substring(pos, stop)
                    if (raw.isNotBlank()) children += XmlNode.Text(decode(raw))
                    pos = stop
                }
            }
        }
        error("Unclosed element $name")
    }

    private fun encodeText(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                else -> append(ch)
            }
        }
    }

    private fun encodeAttr(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }

    private fun decode(value: String): String = buildString(value.length) {
        var i = 0
        while (i < value.length) {
            if (value[i] == '&') {
                val semi = value.indexOf(';', i + 1)
                if (semi in (i + 1)..(i + 12)) {
                    val entity = value.substring(i + 1, semi)
                    val decoded = when (entity) {
                        "amp" -> "&"
                        "lt" -> "<"
                        "gt" -> ">"
                        "quot" -> "\""
                        "apos" -> "'"
                        else -> when {
                            entity.startsWith("#x", ignoreCase = true) ->
                                entity.substring(2).toIntOrNull(16)?.let { Char(it).toString() }
                            entity.startsWith("#") ->
                                entity.substring(1).toIntOrNull()?.let { Char(it).toString() }
                            else -> null
                        }
                    }
                    if (decoded != null) {
                        append(decoded)
                        i = semi + 1
                        continue
                    }
                }
            }
            append(value[i])
            i++
        }
    }

    private fun XmlNode.serialize(): String = when (this) {
        is XmlNode.Element -> buildString {
            append('<').append(name)
            attributes.forEach { (key, value) ->
                append(' ').append(key).append("=\"").append(encodeAttr(value)).append('"')
            }
            if (selfClosing) {
                append("/>")
            } else {
                append('>')
                children.forEach { append(it.serialize()) }
                append("</").append(name).append('>')
            }
        }
        is XmlNode.Text -> encodeText(value)
        is XmlNode.Comment -> "<!--$value-->"
        is XmlNode.CData -> "<![CDATA[$value]]>"
        is XmlNode.Instruction -> "<?$value?>"
    }
}
