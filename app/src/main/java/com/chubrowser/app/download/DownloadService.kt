package com.chubrowser.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chubrowser.app.ChuBrowserApp
import com.chubrowser.app.R

class DownloadService : Service() {

    companion object {
        private const val ACTION_START = "com.chubrowser.app.download.START"
        private const val ACTION_PAUSE = "com.chubrowser.app.download.PAUSE"
        private const val ACTION_CANCEL = "com.chubrowser.app.download.CANCEL"
        private const val EXTRA_DOWNLOAD_ID = "download_id"
        private const val EXTRA_URL = "url"
        private const val EXTRA_FILE_NAME = "file_name"
        private const val EXTRA_MIME_TYPE = "mime_type"
        private const val EXTRA_USER_AGENT = "user_agent"

        fun startDownload(
            context: Context,
            url: String,
            fileName: String,
            mimeType: String = "",
            userAgent: String = ""
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_MIME_TYPE, mimeType)
                putExtra(EXTRA_USER_AGENT, userAgent)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }

        fun cancelDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }

    private lateinit var downloadManager: DownloadManager

    override fun onCreate() {
        super.onCreate()
        downloadManager = DownloadManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: return START_NOT_STICKY
                val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: ""
                val userAgent = intent.getStringExtra(EXTRA_USER_AGENT) ?: ""

                startForeground(1, createDownloadNotification(fileName, 0))

                Thread {
                    try {
                        val request = DownloadManager.DownloadRequest(
                            url = url,
                            fileName = fileName,
                            mimeType = mimeType,
                            userAgent = userAgent
                        )
                        downloadManager.startDownload(request)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        stopSelf()
                    }
                }.start()
            }
            ACTION_PAUSE -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId > 0) {
                    downloadManager.pauseDownload(downloadId)
                }
            }
            ACTION_CANCEL -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId > 0) {
                    downloadManager.cancelDownload(downloadId)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ChuBrowserApp.Constants.CHANNEL_DOWNLOAD,
                "下载通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示文件下载进度和状态"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createDownloadNotification(fileName: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, ChuBrowserApp.Constants.CHANNEL_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName)
            .setContentText("下载中 $progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
