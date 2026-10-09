package io.github.hanxmoneai.sunmibridge.printer

import io.github.hanxmoneai.sunmibridge.bridge.Json

/** A print job requested by the web page, independent of the printer SDK. */
sealed class PrintCommand {

    /** 1D barcode; the code is printed again below in large bold text for easy reading. */
    data class Barcode(val content: String, val showText: Boolean = true) : PrintCommand()

    data class QrCode(val content: String) : PrintCommand()

    data class Text(
        val content: String,
        val size: Int = DEFAULT_TEXT_SIZE,
        val bold: Boolean = false,
        val align: Alignment = Alignment.LEFT,
    ) : PrintCommand()

    enum class Alignment {
        LEFT, CENTER, RIGHT;

        companion object {
            fun parse(value: String?): Alignment =
                entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: LEFT
        }
    }

    companion object {
        const val DEFAULT_TEXT_SIZE = 24
        const val MIN_TEXT_SIZE = 12
        const val MAX_TEXT_SIZE = 64

        private val QR_TYPES = setOf("qrcode", "qr")

        /**
         * Maps the original `print(type, content)` call: type "QR Code" prints a QR code,
         * any other non-blank type prints a barcode, and a blank type or content prints nothing.
         */
        fun fromLegacy(type: String?, content: String?): PrintCommand? {
            if (type.isNullOrBlank() || content.isNullOrBlank()) return null
            val normalized = type.replace(" ", "").lowercase()
            return if (normalized in QR_TYPES) QrCode(content) else Barcode(content)
        }

        fun text(content: String?, size: Int, bold: Boolean, align: String?): PrintCommand? {
            if (content.isNullOrEmpty()) return null
            return Text(content, size.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE), bold, Alignment.parse(align))
        }
    }
}

/** What the page receives in `onPosPrintResult(result)`. */
data class PrintOutcome(
    val ok: Boolean,
    val code: Int,
    val message: String,
    val printerStatus: String?,
) {
    fun toJson(): String = Json.obj(
        "ok" to ok,
        "code" to code,
        "message" to message,
        "printerStatus" to printerStatus,
    )

    companion object {
        const val CODE_PRINTER_UNAVAILABLE = -1
        const val CODE_SDK_ERROR = -2
        const val CODE_NOTHING_TO_PRINT = -3

        fun failure(code: Int, message: String, printerStatus: String? = null) =
            PrintOutcome(false, code, message, printerStatus)
    }
}
