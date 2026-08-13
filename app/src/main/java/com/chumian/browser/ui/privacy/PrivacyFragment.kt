package com.chumian.browser.ui.privacy

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chumian.browser.ChuBrowserApp
import com.chumian.browser.databinding.FragmentPrivacyBinding
import androidx.navigation.fragment.findNavController

class PrivacyFragment : Fragment() {

    private var _binding: FragmentPrivacyBinding? = null
    private val binding get() = _binding!!
    private val settings by lazy { ChuBrowserApp.instance.settingsManager }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivacyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.switchPrivacyMode.isChecked = settings.privacyModeEnabled
        updatePrivacyStatus()

        binding.switchPrivacyMode.setOnCheckedChangeListener { _, isChecked ->
            settings.privacyModeEnabled = isChecked
            updatePrivacyStatus()
            if (isChecked) {
                AlertDialog.Builder(requireContext())
                    .setTitle("隐私隔离模式")
                    .setMessage("已启用隐私隔离模式。在此模式下，所有浏览数据将存储在独立的隔离空间中，关闭后自动清除。")
                    .setPositiveButton("确定", null)
                    .show()
            }
        }

        binding.btnClearPrivacyData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("清除隔离空间数据")
                .setMessage("确定要清除隐私隔离空间中的所有数据吗？")
                .setPositiveButton("清除") { _, _ ->
                    clearPrivacyData()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun updatePrivacyStatus() {
        binding.tvPrivacyStatus.text = if (settings.privacyModeEnabled) {
            "当前正在使用隐私隔离空间，浏览数据不会被保存"
        } else {
            "当前未使用隔离空间"
        }
    }

    private fun clearPrivacyData() {
        Thread {
            try {
                val cacheDir = requireContext().cacheDir
                val privacyDir = requireContext().getDir("privacy_isolation", 0)
                cacheDir.deleteRecursively()
                privacyDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
