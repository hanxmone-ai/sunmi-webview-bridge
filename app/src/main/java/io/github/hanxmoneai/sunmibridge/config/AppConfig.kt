package io.github.hanxmoneai.sunmibridge.config

import android.content.Context

/** URLs entered on the setup screen, kept across restarts. */
class AppConfig(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var startUrl: String
        get() = prefs.getString(KEY_START_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_START_URL, value).apply()

    /** Optional; the "Home" button falls back to [startUrl] when empty. */
    var homeUrl: String
        get() = prefs.getString(KEY_HOME_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_HOME_URL, value).apply()

    private companion object {
        const val PREFS_NAME = "bridge_config"
        const val KEY_START_URL = "start_url"
        const val KEY_HOME_URL = "home_url"
    }
}
