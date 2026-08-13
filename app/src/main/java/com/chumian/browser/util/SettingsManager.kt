package com.chumian.browser.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_ADBLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_ADBLOCK, value).apply()

    var securityEnabled: Boolean
        get() = prefs.getBoolean(KEY_SECURITY, true)
        set(value) = prefs.edit().putBoolean(KEY_SECURITY, value).apply()

    var autofillEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOFILL, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOFILL, value).apply()

    var captchaAutoEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAPTCHA, false)
        set(value) = prefs.edit().putBoolean(KEY_CAPTCHA, value).apply()

    var privacyModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVACY, value).apply()

    var devToolsEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEVTOOLS, false)
        set(value) = prefs.edit().putBoolean(KEY_DEVTOOLS, value).apply()

    fun isDevToolsEnabled(): Boolean = devToolsEnabled

    fun getHomepage(): String = prefs.getString(KEY_HOMEPAGE, "https://www.bing.com") ?: "https://www.bing.com"

    fun setHomepage(value: String) = prefs.edit().putString(KEY_HOMEPAGE, value).apply()

    var searchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "bing") ?: "bing"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) {
            prefs.edit().putInt(KEY_THEME, value).apply()
            AppCompatDelegate.setDefaultNightMode(value)
        }

    fun getSearchUrl(query: String): String {
        return when (searchEngine) {
            "google" -> "https://www.google.com/search?q=$query"
            "baidu" -> "https://www.baidu.com/s?wd=$query"
            "duckduckgo" -> "https://duckduckgo.com/?q=$query"
            else -> "https://www.bing.com/search?q=$query"
        }
    }

    companion object {
        private const val PREFS_NAME = "chu_browser_settings"
        private const val KEY_ADBLOCK = "adblock_enabled"
        private const val KEY_SECURITY = "security_enabled"
        private const val KEY_AUTOFILL = "autofill_enabled"
        private const val KEY_CAPTCHA = "captcha_auto_enabled"
        private const val KEY_PRIVACY = "privacy_mode_enabled"
        private const val KEY_DEVTOOLS = "devtools_enabled"
        private const val KEY_HOMEPAGE = "homepage"
        private const val KEY_SEARCH_ENGINE = "search_engine"
        private const val KEY_THEME = "theme_mode"
    }
}
