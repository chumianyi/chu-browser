package com.chumian.browser.ui

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.chumian.browser.R
import com.chumian.browser.adblock.AdBlocker
import com.chumian.browser.security.SecurityValidator
import com.chumian.browser.util.SettingsManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.AllowOrDeny

class MainActivity : AppCompatActivity() {

    private lateinit var geckoView: GeckoView
    private lateinit var geckoSession: GeckoSession
    private lateinit var geckoRuntime: GeckoRuntime
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var settingsManager: SettingsManager
    private lateinit var adBlocker: AdBlocker

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

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        addressBar = findViewById(R.id.addressBar)
        progressBar = findViewById(R.id.progressBar)
        geckoView = findViewById(R.id.geckoView)
        bottomNav = findViewById(R.id.bottomNav)

        requestStartupPermissions()
        setupGeckoView()
        setupAddressBar()
        setupBottomNav()

        if (savedInstanceState == null) {
            loadUrl(settingsManager.getHomepage())
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

    private fun setupGeckoView() {
        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .remoteDebuggingEnabled(settingsManager.isDevToolsEnabled())
            .build()

        geckoRuntime = GeckoRuntime.create(this, runtimeSettings)
        geckoSession = GeckoSession()
        geckoSession.open(geckoRuntime)
        geckoView.setSession(geckoSession)

        geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
                addressBar.setText(url)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                progressBar.visibility = View.GONE
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                progressBar.progress = progress
            }
        }

        geckoSession.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                toolbar.title = title ?: "Chu浏览器"
            }
        }

        geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val url = request.uri
                if (settingsManager.securityEnabled && !SecurityValidator.validate(url).isSafe) {
                    showBlockedWarning(url)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return null
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession>? {
                loadUrl(uri)
                return null
            }
        }
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
                    geckoSession.goBack()
                    true
                }
                R.id.nav_forward -> {
                    geckoSession.goForward()
                    true
                }
                R.id.nav_home -> {
                    loadUrl(settingsManager.getHomepage())
                    true
                }
                R.id.nav_tabs -> {
                    Toast.makeText(this, "标签页管理", Toast.LENGTH_SHORT).show()
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
        geckoSession.loadUri(url)
    }

    private fun showBlockedWarning(url: String) {
        AlertDialog.Builder(this)
            .setTitle("安全警告")
            .setMessage("检测到该网站可能存在安全风险：\n$url\n\n是否仍要访问？")
            .setPositiveButton("仍要访问") { _, _ ->
                geckoSession.loadUri(url)
            }
            .setNegativeButton("返回安全", null)
            .show()
    }

    private fun showMenuDialog() {
        val items = arrayOf(
            "刷新", "添加书签", "分享", "下载管理", "书签", "历史记录",
            "密码管理", "Cookie管理", "证书信息", "开发者工具", "隐私空间", "设置"
        )
        AlertDialog.Builder(this)
            .setTitle("菜单")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> geckoSession.reload()
                    1 -> Toast.makeText(this, "已添加书签", Toast.LENGTH_SHORT).show()
                    2 -> shareCurrentPage()
                    3 -> Toast.makeText(this, "下载管理", Toast.LENGTH_SHORT).show()
                    4 -> Toast.makeText(this, "书签", Toast.LENGTH_SHORT).show()
                    5 -> Toast.makeText(this, "历史记录", Toast.LENGTH_SHORT).show()
                    6 -> Toast.makeText(this, "密码管理", Toast.LENGTH_SHORT).show()
                    7 -> Toast.makeText(this, "Cookie管理", Toast.LENGTH_SHORT).show()
                    8 -> Toast.makeText(this, "证书信息", Toast.LENGTH_SHORT).show()
                    9 -> Toast.makeText(this, "开发者工具", Toast.LENGTH_SHORT).show()
                    10 -> Toast.makeText(this, "隐私空间", Toast.LENGTH_SHORT).show()
                    11 -> Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun shareCurrentPage() {
        val url = addressBar.text.toString()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, "分享"))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                geckoSession.reload()
                true
            }
            R.id.action_bookmark -> {
                Toast.makeText(this, "已添加书签", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_share -> {
                shareCurrentPage()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            geckoSession.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        geckoSession.close()
    }
}
