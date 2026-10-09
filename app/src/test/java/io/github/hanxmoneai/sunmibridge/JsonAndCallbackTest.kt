package io.github.hanxmoneai.sunmibridge

import io.github.hanxmoneai.sunmibridge.bridge.JsCallback
import io.github.hanxmoneai.sunmibridge.bridge.Json
import io.github.hanxmoneai.sunmibridge.bridge.RawJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JsonAndCallbackTest {

    @Test
    fun quote_escapesCharactersThatWouldBreakTheScript() {
        assertEquals("\"a\\\"b\"", Json.quote("a\"b"))
        assertEquals("\"a\\\\b\"", Json.quote("a\\b"))
        assertEquals("\"line1\\nline2\"", Json.quote("line1\nline2"))
        assertEquals("\"\\u2028\"", Json.quote("\u2028"))
        assertEquals("\"\\u0001\"", Json.quote("\u0001"))
        assertEquals("\"\\u003c/script>\"", Json.quote("</script>"))
        assertEquals("null", Json.quote(null))
    }

    @Test
    fun quote_keepsQuotesFromInjectingCode() {
        val hostile = "abc');alert(1);//"
        val quoted = Json.quote(hostile)
        assertEquals("\"abc');alert(1);//\"", quoted)
        // Single quotes are harmless inside a double-quoted JSON string; no unescaped double quote appears.
        assertFalse(quoted.substring(1, quoted.length - 1).contains("\""))
    }

    @Test
    fun obj_serializesNestedValues() {
        val json = Json.obj(
            "ok" to true,
            "code" to 0,
            "message" to "done",
            "status" to null,
            "items" to listOf(RawJson(Json.obj("v" to "1")), RawJson(Json.obj("v" to "2"))),
        )
        assertEquals(
            "{\"ok\":true,\"code\":0,\"message\":\"done\",\"status\":null,\"items\":[{\"v\":\"1\"},{\"v\":\"2\"}]}",
            json,
        )
    }

    @Test
    fun invoke_callsTheFunctionOnlyWhenDefined() {
        assertEquals(
            "(function(){var f=window['onPosPrintResult'];if(typeof f==='function'){f({\"ok\":true});}})();",
            JsCallback.invoke("onPosPrintResult", "{\"ok\":true}"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invoke_rejectsNonIdentifierNames() {
        JsCallback.invoke("alert(1);x", "null")
    }
}
