package com.chubrowser.app.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chubrowser.app.ChuBrowserApp
import com.chubrowser.app.R
import com.chubrowser.app.adblock.AdBlockManager
import com.chubrowser.app.bookmark.BookmarkActivity
import com.chubrowser.app.cert.CertificateInfoActivity
import com.chubrowser.app.cookie.CookieManagerActivity
import com.chubrowser.app.core.GeckoEngine
import com.chubrowser.app.devtools.DevToolsActivity
import com.chubrowser.app.download.DownloadActivity
import com.chubrowser.app.download.DownloadManager
import com.chubrowser.app.history.HistoryActivity
import com.chubrowser.app.password.PasswordManager
import com.chubrowser.app.password.PasswordManagerActivity
import com.chubrowser.app.privacy.PrivacySpaceActivity
import com.chubrowser.app.security.CaptchaRecognizer
import com.chubrowser.app.security.SecurityCheckActivity
import com.chubrowser.app.security.SecurityManager
import com.chubrowser.app.security.WebDetector
import com.chubrowser.app.settings.SettingsActivity
import com.chubrowser.app.tabs.Tab
import com.chubrowser.app.tabs.TabManager
import com.chubrowser.app.tabs.TabManagerActivity
import com.chubrowser.app.utils.Utils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.PermissionDelegate
import org.mozilla.geckoview.WebRequestError

class MainActivity : AppCompatActivity() {

    private lateinit var geckoView: GeckoView
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tabManager: TabManager
    private lateinit var geckoEngine: GeckoEngine
    private lateinit var downloadManager: DownloadManager
    private lateinit var passwordManager: PasswordManager
    private lateinit var securityManager: SecurityManager
    private lateinit var webDetector: WebDetector
    private lateinit var captchaRecognizer: CaptchaRecognizer
    private lateinit var adBlockManager: AdBlockManager

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "部分权限被拒绝，部分功能可能无法使用", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化管理器
        tabManager = TabManager()
        geckoEngine = GeckoEngine.getInstance(this)
        downloadManager = DownloadManager(this)
        passwordManager = PasswordManager(this)
        securityManager = SecurityManager(this)
        webDetector = WebDetector(this)
        captchaRecognizer = CaptchaRecognizer(this)
        adBlockManager = AdBlockManager(this)

        // 初始化视图
        geckoView = findViewById(R.id.geckoView)
        addressBar = findViewById(R.id.addressBar)
        progressBar = findViewById(R.id.progressBar)
        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottomNav)

        setSupportActionBar(toolbar)

        // 请求启动权限
        requestStartupPermissions()

        // 设置地址栏
        setupAddressBar()

        // 设置底部导航
        setupBottomNav()

        // 创建初始标签页
        if (savedInstanceState == null) {
            val homepage = ChuBrowserApp.settingsManager.getHomepage()
            tabManager.createTab(url = homepage, select = true)
            attachCurrentSession()
        }

        // 检查是否是首次启动
        if (ChuBrowserApp.settingsManager.isFirstLaunch()) {
            showFirstLaunchDialog()
            ChuBrowserApp.settingsManager.setFirstLaunchComplete()
        }
    }

    private fun requestStartupPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.INTERNET)
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    private fun setupAddressBar() {
        addressBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                loadUrl(addressBar.text.toString())
                true
            } else false
        }

        addressBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_back -> {
                    if (tabManager.canGoBack()) {
                        tabManager.goBack()
                    }
                    true
                }
                R.id.nav_forward -> {
                    if (tabManager.canGoForward()) {
                        tabManager.goForward()
                    }
                    true
                }
                R.id.nav_home -> {
                    val homepage = ChuBrowserApp.settingsManager.getHomepage()
                    loadUrl(homepage)
                    true
                }
                R.id.nav_tabs -> {
                    startActivity(Intent(this, TabManagerActivity::class.java))
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

    private fun attachCurrentSession() {
        val tab = tabManager.getCurrentTab() ?: return
        geckoView.session = tab.session

        tab.session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                tab.title = title ?: ""
                tabManager.updateTab(tab.id) { copy(title = title ?: "") }
            }

            override fun onLocationChange(session: GeckoSession, url: String?) {
                url?.let {
                    tab.url = it
                    addressBar.setText(it)
                    tabManager.updateTab(tab.id) { copy(url = it) }
                    // 保存历史记录
                    saveHistory(it, tab.title)
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                progressBar.progress = progress
                progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
                tab.progress = progress
            }

            override fun onPageStart(session: GeckoSession, url: String?) {
                tab.isLoading = true
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                tab.isLoading = false
                progressBar.visibility = View.GONE
                // 检测验证码
                checkForCaptcha()
            }

            override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.SecurityInfo?) {
                // 安全状态变化
            }
        }

        tab.session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String?) {
                tab.isLoading = true
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                tab.isLoading = false
                progressBar.visibility = View.GONE
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                progressBar.progress = progress
            }

            override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.SecurityInfo?) {}
        }

        tab.session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?) {
                url?.let {
                    addressBar.setText(it)
                    tab.url = it
                }
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                // 检查是否需要拦截
                if (webDetector.shouldBlockQuick(request.uri)) {
                    activityScope.launch {
                        showBlockedWarning(request.uri)
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return null
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession>? {
                val newTab = tabManager.createTab(url = uri, select = true)
                return GeckoResult.fromValue(newTab.session)
            }
        }

        tab.session.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onContentPermissionRequest(
                session: GeckoSession,
                perm: ContentPermission
            ): GeckoResult<Int>? {
                return GeckoResult.fromValue(PermissionDelegate.ContentPermission.VALUE_ALLOW)
            }

            override fun onAndroidPermissionsRequest(
                session: GeckoSession,
                permissions: Array<out String>?,
                callback: Callback
            ) {
                permissions?.let {
                    permissionLauncher.launch(it)
                    callback.grant()
                } ?: callback.deny()
            }

            override fun onMediaPermissionRequest(
                session: GeckoSession,
                uri: String,
                video: Array<out MediaSource>?,
                audio: Array<out MediaSource>?,
                callback: MediaCallback
            ) {
                callback.grant(video?.firstOrNull(), audio?.firstOrNull())
            }
        }

        tab.session.downloadDelegate = object : GeckoSession.DownloadDelegate {
            override fun onDownload(
                session: GeckoSession,
                download: GeckoSession.Download
            ) {
                showDownloadConfirm(download)
            }
        }

        tab.session.promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onAlertPrompt(
                session: GeckoSession,
                prompt: AlertPrompt
            ): GeckoResult<PromptResponse>? {
                runOnUiThread {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(prompt.title ?: "")
                        .setMessage(prompt.message)
                        .setPositiveButton("确定") { dialog, _ ->
                            prompt.dismiss()
                            dialog.dismiss()
                        }
                        .show()
                }
                return GeckoResult.fromValue(prompt.dismiss())
            }

            override fun onButtonPrompt(
                session: GeckoSession,
                prompt: ButtonPrompt
            ): GeckoResult<PromptResponse>? {
                return GeckoResult.fromValue(prompt.dismiss())
            }

            override fun onTextPrompt(
                session: GeckoSession,
                prompt: TextPrompt
            ): GeckoResult<PromptResponse>? {
                return GeckoResult.fromValue(prompt.dismiss())
            }

            override fun onAuthPrompt(
                session: GeckoSession,
                prompt: AuthPrompt
            ): GeckoResult<PromptResponse>? {
                return GeckoResult.fromValue(prompt.dismiss())
            }

            override fun onChoicePrompt(
                session: GeckoSession,
                prompt: ChoicePrompt
            ): GeckoResult<PromptResponse>? {
                return GeckoResult.fromValue(prompt.dismiss())
            }

            override fun onDateTimePrompt(
                session: GeckoSession,
                prompt: DateTimePrompt
            ): GeckoResult<PromptResponse>? {
                return GeckoResult.fromValue(prompt.dismiss())
            }

            override fun onColorPrompt(
                session: GeckoSession,
                prompt: ColorPrompt
            ): GeckoResult<PromptResponse>? {
                return GeckoResult.fromValue(prompt.dismiss())
            }

            override fun onFilePrompt(
                session: GeckoSession,
                prompt: FilePrompt
            ): GeckoResult<PromptResponse>? {
                return GeckoResult.fromValue(prompt.dismiss())
            }
        }
    }

    private fun loadUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        val url = if (Utils.isUrl(trimmed)) {
            Utils.normalizeUrl(trimmed)
        } else {
            val searchEngine = ChuBrowserApp.settingsManager.getSearchEngine()
            Utils.getSearchUrl(trimmed, searchEngine)
        }

        val tab = tabManager.getCurrentTab()
        if (tab != null) {
            tab.session.loadUri(url)
            addressBar.clearFocus()
        }
    }

    private fun showDownloadConfirm(download: GeckoSession.Download) {
        val fileName = downloadManager.extractFileName(download.uri, download.contentDisposition)
        val fileSize = if (download.contentLength > 0) {
            Utils.formatFileSize(download.contentLength)
        } else {
            "未知大小"
        }

        runOnUiThread {
            MaterialAlertDialogBuilder(this)
                .setTitle("下载确认")
                .setMessage("文件名: $fileName\n大小: $fileSize\n\n是否下载此文件？")
                .setPositiveButton("下载") { _, _ ->
                    DownloadManager.DownloadRequest(
                        url = download.uri,
                        fileName = fileName,
                        mimeType = download.contentType ?: "",
                        contentLength = download.contentLength,
                        userAgent = ""
                    ).let { request ->
                        activityScope.launch {
                            downloadManager.startDownload(request)
                            Toast.makeText(this@MainActivity, "开始下载: $fileName", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun showBlockedWarning(url: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("网站已被拦截")
            .setMessage("检测到该网站可能存在安全风险，已被安全防护拦截。\n\nURL: $url")
            .setPositiveButton("了解", null)
            .setNegativeButton("仍要访问") { _, _ ->
                val tab = tabManager.getCurrentTab()
                tab?.session?.loadUri(url)
            }
            .show()
    }

    private fun checkForCaptcha() {
        if (!captchaRecognizer.isCaptchaAutoFillEnabled()) return

        val tab = tabManager.getCurrentTab() ?: return
        // 通过JavaScript检测验证码图片
        val script = """
            (function() {
                var captchaImages = document.querySelectorAll('img[src*="captcha"], img[src*="verify"], img[src*="vcode"], img[alt*="验证码"]');
                return captchaImages.length > 0 ? captchaImages[0].src : '';
            })()
        """.trimIndent()

        tab.session.evaluateJS(script)?.let { result ->
            result.then { imageUrl ->
                if (imageUrl is String && imageUrl.isNotBlank()) {
                    // 下载验证码图片并识别
                    activityScope.launch {
                        // 简化处理：提示用户可以使用验证码识别
                        Toast.makeText(
                            this@MainActivity,
                            "检测到验证码，可长按图片识别",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun saveHistory(url: String, title: String) {
        activityScope.launch(Dispatchers.IO) {
            try {
                val historyDao = ChuBrowserApp.database.historyDao()
                val existing = historyDao.getByUrl(url)
                if (existing != null) {
                    historyDao.update(
                        existing.copy(
                            title = title,
                            visitCount = existing.visitCount + 1,
                            lastVisited = System.currentTimeMillis()
                        )
                    )
                } else {
                    com.chubrowser.app.history.HistoryEntity(
                        url = url,
                        title = title
                    ).let { historyDao.insert(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showFirstLaunchDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("欢迎使用Chu浏览器")
            .setMessage(
                "Chu浏览器使用Firefox GeckoView内核，为您提供安全、快速的浏览体验。\n\n" +
                "主要功能：\n" +
                "• 广告拦截\n" +
                "• 安全检测\n" +
                "• 密码管理\n" +
                "• 隐私隔离\n" +
                "• 开发者工具\n\n" +
                "默认搜索引擎为Bing，可在设置中切换。"
            )
            .setPositiveButton("开始使用", null)
            .show()
    }

    private fun showMenuDialog() {
        val options = arrayOf(
            "刷新", "前进", "后退", "主页",
            "添加书签", "分享", "复制链接",
            "书签", "历史", "下载",
            "密码管理", "隐私空间", "Cookie管理",
            "证书信息", "安全检测", "开发者工具",
            "新建标签页", "关闭标签页", "设置", "退出"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("菜单")
            .setItems(options) { _, which ->
                handleMenuAction(which)
            }
            .show()
    }

    private fun handleMenuAction(which: Int) {
        when (which) {
            0 -> tabManager.reloadCurrentTab()
            1 -> if (tabManager.canGoForward()) tabManager.goForward()
            2 -> if (tabManager.canGoBack()) tabManager.goBack()
            3 -> loadUrl(ChuBrowserApp.settingsManager.getHomepage())
            4 -> addCurrentBookmark()
            5 -> shareCurrentPage()
            6 -> copyCurrentUrl()
            7 -> startActivity(Intent(this, BookmarkActivity::class.java))
            8 -> startActivity(Intent(this, HistoryActivity::class.java))
            9 -> startActivity(Intent(this, DownloadActivity::class.java))
            10 -> startActivity(Intent(this, PasswordManagerActivity::class.java))
            11 -> startActivity(Intent(this, PrivacySpaceActivity::class.java))
            12 -> startActivity(Intent(this, CookieManagerActivity::class.java))
            13 -> startActivity(Intent(this, CertificateInfoActivity::class.java).apply {
                putExtra("url", tabManager.getCurrentTab()?.url ?: "")
            })
            14 -> startActivity(Intent(this, SecurityCheckActivity::class.java).apply {
                putExtra("url", tabManager.getCurrentTab()?.url ?: "")
            })
            15 -> startActivity(Intent(this, DevToolsActivity::class.java))
            16 -> tabManager.createTab(select = true).also { attachCurrentSession() }
            17 -> {
                val tab = tabManager.getCurrentTab()
                if (tab != null) {
                    tabManager.closeTab(tab.id)
                    attachCurrentSession()
                }
            }
            18 -> startActivity(Intent(this, SettingsActivity::class.java))
            19 -> finish()
        }
    }

    private fun addCurrentBookmark() {
        val tab = tabManager.getCurrentTab() ?: return
        activityScope.launch {
            ChuBrowserApp.bookmarkManager.addBookmark(tab.url, tab.title.ifEmpty { tab.url })
            Toast.makeText(this@MainActivity, "已添加书签", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCurrentPage() {
        val tab = tabManager.getCurrentTab() ?: return
        Utils.shareText(this, tab.title, tab.url)
    }

    private fun copyCurrentUrl() {
        val tab = tabManager.getCurrentTab() ?: return
        if (Utils.copyToClipboard(this, tab.url)) {
            Toast.makeText(this, "链接已复制", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                tabManager.reloadCurrentTab()
                true
            }
            R.id.action_bookmark -> {
                addCurrentBookmark()
                true
            }
            R.id.action_share -> {
                shareCurrentPage()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (tabManager.canGoBack()) {
                tabManager.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        geckoView.onResume()
    }

    override fun onPause() {
        super.onPause()
        geckoView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        captchaRecognizer.close()
    }
}
