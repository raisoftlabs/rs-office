package com.raisoftlabs.vellum.core

/** Tiny XML text helper shared by the Office readers. */
internal object XmlText {
    fun unescape(s: String): String = s
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}
