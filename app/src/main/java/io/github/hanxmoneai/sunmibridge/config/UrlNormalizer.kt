package io.github.hanxmoneai.sunmibridge.config

/** Turns what an operator types on the setup screen into a URL the WebView can load. */
object UrlNormalizer {

    /** Built-in page that exercises every bridge method. */
    const val DEMO_URL = "file:///android_asset/demo/index.html"

    /**
     * Returns the normalized URL, or `null` if the input cannot be used.
     *
     * - blank input → null
     * - `example.com/pos` → `https://example.com/pos` (HTTPS by default)
     * - `http://192.168.1.20:8080` is kept as typed (LAN servers are often plain HTTP)
     * - other schemes (`ftp://`, `javascript:`, `file://` except the demo page) → null
     */
    fun normalize(input: String?): String? {
        val text = input?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (text == DEMO_URL) return text

        val lower = text.lowercase()
        val url = when {
            lower.startsWith("http://") || lower.startsWith("https://") -> text
            lower.contains(":/") || lower.startsWith("javascript:") || lower.startsWith("data:") -> return null
            else -> "https://$text"
        }

        val host = url.substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
        if (host.isBlank() || host.any { it.isWhitespace() }) return null
        return url
    }
}
