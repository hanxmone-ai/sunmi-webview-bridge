package io.github.hanxmoneai.sunmibridge.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Launches the scanner app that ships with Sunmi devices.
 * The scan result comes back through `onActivityResult` and is read by [ScanResultParser].
 */
object SunmiScanner {

    const val PACKAGE = "com.sunmi.sunmiqrcodescanner"

    /** Spelled "summi" in Sunmi's own documentation and app; do not "fix" it. */
    private const val ACTION_SCAN = "com.summi.scan"

    /** Sunmi option: pick the best preview resolution for the device. */
    private const val PPI_BEST = 0x0003

    private val SYMBOLOGIES = listOf(
        "IS_QR_CODE_ENABLE", "IS_PDF417_ENABLE", "IS_DATA_MATRIX_ENABLE", "IS_AZTEC_ENABLE",
        "IS_Micro_PDF417_ENABLE", "IS_MicroQR_ENABLE", "IS_Hanxin_ENABLE",
        "IS_EAN_8_ENABLE", "IS_EAN_13_ENABLE", "IS_UPC_A_ENABLE", "IS_UPC_E_ENABLE",
        "IS_ISBN_10_ENABLE", "IS_ISBN_13_ENABLE", "IS_CODE_11_ENABLE", "IS_CODE_39_ENABLE",
        "IS_CODE_93_ENABLE", "IS_CODE_128_ENABLE", "IS_CODABAR_ENABLE",
        "IS_INTERLEAVED_2_OF_5_ENABLE", "IS_DATABAR_ENABLE", "IS_DATABAR_EXP_ENABLE",
    )

    fun createIntent(): Intent = Intent(ACTION_SCAN).apply {
        setPackage(PACKAGE)
        putExtra("CURRENT_PPI", PPI_BEST)
        putExtra("PLAY_SOUND", true)
        putExtra("PLAY_VIBRATE", true)
        putExtra("IDENTIFY_MORE_CODE", false)
        putExtra("IDENTIFY_INVERSE", true)
        putExtra("IS_SHOW_SETTING", true)
        putExtra("IS_SHOW_ALBUM", true)
        putExtra("IS_OPEN_LIGHT", true)
        putExtra("SCAN_MODE", false)
        SYMBOLOGIES.forEach { putExtra(it, true) }
    }

    @Suppress("DEPRECATION")
    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Reads the raw `data` extra from the scanner's result intent. */
    @Suppress("DEPRECATION")
    fun readRawResult(data: Intent?): Any? = data?.extras?.getSerializable("data")
}
