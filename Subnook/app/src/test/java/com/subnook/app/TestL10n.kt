package com.subnook.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * JVM-тесты идут без Android-контекста, поэтому строки берутся прямо из
 * res/values-zh/strings.xml: так проверки оригинальных (китайских) подписей остаются как были.
 */
object TestL10n {
    private val zh: Map<Int, String> by lazy { load() }

    fun zh() {
        L10n.testStrings = { id -> zh[id] ?: error("Нет строки для id $id") }
    }

    private fun load(): Map<Int, String> {
        val file = listOf("src/main/res/values-zh/strings.xml", "app/src/main/res/values-zh/strings.xml")
            .map(::File).first { it.exists() }
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val byName = HashMap<String, String>()
        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val e = strings.item(i) as Element
            byName["string/" + e.getAttribute("name")] = unescape(e.textContent)
        }
        val plurals = doc.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val e = plurals.item(i) as Element
            val item = e.getElementsByTagName("item").item(0)
            byName["plurals/" + e.getAttribute("name")] = unescape(item.textContent)
        }
        val ids = HashMap<Int, String>()
        for ((type, cls) in listOf("string" to R.string::class.java, "plurals" to R.plurals::class.java)) {
            for (f in cls.fields) {
                val v = byName["$type/${f.name}"] ?: continue
                ids[f.getInt(null)] = v
            }
        }
        return ids
    }

    private fun unescape(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                out.append(when (val n = s[i + 1]) { 'n' -> '\n'; 't' -> '\t'; else -> n })
                i += 2
            } else {
                out.append(c); i++
            }
        }
        return out.toString()
    }
}
