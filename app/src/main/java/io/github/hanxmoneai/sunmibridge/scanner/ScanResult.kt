package io.github.hanxmoneai.sunmibridge.scanner

import io.github.hanxmoneai.sunmibridge.bridge.Json
import io.github.hanxmoneai.sunmibridge.bridge.RawJson

/** One decoded code, e.g. type `QR_CODE`, value `https://example.com`. */
data class ScanResult(val type: String, val value: String)

/** What the page receives in `onPosScanResult(result)`. */
data class ScanOutcome(
    val results: List<ScanResult>,
    val cancelled: Boolean = false,
    val error: String? = null,
) {
    val ok: Boolean get() = error == null && results.isNotEmpty()

    fun toJson(): String = Json.obj(
        "ok" to ok,
        "cancelled" to cancelled,
        "error" to error,
        "results" to results.map { RawJson(Json.obj("type" to it.type, "value" to it.value)) },
    )

    companion object {
        fun failure(message: String) = ScanOutcome(emptyList(), error = message)
    }
}

object ScanResultParser {

    /**
     * Sunmi's scanner app returns the `data` extra as an
     * `ArrayList<HashMap<String, String>>`, each map holding `TYPE` and `VALUE`.
     * Anything else (null, cancelled scan, unexpected shape) yields an empty list.
     */
    fun parse(raw: Any?): List<ScanResult> {
        val items = raw as? Iterable<*> ?: return emptyList()
        return items.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val value = map["VALUE"]?.toString() ?: return@mapNotNull null
            ScanResult(type = map["TYPE"]?.toString().orEmpty(), value = value)
        }
    }

    /**
     * The string the first version of the app passed to `scanCallBack(...)`,
     * e.g. `[{TYPE=QR_CODE, VALUE=123}]`. Kept so existing pages keep working.
     */
    fun legacyString(results: List<ScanResult>): String =
        results.joinToString(", ", "[", "]") { "{TYPE=${it.type}, VALUE=${it.value}}" }
}
