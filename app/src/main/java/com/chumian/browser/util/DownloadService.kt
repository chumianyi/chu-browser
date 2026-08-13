package com.chumian.browser.util

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chumian.browser.ChuBrowserApp
import com.chumian.browser.R
import com.chumian.browser.data.model.DownloadItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadService : Service() {

    private val client = OkHttpClient()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val activeDownloads = mutableMapOf<Long, Job>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val filename = intent.getStringExtra(EXTRA_FILENAME) ?: "download"
                startDownload(url, filename)
            }
            ACTION_CANCEL -> {
                val id = intent.getLongExtra(EXTRA_ID, -1)
                cancelDownload(id)
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(url: String, filename: String) {
        serviceScope.launch {
            val db = ChuBrowserApp.instance.database
            val downloadDir = getExternalFilesDir(null) ?: filesDir
            val file = File(downloadDir, sanitizeFilename(filename))
            val filePath = file.absolutePath

            val item = DownloadItem(
                filename = filename,
                url = url,
                filePath = filePath,
                status = DownloadItem.STATUS_DOWNLOADING
            )
            val id = db.downloadDao().insert(item)

            val notification = createNotification(id, filename, 0)
            startForeground(id.toInt(), notification)

            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body ?: throw Exception("Empty response")
                val totalSize = body.contentLength()

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var read: Int

                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    downloaded += read
                    val progress = if (totalSize > 0) (downloaded * 100 / totalSize).toInt() else 0
                    updateNotification(id, filename, progress)
                    db.downloadDao().update(
                        item.copy(
                            id = id,
                            totalSize = totalSize,
                            downloadedSize = downloaded,
                            status = DownloadItem.STATUS_DOWNLOADING
                        )
                    )
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                db.downloadDao().update(
                    item.copy(
                        id = id,
                        totalSize = totalSize,
                        downloadedSize = downloaded,
                        status = DownloadItem.STATUS_COMPLETED
                    )
                )
                updateNotification(id, filename, 100, true)
            } catch (e: Exception) {
                e.printStackTrace()
                db.downloadDao().update(
                    item.copy(id = id, status = DownloadItem.STATUS_FAILED)
                )
            } finally {
                activeDownloads.remove(id)
                if (activeDownloads.isEmpty()) stopSelf()
            }
        }
    }

    private fun cancelDownload(id: Long) {
        activeDownloads[id]?.cancel()
        activeDownloads.remove(id)
    }

    private fun sanitizeFilename(filename: String): String {
        return filename.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun createNotification(id: Long, filename: String, progress: Int, completed: Boolean = false): Notification {
        val builder = NotificationCompat.Builder(this, ChuBrowserApp.CHANNEL_DOWNLOAD)
            .setContentTitle(filename)
            .setContentText(if (completed) "下载完成" else "下载中... $progress%")
            .setSmallIcon(R.drawable.ic_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(!completed)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }

    private fun updateNotification(id: Long, filename: String, progress: Int, completed: Boolean = false) {
        val notification = createNotification(id, filename, progress, completed)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(id.toInt(), notification)
    }

    companion object {
        const val ACTION_START = "com.chumian.browser.START_DOWNLOAD"
        const val ACTION_CANCEL = "com.chumian.browser.CANCEL_DOWNLOAD"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILENAME = "extra_filename"
        const val EXTRA_ID = "extra_id"
    }
}
