package io.github.hanxmoneai.sunmibridge.bridge

import android.webkit.JavascriptInterface
import io.github.hanxmoneai.sunmibridge.printer.PrintCommand
import io.github.hanxmoneai.sunmibridge.printer.PrintOutcome
import io.github.hanxmoneai.sunmibridge.printer.SunmiPrinter
import io.github.hanxmoneai.sunmibridge.scanner.ScanOutcome
import io.github.hanxmoneai.sunmibridge.scanner.ScanResultParser

/**
 * The object exposed to JavaScript as `window.PosBridge` (and as `window.android`
 * for pages written against the first version of this app).
 *
 * JavaScript calls arrive on WebView's background "JavaBridge" thread, so every call
 * that touches the UI or the WebView is posted to the main thread through [Host].
 * Results go back to the page as JSON through these global callbacks:
 *
 * - `onPosScanResult({ok, cancelled, error, results: [{type, value}]})`
 * - `onPosPrintResult({ok, code, message, printerStatus})`
 * - legacy: `scanCallBack(string)` and `printCallBack(printerStatus)`
 */
class PosBridge(
    private val host: Host,
    private val printer: SunmiPrinter,
) {

    interface Host {
        /** Starts the Sunmi scanner; returns false if it is not installed. Main thread only. */
        fun launchScanner(): Boolean

        fun runOnMain(block: () -> Unit)

        /** Runs [script] in the page. Main thread only. */
        fun evaluate(script: String)
    }

    // ---- Called from JavaScript ---------------------------------------------------

    /** Lets the page detect the bridge: `window.PosBridge && PosBridge.isReady()`. */
    @JavascriptInterface
    fun isReady(): Boolean = true

    /** Returns `{"available": bool, "status": string|null}` as a JSON string. */
    @JavascriptInterface
    fun getPrinterStatus(): String = Json.obj(
        "available" to printer.isAvailable,
        "status" to printer.statusName(),
    )

    @JavascriptInterface
    fun scan() {
        host.runOnMain {
            if (!host.launchScanner()) {
                deliverScan(ScanOutcome.failure("Sunmi scanner app not found on this device"))
            }
        }
    }

    @JavascriptInterface
    fun printBarcode(content: String?) {
        submit(if (content.isNullOrBlank()) null else PrintCommand.Barcode(content))
    }

    @JavascriptInterface
    fun printQrCode(content: String?) {
        submit(if (content.isNullOrBlank()) null else PrintCommand.QrCode(content))
    }

    /** @param align "left", "center" or "right" */
    @JavascriptInterface
    fun printText(content: String?, size: Int, bold: Boolean, align: String?) {
        submit(PrintCommand.text(content, size, bold, align))
    }

    /** First-version API: `android.print("QR Code" | "<barcode type>", content)`. */
    @JavascriptInterface
    fun print(type: String?, content: String?) {
        submit(PrintCommand.fromLegacy(type, content), legacy = true)
    }

    // ---- Called from the Activity -------------------------------------------------

    fun deliverScan(outcome: ScanOutcome) {
        host.runOnMain {
            host.evaluate(JsCallback.invoke("onPosScanResult", outcome.toJson()))
            if (outcome.ok) {
                val legacy = ScanResultParser.legacyString(outcome.results)
                host.evaluate(JsCallback.invoke("scanCallBack", Json.quote(legacy)))
            }
        }
    }

    // -------------------------------------------------------------------------------

    private fun submit(command: PrintCommand?, legacy: Boolean = false) {
        if (command == null) {
            deliverPrint(
                PrintOutcome.failure(PrintOutcome.CODE_NOTHING_TO_PRINT, "Nothing to print"),
                legacy = false,
            )
            return
        }
        printer.print(command) { outcome -> deliverPrint(outcome, legacy) }
    }

    private fun deliverPrint(outcome: PrintOutcome, legacy: Boolean) {
        host.runOnMain {
            host.evaluate(JsCallback.invoke("onPosPrintResult", outcome.toJson()))
            if (legacy) {
                host.evaluate(JsCallback.invoke("printCallBack", Json.quote(outcome.printerStatus.orEmpty())))
            }
        }
    }
}
