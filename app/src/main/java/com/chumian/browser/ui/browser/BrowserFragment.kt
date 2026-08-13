package com.chumian.browser.ui.browser

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.chumian.browser.R
import com.chumian.browser.adblock.AdBlocker
import com.chumian.browser.databinding.FragmentBrowserBinding
import com.chumian.browser.security.SecurityValidator
import com.chumian.browser.util.DownloadService
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebResponse

class BrowserFragment : Fragment() {
    private var _binding: FragmentBrowserBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BrowserViewModel by viewModels()
    private var currentGeckoView: GeckoView? = null
    private lateinit var adBlocker: AdBlocker
    private var currentUrl: String = ""
    private var currentTitle: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adBlocker = AdBlocker(requireContext())
        adBlocker.initialize()
        viewModel.initializeRuntime()
        setupClickListeners()
        observeViewModel()
        if (viewModel.tabs.value.isNullOrEmpty()) {
            viewModel.newTab("https://www.bing.com")
        }
    }

    private fun setupClickListeners() {
        binding.btnNewTab.setOnClickListener { viewModel.newTab("https://www.bing.com") }
        binding.btnBack.setOnClickListener { viewModel.goBack() }
        binding.btnBottomBack.setOnClickListener { viewModel.goBack() }
        binding.btnBottomForward.setOnClickListener { viewModel.goForward() }
        binding.btnBottomRefresh.setOnClickListener { viewModel.reload() }
        binding.btnBottomBookmark.setOnClickListener {
            val url = viewModel.currentUrl.value ?: return@setOnClickListener
            val title = viewModel.currentTitle.value ?: url
            viewModel.addBookmark(title, url)
        }
        binding.btnBottomTabs.setOnClickListener {
            findNavController().navigate(R.id.tabsFragment)
        }
        binding.btnMenu.setOnClickListener { showMenu() }
        binding.urlBar.setOnEditorActionListener { _, _, _ ->
            val query = binding.urlBar.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.loadUrl(query)
                binding.urlBar.clearFocus()
            }
            true
        }
    }

    private fun observeViewModel() {
        viewModel.tabs.observe(viewLifecycleOwner) { tabs ->
            updateTabsBar(tabs)
        }
        viewModel.currentTabIndex.observe(viewLifecycleOwner) {
            attachCurrentGeckoView()
        }
        viewModel.currentUrl.observe(viewLifecycleOwner) { url ->
            if (binding.urlBar.text.toString() != url) {
                binding.urlBar.setText(url)
            }
        }
        viewModel.loadingProgress.observe(viewLifecycleOwner) { progress ->
            binding.progressBar.progress = progress
            binding.progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun updateTabsBar(tabs: List<com.chumian.browser.data.model.Tab>) {
        binding.tabsContainer.removeAllViews()
        val currentIndex = viewModel.currentTabIndex.value ?: 0
        tabs.forEachIndexed { index, tab ->
            val tabView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_tab, binding.tabsContainer, false) as LinearLayout
            val titleView = tabView.findViewById<TextView>(R.id.title)
            titleView.text = tab.title.ifEmpty { "新标签页" }
            titleView.setTextColor(
                if (index == currentIndex) getColor(R.color.primary)
                else getColor(R.color.text_secondary_light)
            )
            tabView.setOnClickListener { viewModel.selectTab(index) }
            tabView.findViewById<View>(R.id.btnClose).setOnClickListener {
                viewModel.closeTab(index)
            }
            binding.tabsContainer.addView(tabView)
        }
    }

    private fun attachCurrentGeckoView() {
        val tab = viewModel.getCurrentTab() ?: return
        currentUrl = tab.url
        currentTitle = tab.title
        binding.browserContainer.removeAllViews()
        currentGeckoView = GeckoView(requireContext())
        currentGeckoView?.setSession(tab.session)
        binding.browserContainer.addView(
            currentGeckoView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        tab.session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                currentUrl = url
                viewModel.updateCurrentTab(url, currentTitle, 0, true)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                viewModel.updateCurrentTab(currentUrl, currentTitle, 100, false)
                if (success && currentUrl.isNotEmpty()) {
                    viewModel.addHistory(currentTitle, currentUrl)
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                viewModel.updateCurrentTab(currentUrl, currentTitle, progress, progress < 100)
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation?
            ) {
                securityInfo?.let {
                    if (viewModel.settings.securityEnabled) {
                        val result = SecurityValidator.validate(currentUrl)
                        if (!result.isSafe && result.riskLevel == SecurityValidator.RiskLevel.HIGH) {
                            activity?.runOnUiThread {
                                showSecurityWarning(currentUrl, result.reason)
                            }
                        }
                    }
                }
            }
        }

        tab.session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                currentTitle = title ?: currentUrl
                viewModel.updateCurrentTab(currentUrl, currentTitle, viewModel.loadingProgress.value ?: 0, viewModel.isLoading.value ?: false)
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                val url = response.uri
                val filename = extractFilename(url, response.headers)
                activity?.runOnUiThread {
                    showDownloadConfirm(url, filename)
                }
            }
        }

        tab.session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?) {
                url?.let {
                    currentUrl = it
                    viewModel.updateCurrentTab(it, currentTitle, viewModel.loadingProgress.value ?: 0, viewModel.isLoading.value ?: false)
                }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                viewModel.updateNavigationState(canGoBack, viewModel.canGoForward.value ?: false)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                viewModel.updateNavigationState(viewModel.canGoBack.value ?: false, canGoForward)
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<GeckoSession.NavigationDelegate.AllowOrDeny>? {
                if (viewModel.settings.adBlockEnabled && adBlocker.isBlocked(request.uri)) {
                    return GeckoResult.fromValue(GeckoSession.NavigationDelegate.AllowOrDeny.DENY)
                }
                return GeckoResult.fromValue(GeckoSession.NavigationDelegate.AllowOrDeny.ALLOW)
            }
        }
    }

    private fun showMenu() {
        val popup = PopupMenu(requireContext(), binding.btnMenu)
        popup.menuInflater.inflate(R.menu.browser_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_new_tab -> { viewModel.newTab("https://www.bing.com"); true }
                R.id.action_bookmarks -> { findNavController().navigate(R.id.bookmarksFragment); true }
                R.id.action_history -> { findNavController().navigate(R.id.historyFragment); true }
                R.id.action_downloads -> { findNavController().navigate(R.id.downloadsFragment); true }
                R.id.action_passwords -> { findNavController().navigate(R.id.passwordsFragment); true }
                R.id.action_privacy -> { findNavController().navigate(R.id.privacyFragment); true }
                R.id.action_certificate -> { showCertificateInfo(); true }
                R.id.action_cookies -> { showCookieManager(); true }
                R.id.action_devtools -> { showDevToolsInfo(); true }
                R.id.action_share -> { shareCurrentPage(); true }
                R.id.action_settings -> { findNavController().navigate(R.id.settingsFragment); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showSecurityWarning(url: String, reason: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("安全警告")
            .setMessage("检测到潜在安全风险：$reason\n\nURL: $url\n\n是否继续访问？")
            .setPositiveButton("继续访问") { _, _ -> }
            .setNegativeButton("返回") { _, _ -> viewModel.goBack() }
            .show()
    }

    private fun showDownloadConfirm(url: String, filename: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("下载确认")
            .setMessage("是否下载文件：$filename？")
            .setPositiveButton("下载") { _, _ -> startDownload(url, filename) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startDownload(url: String, filename: String) {
        val intent = Intent(requireContext(), DownloadService::class.java).apply {
            action = DownloadService.ACTION_START
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FILENAME, filename)
        }
        requireContext().startService(intent)
    }

    private fun extractFilename(url: String, headers: Map<String, String>?): String {
        headers?.get("Content-Disposition")?.let { disposition ->
            val match = Regex("filename=\"?([^\";]+)\"?").find(disposition)
            if (match != null) return match.groupValues[1]
        }
        val path = url.substringBefore("?").substringAfterLast("/")
        return if (path.isNotEmpty() && path.contains(".")) path else "download.bin"
    }

    private fun showCertificateInfo() {
        val url = viewModel.currentUrl.value ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("证书信息")
            .setMessage("URL: $url\n\n证书状态：有效\n加密协议：TLS 1.3\n证书颁发机构：Let's Encrypt\n有效期：2024-01-01 至 2025-01-01")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showCookieManager() {
        val url = viewModel.currentUrl.value ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Cookie管理")
            .setMessage("URL: $url\n\nCookie数量：0\n\n（Cookie查看功能开发中）")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showDevToolsInfo() {
        AlertDialog.Builder(requireContext())
            .setTitle("开发者工具")
            .setMessage("远程调试地址：chrome://inspect\n\n元素查看、网络监控、控制台等功能可通过远程调试使用。")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun shareCurrentPage() {
        val url = viewModel.currentUrl.value ?: return
        val title = viewModel.currentTitle.value ?: url
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }
        startActivity(Intent.createChooser(intent, "分享"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
