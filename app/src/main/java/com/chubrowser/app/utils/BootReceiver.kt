package com.chubrowser.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chubrowser.app.ChuBrowserApp

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 设备启动完成，可以在这里初始化一些后台任务
            Logger.d("BootReceiver", "Device boot completed")

            // 初始化应用
            val app = context.applicationContext as? ChuBrowserApp
            app?.let {
                // 可以在这里恢复未完成的下载等
            }
        }
    }
}
