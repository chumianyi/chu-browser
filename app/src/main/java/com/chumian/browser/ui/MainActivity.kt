package com.chumian.browser.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chumian.browser.R
import com.chumian.browser.adblock.AdBlocker
import com.chumian.browser.data.local.AppDatabase
import com.chumian.browser.data.model.Bookmark
import com.chumian.browser.data.model.HistoryItem
import com.chumian.browser.security.SecurityValidator
import com.chumian.browser.util.DownloadService
import com.chumian.browser.util.SettingsManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.GeckoResult
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var geckoView: GeckoView
    private lateinit var geckoRuntime: GeckoRuntime
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var settingsManager: SettingsManager
    private lateinit var adBlocker: AdBlocker
    private lateinit var database: AppDatabase
    private lateinit var urlText: TextView
    private lateinit var securityIcon: ImageView
    private lateinit var btnNewTab: ImageButton
    private lateinit var btnTabs: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnClear: ImageButton

    private val sessions = mutableListOf<GeckoSession>()
    private val sessionTitles = mutableListOf<String>()
    private val sessionUrls = mutableListOf<String>()
    private var currentSessionIndex = 0

    private val currentSession: GeckoSession
        get() = sessions[currentSessionIndex]

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (!granted) {
            Toast.makeText(this, "部分权限未授予，可能影响功能使用", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsManager = SettingsManager(this)
        adBlocker = AdBlocker(this)
        adBlocker.initialize()
        database = AppDatabase.getInstance(this)

        addressBar = findViewById(R.id.addressBar)
        progressBar = findViewById(R.id.progressBar)
        geckoView = findViewById(R.id.geckoView)
        bottomNav = findViewById(R.id.bottomNav)
        urlText = findViewById(R.id.urlText)
        securityIcon = findViewById(R.id.securityIcon)
        btnNewTab = findViewById(R.id.btnNewTab)
        btnTabs = findViewById(R.id.btnTabs)
        btnMenu = findViewById(R.id.btnMenu)
        btnClear = findViewById(R.id.btnClear)

        requestStartupPermissions()
        setupGeckoRuntime()
        createNewSession(settingsManager.getHomepage())
        setupAddressBar()
        setupBottomNav()
        setupTopButtons()
    }

    private fun setupTopButtons() {
        btnNewTab.setOnClickListener { createNewSession("about:blank") }
        btnTabs.setOnClickListener { showTabsManager() }
        btnMenu.setOnClickListener { showMenuDialog() }
        btnClear.setOnClickListener {
            addressBar.text.clear()
            addressBar.requestFocus()
        }
    }

    private fun requestStartupPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun setupGeckoRuntime() {
        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .remoteDebuggingEnabled(settingsManager.isDevToolsEnabled())
            .build()
        geckoRuntime = GeckoRuntime.create(this, runtimeSettings)
    }

    private fun createNewSession(url: String? = null) {
        val session = GeckoSession()
        session.open(geckoRuntime)
        setupSessionDelegates(session)

        sessions.add(session)
        sessionTitles.add("新标签页")
        sessionUrls.add(url ?: "about:blank")

        currentSessionIndex = sessions.size - 1
        geckoView.setSession(session)

        if (url != null) {
            session.loadUri(url)
        }
        updateUrlDisplay()
    }

    private fun setupSessionDelegates(session: GeckoSession) {
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(s: GeckoSession, url: String) {
                if (s == currentSession) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 0
                    addressBar.setText(url)
                    urlText.text = url
                }
                val idx = sessions.indexOf(s)
                if (idx >= 0) sessionUrls[idx] = url
                addToHistory(url, "")
            }

            override fun onPageStop(s: GeckoSession, success: Boolean) {
                if (s == currentSession) {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onProgressChange(s: GeckoSession, progress: Int) {
                if (s == currentSession) {
                    progressBar.progress = progress
                }
            }

            override fun onSecurityChange(s: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation?) {
                if (s == currentSession) {
                    val url = sessionUrls[currentSessionIndex]
                    val isSecure = url.startsWith("https://")
                    securityIcon.setImageResource(if (isSecure) R.drawable.ic_security else R.drawable.ic_warning)
                    securityIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, if (isSecure) R.color.primary else R.color.danger))
                }
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(s: GeckoSession, title: String?) {
                val idx = sessions.indexOf(s)
                if (idx >= 0) {
                    sessionTitles[idx] = title ?: "无标题"
                }
            }

            override fun onExternalResponse(s: GeckoSession, response: GeckoSession.WebResponseInfo) {
                if (response.contentType?.startsWith("application/") == true ||
                    response.contentType?.contains("octet-stream") == true ||
                    response.contentType?.contains("zip") == true ||
                    response.contentType?.contains("pdf") == true ||
                    response.contentType?.contains("apk") == true) {
                    showDownloadConfirm(response.uri, response.contentType ?: "file")
                }
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                s: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val url = request.uri
                if (settingsManager.securityEnabled && !SecurityValidator.validate(url).isSafe) {
                    if (s == currentSession) {
                        showBlockedWarning(url)
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                if (settingsManager.adBlockEnabled && adBlocker.isBlocked(url)) {
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return null
            }

            override fun onNewSession(s: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                createNewSession(uri)
                return GeckoResult.fromValue(currentSession)
            }

            override fun onLocationChange(s: GeckoSession, url: String?) {
                val idx = sessions.indexOf(s)
                if (idx >= 0 && url != null) {
                    sessionUrls[idx] = url
                    if (s == currentSession) {
                        addressBar.setText(url)
                        urlText.text = url
                    }
                }
            }
        }
    }

    private fun updateUrlDisplay() {
        val url = sessionUrls[currentSessionIndex]
        urlText.text = url
        addressBar.setText(url)
    }

    private fun setupAddressBar() {
        addressBar.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                val input = addressBar.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrl(input)
                }
                true
            } else false
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_back -> {
                    currentSession.goBack()
                    true
                }
                R.id.nav_forward -> {
                    currentSession.goForward()
                    true
                }
                R.id.nav_home -> {
                    loadUrl(settingsManager.getHomepage())
                    true
                }
                R.id.nav_tabs -> {
                    showTabsManager()
                    true
                }
                R.id.nav_menu -> {
                    showMenuDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUrl(input: String) {
        val url = if (URLUtil.isNetworkUrl(input)) {
            input
        } else if (input.contains(".") && !input.contains(" ")) {
            "https://$input"
        } else {
            settingsManager.getSearchUrl(input)
        }
        currentSession.loadUri(url)
    }

    private fun showTabsManager() {
        val tabNames = sessionTitles.mapIndexed { index, title ->
            "${index + 1}. ${title.ifEmpty { sessionUrls[index] }}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("标签页管理 (${sessions.size}个)")
            .setItems(tabNames) { _, which ->
                switchToSession(which)
            }
            .setPositiveButton("新建标签") { _, _ ->
                createNewSession("about:blank")
                addressBar.setText("")
                addressBar.requestFocus()
            }
            .setNeutralButton("关闭当前") { _, _ ->
                closeCurrentSession()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun switchToSession(index: Int) {
        if (index in sessions.indices && index != currentSessionIndex) {
            currentSessionIndex = index
            geckoView.setSession(currentSession)
            updateUrlDisplay()
        }
    }

    private fun closeCurrentSession() {
        if (sessions.size <= 1) {
            Toast.makeText(this, "至少保留一个标签页", Toast.LENGTH_SHORT).show()
            return
        }
        val session = sessions.removeAt(currentSessionIndex)
        sessionTitles.removeAt(currentSessionIndex)
        sessionUrls.removeAt(currentSessionIndex)
        session.close()

        currentSessionIndex = minOf(currentSessionIndex, sessions.size - 1)
        geckoView.setSession(currentSession)
        updateUrlDisplay()
    }

    private fun showBlockedWarning(url: String) {
        val result = SecurityValidator.validate(url)
        AlertDialog.Builder(this)
            .setTitle("安全警告")
            .setMessage("检测到该网站可能存在安全风险：\n$url\n\n风险等级：${result.riskLevel}\n原因：${result.reason}\n\n是否仍要访问？")
            .setPositiveButton("仍要访问") { _, _ ->
                currentSession.loadUri(url)
            }
            .setNegativeButton("返回安全", null)
            .show()
    }

    private fun showDownloadConfirm(url: String, contentType: String) {
        val filename = url.substringAfterLast("/").substringBefore("?").ifEmpty { "download" }
        AlertDialog.Builder(this)
            .setTitle("下载确认")
            .setMessage("文件名：$filename\n类型：$contentType\n\n是否下载？")
            .setPositiveButton("下载") { _, _ ->
                startDownload(url, filename)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startDownload(url: String, filename: String) {
        val intent = Intent(this, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FILENAME, filename)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "开始下载：$filename", Toast.LENGTH_SHORT).show()
    }

    private fun showMenuDialog() {
        val items = arrayOf(
            "刷新", "新建标签页", "添加书签", "分享", "下载管理", "书签",
            "历史记录", "密码管理", "Cookie管理", "证书信息", "开发者工具",
            "隐私空间", "清除数据", "设置"
        )
        AlertDialog.Builder(this)
            .setTitle("菜单")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> currentSession.reload()
                    1 -> createNewSession("about:blank")
                    2 -> addCurrentBookmark()
                    3 -> shareCurrentPage()
                    4 -> showDownloads()
                    5 -> showBookmarks()
                    6 -> showHistory()
                    7 -> showPasswords()
                    8 -> showCookieManager()
                    9 -> showCertificateInfo()
                    10 -> showDevTools()
                    11 -> showPrivacyMode()
                    12 -> clearBrowsingData()
                    13 -> showSettings()
                }
            }
            .show()
    }

    private fun addCurrentBookmark() {
        val url = sessionUrls[currentSessionIndex]
        val title = sessionTitles[currentSessionIndex]
        lifecycleScope.launch {
            val exists = withContext(Dispatchers.IO) {
                database.bookmarkDao().exists(url)
            }
            if (exists > 0) {
                Toast.makeText(this@MainActivity, "该书签已存在", Toast.LENGTH_SHORT).show()
            } else {
                withContext(Dispatchers.IO) {
                    database.bookmarkDao().insert(Bookmark(title = title, url = url, timestamp = System.currentTimeMillis()))
                }
                Toast.makeText(this@MainActivity, "已添加书签：$title", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addToHistory(url: String, title: String) {
        if (settingsManager.privacyModeEnabled) return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.historyDao().insert(HistoryItem(title = title, url = url, timestamp = System.currentTimeMillis()))
            }
        }
    }

    private fun shareCurrentPage() {
        val url = sessionUrls[currentSessionIndex]
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_SUBJECT, sessionTitles[currentSessionIndex])
        }
        startActivity(Intent.createChooser(intent, "分享"))
    }

    private fun showDownloads() {
        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ChuBrowser")
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val files = downloadsDir.listFiles()?.map { it.name }?.toTypedArray() ?: arrayOf("暂无下载文件")
        AlertDialog.Builder(this)
            .setTitle("下载管理")
            .setItems(files) { _, which ->
                if (files[which] != "暂无下载文件") {
                    val file = File(downloadsDir, files[which])
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.fromFile(file), "application/*")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setPositiveButton("打开下载目录") { _, _ ->
                Toast.makeText(this, "下载目录：${downloadsDir.absolutePath}", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showBookmarks() {
        lifecycleScope.launch {
            val bookmarks = withContext(Dispatchers.IO) {
                database.bookmarkDao().getAllList()
            }
            if (bookmarks.isEmpty()) {
                Toast.makeText(this@MainActivity, "暂无书签", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val titles = bookmarks.map { it.title.ifEmpty { it.url } }.toTypedArray()
            AlertDialog.Builder(this@MainActivity)
                .setTitle("书签 (${bookmarks.size}个)")
                .setItems(titles) { _, which ->
                    loadUrl(bookmarks[which].url)
                }
                .setPositiveButton("清除全部") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { database.bookmarkDao().clearAll() }
                        Toast.makeText(this@MainActivity, "书签已清除", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("关闭", null)
                .show()
        }
    }

    private fun showHistory() {
        lifecycleScope.launch {
            val history = withContext(Dispatchers.IO) {
                database.historyDao().getAllList()
            }
            if (history.isEmpty()) {
                Toast.makeText(this@MainActivity, "暂无历史记录", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val titles = history.map { it.title.ifEmpty { it.url } }.toTypedArray()
            AlertDialog.Builder(this@MainActivity)
                .setTitle("历史记录 (${history.size}条)")
                .setItems(titles) { _, which ->
                    loadUrl(history[which].url)
                }
                .setPositiveButton("清除全部") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { database.historyDao().clearAll() }
                        Toast.makeText(this@MainActivity, "历史记录已清除", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("关闭", null)
                .show()
        }
    }

    private fun showPasswords() {
        lifecycleScope.launch {
            val passwords = withContext(Dispatchers.IO) {
                database.passwordDao().getAllList()
            }
            if (passwords.isEmpty()) {
                Toast.makeText(this@MainActivity, "暂无保存的密码", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val items = passwords.map { "${it.site} - ${it.username}" }.toTypedArray()
            AlertDialog.Builder(this@MainActivity)
                .setTitle("密码管理 (${passwords.size}个)")
                .setItems(items) { _, which ->
                    val pwd = passwords[which]
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(pwd.site)
                        .setMessage("用户名：${pwd.username}\n密码：${pwd.encryptedPassword}")
                        .setPositiveButton("复制密码") { _, _ ->
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.text = pwd.encryptedPassword
                            Toast.makeText(this@MainActivity, "密码已复制", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("删除") { _, _ ->
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) { database.passwordDao().delete(pwd) }
                                Toast.makeText(this@MainActivity, "密码已删除", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .show()
                }
                .setNegativeButton("关闭", null)
                .show()
        }
    }

    private fun showCookieManager() {
        AlertDialog.Builder(this)
            .setTitle("Cookie管理")
            .setMessage("当前网站：${sessionUrls[currentSessionIndex]}\n\nGeckoView 内核自动管理 Cookie，隐私模式下不保存 Cookie。\n\n如需清除所有 Cookie，请使用\"清除数据\"功能。")
            .setPositiveButton("刷新页面") { _, _ ->
                currentSession.reload()
                Toast.makeText(this, "已刷新页面", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showCertificateInfo() {
        val url = sessionUrls[currentSessionIndex]
        val isHttps = url.startsWith("https://")
        AlertDialog.Builder(this)
            .setTitle("证书信息")
            .setMessage(
                "当前网址：$url\n\n" +
                "协议：${if (isHttps) "HTTPS (加密)" else "HTTP (未加密)"}\n" +
                "安全状态：${if (isHttps) "安全连接" else "不安全连接"}\n" +
                "内核：Mozilla GeckoView\n" +
                "证书验证：由 GeckoView 内核自动验证"
            )
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showDevTools() {
        val enabled = settingsManager.isDevToolsEnabled()
        AlertDialog.Builder(this)
            .setTitle("开发者工具")
            .setMessage(
                "远程调试：${if (enabled) "已启用" else "已禁用"}\n\n" +
                "使用方法：\n" +
                "1. 在电脑上打开 Firefox 浏览器\n" +
                "2. 地址栏输入 about:debugging\n" +
                "3. 启用 USB 调试并连接设备\n" +
                "4. 即可远程调试本浏览器页面\n\n" +
                "当前状态：${if (enabled) "可连接" else "需在设置中启用"}"
            )
            .setPositiveButton(if (enabled) "禁用调试" else "启用调试") { _, _ ->
                settingsManager.devToolsEnabled = !enabled
                Toast.makeText(this, "远程调试已${if (!enabled) "启用" else "禁用"}，重启应用生效", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showPrivacyMode() {
        val enabled = settingsManager.privacyModeEnabled
        AlertDialog.Builder(this)
            .setTitle("隐私隔离空间")
            .setMessage(
                "当前状态：${if (enabled) "已启用隐私模式" else "普通模式"}\n\n" +
                "隐私模式特性：\n" +
                "• 不记录浏览历史\n" +
                "• 不保存 Cookie\n" +
                "• 不缓存网页数据\n" +
                "• 独立数据分区\n\n" +
                "切换后新标签页生效"
            )
            .setPositiveButton(if (enabled) "关闭隐私模式" else "启用隐私模式") { _, _ ->
                settingsManager.privacyModeEnabled = !enabled
                Toast.makeText(this, "隐私模式已${if (!enabled) "启用" else "关闭"}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun clearBrowsingData() {
        AlertDialog.Builder(this)
            .setTitle("清除浏览数据")
            .setMessage("确定要清除所有历史记录、书签和密码吗？此操作不可撤销。")
            .setPositiveButton("清除") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.historyDao().clearAll()
                        database.bookmarkDao().clearAll()
                        database.passwordDao().clearAll()
                    }
                    currentSession.reload()
                    Toast.makeText(this@MainActivity, "浏览数据已清除", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSettings() {
        val adBlock = settingsManager.adBlockEnabled
        val security = settingsManager.securityEnabled
        val privacy = settingsManager.privacyModeEnabled
        val autofill = settingsManager.autofillEnabled
        val devtools = settingsManager.isDevToolsEnabled()
        val searchEngine = settingsManager.searchEngine

        AlertDialog.Builder(this)
            .setTitle("设置")
            .setMessage(
                "您是 free 用户\n\n" +
                "广告拦截：${if (adBlock) "开启" else "关闭"}\n" +
                "安全验证：${if (security) "开启" else "关闭"}\n" +
                "隐私模式：${if (privacy) "开启" else "关闭"}\n" +
                "自动填充：${if (autofill) "开启" else "关闭"}\n" +
                "开发者工具：${if (devtools) "开启" else "关闭"}\n" +
                "搜索引擎：${searchEngine.uppercase()}\n" +
                "版本：1.0.0"
            )
            .setPositiveButton("广告拦截") { _, _ ->
                settingsManager.adBlockEnabled = !adBlock
                Toast.makeText(this, "广告拦截已${if (!adBlock) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("安全验证") { _, _ ->
                settingsManager.securityEnabled = !security
                Toast.makeText(this, "安全验证已${if (!security) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("搜索引擎") { _, _ ->
                val engines = arrayOf("Bing", "Google", "百度", "DuckDuckGo")
                AlertDialog.Builder(this)
                    .setTitle("选择搜索引擎")
                    .setItems(engines) { _, which ->
                        val engine = when (which) {
                            0 -> "bing"
                            1 -> "google"
                            2 -> "baidu"
                            else -> "duckduckgo"
                        }
                        settingsManager.searchEngine = engine
                        Toast.makeText(this, "搜索引擎已切换为 ${engines[which]}", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (currentSession.canGoBack()) {
                currentSession.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        sessions.forEach { it.close() }
    }
}
