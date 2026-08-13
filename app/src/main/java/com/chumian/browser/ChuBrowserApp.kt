package com.chumian.browser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.chumian.browser.data.local.AppDatabase
import com.chumian.browser.util.SettingsManager

class ChuBrowserApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        settingsManager = SettingsManager(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "下载通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "文件下载进度通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: ChuBrowserApp
            private set
        const val CHANNEL_DOWNLOAD = "download_channel"
    }
}
