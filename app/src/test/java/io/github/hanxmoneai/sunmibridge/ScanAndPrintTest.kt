package io.github.hanxmoneai.sunmibridge

import io.github.hanxmoneai.sunmibridge.printer.PrintCommand
import io.github.hanxmoneai.sunmibridge.printer.PrintOutcome
import io.github.hanxmoneai.sunmibridge.scanner.ScanOutcome
import io.github.hanxmoneai.sunmibridge.scanner.ScanResult
import io.github.hanxmoneai.sunmibridge.scanner.ScanResultParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanAndPrintTest {

    @Test
    fun parse_readsSunmiResultShape() {
        val raw = arrayListOf(
            hashMapOf("TYPE" to "QR_CODE", "VALUE" to "https://example.com"),
            hashMapOf("TYPE" to "EAN_13", "VALUE" to "6901234567892"),
        )
        assertEquals(
            listOf(ScanResult("QR_CODE", "https://example.com"), ScanResult("EAN_13", "6901234567892")),
            ScanResultParser.parse(raw),
        )
    }

    @Test
    fun parse_ignoresUnexpectedShapes() {
        assertEquals(emptyList<ScanResult>(), ScanResultParser.parse(null))
        assertEquals(emptyList<ScanResult>(), ScanResultParser.parse("not a list"))
        assertEquals(emptyList<ScanResult>(), ScanResultParser.parse(listOf(mapOf("TYPE" to "QR_CODE"))))
    }

    @Test
    fun legacyString_matchesFirstVersionFormat() {
        assertEquals(
            "[{TYPE=QR_CODE, VALUE=123}]",
            ScanResultParser.legacyString(listOf(ScanResult("QR_CODE", "123"))),
        )
    }

    @Test
    fun scanOutcome_reportsCancelledScan() {
        val outcome = ScanOutcome(emptyList(), cancelled = true)
        assertFalse(outcome.ok)
        assertEquals("{\"ok\":false,\"cancelled\":true,\"error\":null,\"results\":[]}", outcome.toJson())
    }

    @Test
    fun fromLegacy_mapsTypeToCommand() {
        assertEquals(PrintCommand.QrCode("abc"), PrintCommand.fromLegacy("QR Code", "abc"))
        assertEquals(PrintCommand.QrCode("abc"), PrintCommand.fromLegacy("qrcode", "abc"))
        assertEquals(PrintCommand.Barcode("123"), PrintCommand.fromLegacy("CODE_128", "123"))
        assertNull(PrintCommand.fromLegacy("", "123"))
        assertNull(PrintCommand.fromLegacy("QR Code", " "))
    }

    @Test
    fun text_clampsSizeAndParsesAlignment() {
        val command = PrintCommand.text("Hi", 500, true, " Center ") as PrintCommand.Text
        assertEquals(PrintCommand.MAX_TEXT_SIZE, command.size)
        assertEquals(PrintCommand.Alignment.CENTER, command.align)
        assertTrue(command.bold)
        assertEquals(PrintCommand.Alignment.LEFT, PrintCommand.Alignment.parse("diagonal"))
        assertNull(PrintCommand.text("", 24, false, null))
    }

    @Test
    fun printOutcome_serializes() {
        assertEquals(
            "{\"ok\":false,\"code\":-1,\"message\":\"Printer not available\",\"printerStatus\":null}",
            PrintOutcome.failure(PrintOutcome.CODE_PRINTER_UNAVAILABLE, "Printer not available").toJson(),
        )
    }
}
