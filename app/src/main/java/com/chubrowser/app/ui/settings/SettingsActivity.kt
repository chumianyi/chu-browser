package com.chubrowser.app.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.chubrowser.app.ChuBrowserApp
import com.chubrowser.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        setupSettings()
    }

    private fun setupSettings() {
        // 搜索引擎
        findViewById<android.view.View>(R.id.setting_search_engine).setOnClickListener {
            showSearchEngineDialog()
        }

        // 广告拦截
        findViewById<MaterialSwitch>(R.id.switch_adblock).apply {
            isChecked = ChuBrowserApp.settingsManager.isAdBlockEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setAdBlockEnabled(isChecked)
                Toast.makeText(this@SettingsActivity,
                    if (isChecked) "广告拦截已开启" else "广告拦截已关闭",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // 安全检测
        findViewById<MaterialSwitch>(R.id.switch_security).apply {
            isChecked = ChuBrowserApp.settingsManager.isSecurityCheckEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setSecurityCheckEnabled(isChecked)
            }
        }

        // 网页检测
        findViewById<MaterialSwitch>(R.id.switch_web_detector).apply {
            isChecked = ChuBrowserApp.settingsManager.isWebDetectorEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setWebDetectorEnabled(isChecked)
            }
        }

        // 密码保存
        findViewById<MaterialSwitch>(R.id.switch_password_save).apply {
            isChecked = ChuBrowserApp.settingsManager.isPasswordSaveEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setPasswordSaveEnabled(isChecked)
            }
        }

        // 自动填充
        findViewById<MaterialSwitch>(R.id.switch_autofill).apply {
            isChecked = ChuBrowserApp.settingsManager.isAutoFillEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setAutoFillEnabled(isChecked)
            }
        }

        // 验证码自动识别
        findViewById<MaterialSwitch>(R.id.switch_captcha).apply {
            isChecked = ChuBrowserApp.settingsManager.isCaptchaAutoFillEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setCaptchaAutoFillEnabled(isChecked)
            }
        }

        // 隐私隔离
        findViewById<MaterialSwitch>(R.id.switch_privacy_isolation).apply {
            isChecked = ChuBrowserApp.settingsManager.isPrivacyIsolationEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setPrivacyIsolationEnabled(isChecked)
            }
        }

        // Do Not Track
        findViewById<MaterialSwitch>(R.id.switch_dnt).apply {
            isChecked = ChuBrowserApp.settingsManager.isDoNotTrackEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setDoNotTrackEnabled(isChecked)
            }
        }

        // 阻止追踪器
        findViewById<MaterialSwitch>(R.id.switch_block_trackers).apply {
            isChecked = ChuBrowserApp.settingsManager.isBlockTrackersEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setBlockTrackersEnabled(isChecked)
            }
        }

        // 阻止弹窗
        findViewById<MaterialSwitch>(R.id.switch_block_popups).apply {
            isChecked = ChuBrowserApp.settingsManager.isBlockPopupsEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setBlockPopupsEnabled(isChecked)
            }
        }

        // 开发者工具
        findViewById<MaterialSwitch>(R.id.switch_devtools).apply {
            isChecked = ChuBrowserApp.settingsManager.isDevToolsEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setDevToolsEnabled(isChecked)
            }
        }

        // 远程调试
        findViewById<MaterialSwitch>(R.id.switch_remote_debugging).apply {
            isChecked = ChuBrowserApp.settingsManager.isRemoteDebuggingEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setRemoteDebuggingEnabled(isChecked)
            }
        }

        // 下载确认
        findViewById<MaterialSwitch>(R.id.switch_download_confirm).apply {
            isChecked = ChuBrowserApp.settingsManager.isDownloadConfirmEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                ChuBrowserApp.settingsManager.setDownloadConfirmEnabled(isChecked)
            }
        }

        // 主题
        findViewById<android.view.View>(R.id.setting_theme).setOnClickListener {
            showThemeDialog()
        }

        // 清除缓存
        findViewById<android.view.View>(R.id.setting_clear_cache).setOnClickListener {
            clearCache()
        }

        // 清除历史
        findViewById<android.view.View>(R.id.setting_clear_history).setOnClickListener {
            clearHistory()
        }

        // 清除Cookie
        findViewById<android.view.View>(R.id.setting_clear_cookies).setOnClickListener {
            clearCookies()
        }

        // 关于
        findViewById<android.view.View>(R.id.setting_about).setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showSearchEngineDialog() {
        val engines = arrayOf("Bing", "Google", "百度", "DuckDuckGo", "Yahoo")
        val engineKeys = arrayOf("bing", "google", "baidu", "duckduckgo", "yahoo")
        val current = ChuBrowserApp.settingsManager.getSearchEngine()
        val checkedIndex = engineKeys.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle("选择搜索引擎")
            .setSingleChoiceItems(engines, checkedIndex) { dialog, which ->
                ChuBrowserApp.settingsManager.setSearchEngine(engineKeys[which])
                Toast.makeText(this, "已切换到${engines[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("跟随系统", "浅色", "深色")
        val themeKeys = arrayOf("system", "light", "dark")
        val current = ChuBrowserApp.settingsManager.getThemeMode()
        val checkedIndex = themeKeys.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle("选择主题")
            .setSingleChoiceItems(themes, checkedIndex) { dialog, which ->
                ChuBrowserApp.settingsManager.setThemeMode(themeKeys[which])
                Toast.makeText(this, "主题已切换", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                recreate()
            }
            .show()
    }

    private fun clearCache() {
        MaterialAlertDialogBuilder(this)
            .setTitle("清除缓存")
            .setMessage("确定要清除所有缓存数据吗？")
            .setPositiveButton("确定") { _, _ ->
                cacheDir.deleteRecursively()
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearHistory() {
        MaterialAlertDialogBuilder(this)
            .setTitle("清除历史记录")
            .setMessage("确定要清除所有浏览历史吗？此操作不可恢复。")
            .setPositiveButton("确定") { _, _ ->
                Thread {
                    ChuBrowserApp.database.historyDao().deleteAll()
                    runOnUiThread {
                        Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearCookies() {
        MaterialAlertDialogBuilder(this)
            .setTitle("清除Cookie")
            .setMessage("确定要清除所有Cookie吗？这将使您退出所有登录的网站。")
            .setPositiveButton("确定") { _, _ ->
                Toast.makeText(this, "Cookie已清除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("关于Chu浏览器")
            .setMessage(
                "Chu浏览器 v1.0.0\n\n" +
                "基于Mozilla GeckoView (Firefox内核)\n\n" +
                "开源协议: GPL-3.0\n\n" +
                "功能特性：\n" +
                "• 广告拦截\n" +
                "• 安全检测\n" +
                "• 密码管理\n" +
                "• 隐私隔离\n" +
                "• 开发者工具\n" +
                "• 多标签浏览\n" +
                "• 下载管理\n\n" +
                "© 2026 Chu Browser"
            )
            .setPositiveButton("确定", null)
            .show()
    }
}
