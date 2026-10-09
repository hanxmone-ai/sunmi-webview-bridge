package io.github.hanxmoneai.sunmibridge.bridge

/** Builds the scripts used to call back into the web page. */
object JsCallback {

    private val IDENTIFIER = Regex("[A-Za-z_\$][A-Za-z0-9_\$]*")

    /**
     * Returns a script that calls `window[function](argumentJson)` only when the page
     * defined that function, so pages that ignore a callback do not throw.
     *
     * @param argumentJson an already-serialized JSON value (see [Json]).
     */
    fun invoke(function: String, argumentJson: String): String {
        require(IDENTIFIER.matches(function)) { "Invalid JavaScript function name: $function" }
        return "(function(){var f=window['$function'];" +
            "if(typeof f==='function'){f($argumentJson);}})();"
    }
}
