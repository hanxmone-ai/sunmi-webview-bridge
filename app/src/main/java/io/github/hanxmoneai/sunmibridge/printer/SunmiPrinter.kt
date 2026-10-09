package io.github.hanxmoneai.sunmibridge.printer

import android.content.Context
import android.util.Log
import com.sunmi.printerx.PrinterSdk
import com.sunmi.printerx.api.PrintResult
import com.sunmi.printerx.enums.Align
import com.sunmi.printerx.enums.ErrorLevel
import com.sunmi.printerx.enums.HumanReadable
import com.sunmi.printerx.style.BarcodeStyle
import com.sunmi.printerx.style.QrStyle
import com.sunmi.printerx.style.TextStyle

/**
 * Thin wrapper around Sunmi's printerx SDK.
 *
 * On devices without a Sunmi printer (emulators, ordinary phones) [isAvailable] stays
 * false and every job fails fast with [PrintOutcome.CODE_PRINTER_UNAVAILABLE], so the
 * web page can be developed and tested anywhere.
 */
class SunmiPrinter(context: Context) {

    fun interface Listener {
        fun onAvailabilityChanged(available: Boolean)
    }

    @Volatile
    private var printer: PrinterSdk.Printer? = null

    @Volatile
    var listener: Listener? = null

    val isAvailable: Boolean get() = printer != null

    init {
        try {
            PrinterSdk.getInstance().getPrinter(context.applicationContext, object : PrinterSdk.PrinterListen {
                override fun onDefPrinter(printer: PrinterSdk.Printer?) {
                    this@SunmiPrinter.printer = printer
                    listener?.onAvailabilityChanged(printer != null)
                }

                override fun onPrinters(printers: MutableList<PrinterSdk.Printer>?) = Unit
            })
        } catch (e: Exception) {
            Log.w(TAG, "Sunmi printer service not available on this device", e)
        }
    }

    /** Printer state as reported by the SDK (e.g. READY, OUT_OF_PAPER), or null if unknown. */
    fun statusName(): String? {
        val current = printer ?: return null
        return try {
            current.queryApi().status?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Prints [command] as one transaction. [callback] runs on an SDK thread, exactly once.
     */
    fun print(command: PrintCommand, callback: (PrintOutcome) -> Unit) {
        val current = printer
        if (current == null) {
            callback(PrintOutcome.failure(PrintOutcome.CODE_PRINTER_UNAVAILABLE, "Printer not available"))
            return
        }
        try {
            val api = current.lineApi()
            // Transaction mode buffers the commands and reports one result for the whole job.
            api.enableTransMode(true)
            when (command) {
                is PrintCommand.Barcode -> {
                    val style = BarcodeStyle.getStyle()
                        .setAlign(Align.CENTER)
                        .setDotWidth(2)
                        .setBarHeight(100)
                        .setReadable(HumanReadable.HIDE)
                    api.printBarCode(command.content, style)
                    if (command.showText) {
                        // The SDK's built-in caption is small; print the value on its own line instead.
                        api.printText(command.content, TextStyle().setTextSize(36).setAlign(Align.CENTER).enableBold(true))
                    }
                    api.printText("\n", TextStyle().setAlign(Align.CENTER))
                }

                is PrintCommand.QrCode -> {
                    val style = QrStyle.getStyle()
                        .setAlign(Align.CENTER)
                        .setDot(9)
                        .setErrorLevel(ErrorLevel.L)
                    api.printQrCode(command.content, style)
                    repeat(3) { api.printText("\n", TextStyle().setAlign(Align.CENTER)) }
                }

                is PrintCommand.Text -> {
                    val style = TextStyle()
                        .setTextSize(command.size)
                        .setAlign(command.align.toSunmi())
                        .enableBold(command.bold)
                    api.printText(command.content, style)
                    api.printText("\n", TextStyle())
                }
            }
            api.autoOut()
            api.printTrans(object : PrintResult() {
                override fun onResult(resultCode: Int, message: String?) {
                    callback(PrintOutcome(resultCode == 0, resultCode, message.orEmpty(), statusName()))
                }
            })
            api.enableTransMode(false)
        } catch (e: Exception) {
            Log.e(TAG, "Print failed", e)
            callback(
                PrintOutcome.failure(
                    PrintOutcome.CODE_SDK_ERROR,
                    e.message ?: e.javaClass.simpleName,
                    statusName(),
                ),
            )
        }
    }

    private fun PrintCommand.Alignment.toSunmi(): Align = when (this) {
        PrintCommand.Alignment.LEFT -> Align.LEFT
        PrintCommand.Alignment.CENTER -> Align.CENTER
        PrintCommand.Alignment.RIGHT -> Align.RIGHT
    }

    private companion object {
        const val TAG = "SunmiPrinter"
    }
}
