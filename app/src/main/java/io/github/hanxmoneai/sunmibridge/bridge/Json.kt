package io.github.hanxmoneai.sunmibridge.bridge

import java.util.Locale

/** Already-serialized JSON that should be embedded as-is. */
data class RawJson(val json: String)

/**
 * Minimal JSON writer for the payloads sent back to the web page.
 *
 * Kept free of Android classes (org.json is stubbed in local unit tests) so the
 * escaping rules can be unit-tested on the JVM. Escaping matters here because the
 * result is injected into the page with evaluateJavascript: a scanned value such as
 * `abc')alert(1)//` must stay a string, not become code.
 */
object Json {

    fun quote(value: String?): String {
        if (value == null) return "null"
        val out = StringBuilder(value.length + 2)
        out.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                // Valid in JSON but line terminators in older JavaScript engines.
                ' ' -> out.append("\\u2028")
                ' ' -> out.append("\\u2029")
                // Avoid closing a surrounding <script> block if the script is ever inlined.
                '<' -> out.append("\\u003c")
                else -> if (ch < ' ') {
                    out.append(String.format(Locale.ROOT, "\\u%04x", ch.code))
                } else {
                    out.append(ch)
                }
            }
        }
        out.append('"')
        return out.toString()
    }

    fun value(value: Any?): String = when (value) {
        null -> "null"
        is RawJson -> value.json
        is Boolean -> value.toString()
        is Int, is Long, is Short, is Byte -> value.toString()
        is String -> quote(value)
        is Iterable<*> -> value.joinToString(",", "[", "]") { value(it) }
        else -> quote(value.toString())
    }

    fun obj(vararg fields: Pair<String, Any?>): String =
        fields.joinToString(",", "{", "}") { (key, v) -> quote(key) + ":" + value(v) }
}
