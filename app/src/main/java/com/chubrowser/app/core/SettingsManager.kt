package com.chubrowser.app.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chubrowser.app.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class SettingsManager(private val context: Context) {

    companion object {
        // 搜索引擎
        private val KEY_SEARCH_ENGINE = stringPreferencesKey("search_engine")
        private val KEY_CUSTOM_SEARCH_URL = stringPreferencesKey("custom_search_url")

        // 主页
        private val KEY_HOMEPAGE = stringPreferencesKey("homepage")
        private val KEY_HOMEPAGE_TYPE = stringPreferencesKey("homepage_type")

        // 广告拦截
        private val KEY_ADBLOCK_ENABLED = booleanPreferencesKey("adblock_enabled")
        private val KEY_ADBLOCK_WHITELIST = stringPreferencesKey("adblock_whitelist")

        // 安全
        private val KEY_SECURITY_CHECK_ENABLED = booleanPreferencesKey("security_check_enabled")
        private val KEY_WEB_DETECTOR_ENABLED = booleanPreferencesKey("web_detector_enabled")
        private val KEY_HTTPS_ONLY = booleanPreferencesKey("https_only")

        // 密码
        private val KEY_PASSWORD_SAVE_ENABLED = booleanPreferencesKey("password_save_enabled")
        private val KEY_AUTO_FILL_ENABLED = booleanPreferencesKey("auto_fill_enabled")
        private val KEY_MASTER_PASSWORD_SET = booleanPreferencesKey("master_password_set")
        private val KEY_MASTER_PASSWORD_HASH = stringPreferencesKey("master_password_hash")

        // 验证码
        private val KEY_CAPTCHA_AUTO_FILL = booleanPreferencesKey("captcha_auto_fill")

        // 隐私
        private val KEY_PRIVACY_ISOLATION = booleanPreferencesKey("privacy_isolation")
        private val KEY_DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
        private val KEY_BLOCK_TRACKERS = booleanPreferencesKey("block_trackers")
        private val KEY_BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        private val KEY_BLOCK_POPUPS = booleanPreferencesKey("block_popups")
        private val KEY_FINGERPRINT_PROTECTION = booleanPreferencesKey("fingerprint_protection")
        private val KEY_WEBRTC_PROTECTION = booleanPreferencesKey("webrtc_protection")

        // 开发者工具
        private val KEY_DEVTOOLS_ENABLED = booleanPreferencesKey("devtools_enabled")
        private val KEY_REMOTE_DEBUGGING = booleanPreferencesKey("remote_debugging")

        // 外观
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")

        // 下载
        private val KEY_DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
        private val KEY_DOWNLOAD_CONFIRM = booleanPreferencesKey("download_confirm")

        // User Agent
        private val KEY_USER_AGENT = stringPreferencesKey("user_agent")
        private val KEY_DESKTOP_MODE = booleanPreferencesKey("desktop_mode")

        // 其他
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_LAST_VERSION = intPreferencesKey("last_version")
    }

    // 搜索引擎
    fun getSearchEngine(): String = runBlocking {
        context.dataStore.data.map { it[KEY_SEARCH_ENGINE] ?: "bing" }.first()
    }

    suspend fun setSearchEngine(engine: String) {
        context.dataStore.edit { it[KEY_SEARCH_ENGINE] = engine }
    }

    fun getSearchUrl(query: String): String {
        return when (getSearchEngine()) {
            "bing" -> "https://www.bing.com/search?q=$query"
            "google" -> "https://www.google.com/search?q=$query"
            "baidu" -> "https://www.baidu.com/s?wd=$query"
            "duckduckgo" -> "https://duckduckgo.com/?q=$query"
            "yahoo" -> "https://search.yahoo.com/search?p=$query"
            "custom" -> {
                val url = runBlocking {
                    context.dataStore.data.map { it[KEY_CUSTOM_SEARCH_URL] ?: "" }.first()
                }
                if (url.isNotEmpty()) url.replace("%s", query) else "https://www.bing.com/search?q=$query"
            }
            else -> "https://www.bing.com/search?q=$query"
        }
    }

    // 主页
    fun getHomepage(): String = runBlocking {
        context.dataStore.data.map { it[KEY_HOMEPAGE] ?: "https://www.bing.com" }.first()
    }

    suspend fun setHomepage(url: String) {
        context.dataStore.edit { it[KEY_HOMEPAGE] = url }
    }

    fun getHomepageType(): String = runBlocking {
        context.dataStore.data.map { it[KEY_HOMEPAGE_TYPE] ?: "default" }.first()
    }

    suspend fun setHomepageType(type: String) {
        context.dataStore.edit { it[KEY_HOMEPAGE_TYPE] = type }
    }

    // 广告拦截
    fun isAdBlockEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_ADBLOCK_ENABLED] ?: true }.first()
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ADBLOCK_ENABLED] = enabled }
    }

    // 安全检测
    fun isSecurityCheckEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_SECURITY_CHECK_ENABLED] ?: true }.first()
    }

    suspend fun setSecurityCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SECURITY_CHECK_ENABLED] = enabled }
    }

    fun isWebDetectorEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_WEB_DETECTOR_ENABLED] ?: true }.first()
    }

    suspend fun setWebDetectorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WEB_DETECTOR_ENABLED] = enabled }
    }

    fun isHttpsOnly(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_HTTPS_ONLY] ?: false }.first()
    }

    suspend fun setHttpsOnly(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HTTPS_ONLY] = enabled }
    }

    // 密码
    fun isPasswordSaveEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_PASSWORD_SAVE_ENABLED] ?: true }.first()
    }

    suspend fun setPasswordSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PASSWORD_SAVE_ENABLED] = enabled }
    }

    fun isAutoFillEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_AUTO_FILL_ENABLED] ?: true }.first()
    }

    suspend fun setAutoFillEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_FILL_ENABLED] = enabled }
    }

    fun isMasterPasswordSet(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_MASTER_PASSWORD_SET] ?: false }.first()
    }

    suspend fun setMasterPassword(hash: String) {
        context.dataStore.edit {
            it[KEY_MASTER_PASSWORD_SET] = true
            it[KEY_MASTER_PASSWORD_HASH] = hash
        }
    }

    fun getMasterPasswordHash(): String = runBlocking {
        context.dataStore.data.map { it[KEY_MASTER_PASSWORD_HASH] ?: "" }.first()
    }

    // 验证码
    fun isCaptchaAutoFillEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_CAPTCHA_AUTO_FILL] ?: true }.first()
    }

    suspend fun setCaptchaAutoFillEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CAPTCHA_AUTO_FILL] = enabled }
    }

    // 隐私
    fun isPrivacyIsolationEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_PRIVACY_ISOLATION] ?: true }.first()
    }

    suspend fun setPrivacyIsolationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PRIVACY_ISOLATION] = enabled }
    }

    fun isDoNotTrackEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_DO_NOT_TRACK] ?: true }.first()
    }

    suspend fun setDoNotTrackEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DO_NOT_TRACK] = enabled }
    }

    fun isBlockTrackersEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_BLOCK_TRACKERS] ?: true }.first()
    }

    suspend fun setBlockTrackersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BLOCK_TRACKERS] = enabled }
    }

    fun isBlockThirdPartyCookiesEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_BLOCK_THIRD_PARTY_COOKIES] ?: false }.first()
    }

    suspend fun setBlockThirdPartyCookiesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BLOCK_THIRD_PARTY_COOKIES] = enabled }
    }

    fun isBlockPopupsEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_BLOCK_POPUPS] ?: true }.first()
    }

    suspend fun setBlockPopupsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BLOCK_POPUPS] = enabled }
    }

    fun isFingerprintProtectionEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_FINGERPRINT_PROTECTION] ?: true }.first()
    }

    suspend fun setFingerprintProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FINGERPRINT_PROTECTION] = enabled }
    }

    fun isWebRtcProtectionEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_WEBRTC_PROTECTION] ?: true }.first()
    }

    suspend fun setWebRtcProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WEBRTC_PROTECTION] = enabled }
    }

    // 开发者工具
    fun isDevToolsEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_DEVTOOLS_ENABLED] ?: false }.first()
    }

    suspend fun setDevToolsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DEVTOOLS_ENABLED] = enabled }
    }

    fun isRemoteDebuggingEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_REMOTE_DEBUGGING] ?: false }.first()
    }

    suspend fun setRemoteDebuggingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_REMOTE_DEBUGGING] = enabled }
    }

    // 外观
    fun getThemeMode(): String = runBlocking {
        context.dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }.first()
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    fun getFontSize(): String = runBlocking {
        context.dataStore.data.map { it[KEY_FONT_SIZE] ?: "medium" }.first()
    }

    suspend fun setFontSize(size: String) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    fun getFontScale(): Float = when (getFontSize()) {
        "small" -> 0.85f
        "large" -> 1.15f
        "huge" -> 1.3f
        else -> 1.0f
    }

    // 下载
    fun getDownloadLocation(): String = runBlocking {
        context.dataStore.data.map {
            it[KEY_DOWNLOAD_LOCATION] ?: "${context.getExternalFilesDir(null)?.absolutePath}/Downloads"
        }.first()
    }

    suspend fun setDownloadLocation(path: String) {
        context.dataStore.edit { it[KEY_DOWNLOAD_LOCATION] = path }
    }

    fun isDownloadConfirmEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_DOWNLOAD_CONFIRM] ?: true }.first()
    }

    suspend fun setDownloadConfirmEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DOWNLOAD_CONFIRM] = enabled }
    }

    // User Agent
    fun getUserAgent(): String = runBlocking {
        context.dataStore.data.map {
            it[KEY_USER_AGENT] ?: "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }.first()
    }

    suspend fun setUserAgent(ua: String) {
        context.dataStore.edit { it[KEY_USER_AGENT] = ua }
    }

    fun isDesktopMode(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_DESKTOP_MODE] ?: false }.first()
    }

    suspend fun setDesktopMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DESKTOP_MODE] = enabled }
    }

    // 首次启动
    fun isFirstLaunch(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_FIRST_LAUNCH] ?: true }.first()
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { it[KEY_FIRST_LAUNCH] = false }
    }

    // 监听设置变化
    fun <T> getSettingFlow(key: androidx.datastore.preferences.core.Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { it[key] ?: defaultValue }
    }
}
