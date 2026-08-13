package com.chubrowser.app.download

import android.app.DownloadManager as SystemDownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.chubrowser.app.utils.Logger

class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            SystemDownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                val downloadId = intent.getLongExtra(SystemDownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    Logger.d("DownloadComplete", "Download completed: $downloadId")
                    Toast.makeText(context, "下载完成", Toast.LENGTH_SHORT).show()
                }
            }
            SystemDownloadManager.ACTION_NOTIFICATION_CLICKED -> {
                Logger.d("DownloadComplete", "Download notification clicked")
            }
        }
    }
}
