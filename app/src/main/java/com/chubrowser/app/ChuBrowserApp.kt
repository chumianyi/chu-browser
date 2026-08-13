package com.chubrowser.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.chubrowser.app.core.AppDatabase
import com.chubrowser.app.core.SettingsManager
import com.chubrowser.app.adblock.AdBlockManager
import com.chubrowser.app.password.PasswordManager
import com.chubrowser.app.security.SecurityManager
import com.chubrowser.app.download.DownloadManager
import com.chubrowser.app.bookmark.BookmarkManager
import com.chubrowser.app.privacy.PrivacySpaceManager
import com.chubrowser.app.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chu_browser_settings")

class ChuBrowserApp : Application() {

    companion object {
        lateinit var instance: ChuBrowserApp
            private set

        lateinit var database: AppDatabase
            private set

        lateinit var settingsManager: SettingsManager
            private set

        lateinit var adBlockManager: AdBlockManager
            private set

        lateinit var passwordManager: PasswordManager
            private set

        lateinit var securityManager: SecurityManager
            private set

        lateinit var downloadManager: DownloadManager
            private set

        lateinit var bookmarkManager: BookmarkManager
            private set

        lateinit var privacySpaceManager: PrivacySpaceManager
            private set

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Logger.init()
        Logger.d("ChuBrowserApp", "Application starting...")

        // 初始化数据库
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "chu_browser.db"
        )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()

        Logger.d("ChuBrowserApp", "Database initialized")

        // 初始化设置管理器
        settingsManager = SettingsManager(this)

        // 应用主题
        applyTheme()

        // 初始化各功能模块
        adBlockManager = AdBlockManager(this)
        passwordManager = PasswordManager(this)
        securityManager = SecurityManager(this)
        downloadManager = DownloadManager(this)
        bookmarkManager = BookmarkManager(this)
        privacySpaceManager = PrivacySpaceManager(this)

        Logger.d("ChuBrowserApp", "All managers initialized")

        // 创建通知渠道
        createNotificationChannels()

        // 异步初始化广告拦截规则
        applicationScope.launch {
            adBlockManager.initialize()
            Logger.d("ChuBrowserApp", "AdBlock initialized")
        }
    }

    private fun applyTheme() {
        val themeMode = settingsManager.getThemeMode()
        val mode = when (themeMode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // 下载通知渠道
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "下载通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示文件下载进度和状态"
                setShowBadge(true)
            }

            // 安全警告渠道
            val securityChannel = NotificationChannel(
                CHANNEL_SECURITY,
                "安全警告",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "网站安全检测和威胁警告"
                setShowBadge(true)
            }

            // 通用通知渠道
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "通用通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "应用通用通知"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(downloadChannel, securityChannel, generalChannel)
            )
        }
    }

    fun getAppContext(): Context = applicationContext

    object Constants {
        const val CHANNEL_DOWNLOAD = "download_channel"
        const val CHANNEL_SECURITY = "security_channel"
        const val CHANNEL_GENERAL = "general_channel"
        const val DOWNLOAD_NOTIFICATION_ID = 1001
        const val SECURITY_NOTIFICATION_ID = 1002
    }
}
