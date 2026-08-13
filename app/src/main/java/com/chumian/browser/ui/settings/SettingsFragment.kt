package com.chumian.browser.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.chumian.browser.ChuBrowserApp
import com.chumian.browser.R
import com.chumian.browser.databinding.FragmentSettingsBinding
import com.chumian.browser.ui.browser.BrowserViewModel
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val settings by lazy { ChuBrowserApp.instance.settingsManager }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.freeUserBanner.text = getString(R.string.free_user_banner)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.switchAdBlock.isChecked = settings.adBlockEnabled
        binding.switchSecurity.isChecked = settings.securityEnabled
        binding.switchAutofill.isChecked = settings.autofillEnabled
        binding.switchCaptcha.isChecked = settings.captchaAutoEnabled

        binding.tvSearchEngineValue.text = when (settings.searchEngine) {
            "google" -> "Google"
            "baidu" -> "百度"
            "duckduckgo" -> "DuckDuckGo"
            else -> "Bing"
        }

        binding.tvThemeValue.text = when (settings.themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "浅色"
            AppCompatDelegate.MODE_NIGHT_YES -> "深色"
            else -> "跟随系统"
        }
    }

    private fun setupListeners() {
        binding.switchAdBlock.setOnCheckedChangeListener { _, isChecked ->
            settings.adBlockEnabled = isChecked
        }

        binding.switchSecurity.setOnCheckedChangeListener { _, isChecked ->
            settings.securityEnabled = isChecked
        }

        binding.switchAutofill.setOnCheckedChangeListener { _, isChecked ->
            settings.autofillEnabled = isChecked
        }

        binding.switchCaptcha.setOnCheckedChangeListener { _, isChecked ->
            settings.captchaAutoEnabled = isChecked
        }

        binding.tvSearchEngineValue.setOnClickListener { showSearchEngineDialog() }
        binding.tvThemeValue.setOnClickListener { showThemeDialog() }

        binding.itemPasswords.setOnClickListener {
            findNavController().navigate(R.id.passwordsFragment)
        }

        binding.itemDownloads.setOnClickListener {
            findNavController().navigate(R.id.downloadsFragment)
        }

        binding.itemClearData.setOnClickListener { showClearDataDialog() }
    }

    private fun showSearchEngineDialog() {
        val engines = arrayOf("Bing", "Google", "百度", "DuckDuckGo")
        val values = arrayOf("bing", "google", "baidu", "duckduckgo")
        val currentIndex = values.indexOf(settings.searchEngine)

        AlertDialog.Builder(requireContext())
            .setTitle("选择搜索引擎")
            .setSingleChoiceItems(engines, currentIndex) { dialog, which ->
                settings.searchEngine = values[which]
                binding.tvSearchEngineValue.text = engines[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("跟随系统", "浅色", "深色")
        val values = arrayOf(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
        )
        val currentIndex = values.indexOf(settings.themeMode)

        AlertDialog.Builder(requireContext())
            .setTitle("选择主题")
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                settings.themeMode = values[which]
                binding.tvThemeValue.text = themes[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("清除浏览数据")
            .setMessage("确定要清除所有历史记录、书签和保存的密码吗？此操作不可撤销。")
            .setPositiveButton("清除") { _, _ ->
                lifecycleScope.launch {
                    ChuBrowserApp.instance.database.historyDao().clearAll()
                    ChuBrowserApp.instance.database.bookmarkDao().clearAll()
                    ChuBrowserApp.instance.database.passwordDao().clearAll()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
