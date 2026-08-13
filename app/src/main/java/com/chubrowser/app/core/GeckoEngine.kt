package com.chubrowser.app.core

import android.content.Context
import android.util.Log
import com.chubrowser.app.ChuBrowserApp
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.ContentBlocking

class GeckoEngine(private val context: Context) {

    companion object {
        private const val TAG = "GeckoEngine"
        private var instance: GeckoEngine? = null

        fun getInstance(context: Context): GeckoEngine {
            if (instance == null) {
                instance = GeckoEngine(context.applicationContext)
            }
            return instance!!
        }
    }

    private var runtime: GeckoRuntime? = null

    fun getRuntime(): GeckoRuntime {
        if (runtime == null) {
            initializeRuntime()
        }
        return runtime!!
    }

    private fun initializeRuntime() {
        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .domStorageEnabled(true)
            .webFontsEnabled(true)
            .hardwareAccelerationEnabled(true)
            .useTrackingProtection(ChuBrowserApp.settingsManager.isBlockTrackersEnabled())
            .remoteDebuggingEnabled(ChuBrowserApp.settingsManager.isRemoteDebuggingEnabled())
            .consoleOutput(true)
            .build()

        runtime = GeckoRuntime.create(context, settings)

        // 配置内容拦截（广告拦截）
        configureContentBlocking()

        Log.d(TAG, "GeckoRuntime initialized")
    }

    private fun configureContentBlocking() {
        val runtime = runtime ?: return

        if (ChuBrowserApp.settingsManager.isAdBlockEnabled()) {
            try {
                // 使用GeckoView内置的安全浏览
                val safeBrowsing = ContentBlocking.SafeBrowsingProvider
                    .fromSafeBrowsing()
                    .build()

                runtime.settings.contentBlocking = ContentBlocking.Settings.Builder()
                    .safeBrowsing(safeBrowsing)
                    .build()

                Log.d(TAG, "Content blocking enabled")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to configure content blocking: ${e.message}")
            }
        }
    }

    fun updateSettings() {
        val runtime = runtime ?: return
        val settings = runtime.settings

        settings.useTrackingProtection = ChuBrowserApp.settingsManager.isBlockTrackersEnabled()
        settings.remoteDebuggingEnabled = ChuBrowserApp.settingsManager.isRemoteDebuggingEnabled()

        configureContentBlocking()
    }

    fun setUserAgent(userAgent: String) {
        runtime?.settings?.userAgent = userAgent
    }

    fun setAcceptLanguage(language: String) {
        runtime?.settings?.acceptLanguage = language
    }

    fun clearCache() {
        try {
            runtime?.clearCache()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear cache: ${e.message}")
        }
    }

    fun clearCookies() {
        try {
            runtime?.cookieStorage?.clear()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear cookies: ${e.message}")
        }
    }

    fun clearHistory() {
        try {
            runtime?.historyStorage?.clear()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear history: ${e.message}")
        }
    }

    fun clearAllData() {
        clearCache()
        clearCookies()
        clearHistory()
    }

    fun getCookieStorage() = runtime?.cookieStorage

    fun getHistoryStorage() = runtime?.historyStorage

    fun getWebExtensionController() = runtime?.webExtensionController

    fun shutdown() {
        try {
            runtime?.shutdown()
            runtime = null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to shutdown runtime: ${e.message}")
        }
    }
}
