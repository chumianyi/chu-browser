package com.chumian.browser.ui.passwords

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.browser.ChuBrowserApp
import com.chumian.browser.R
import com.chumian.browser.data.model.PasswordItem
import com.chumian.browser.databinding.FragmentPasswordsBinding
import com.chumian.browser.util.EncryptionHelper
import com.chumian.browser.util.PasswordGenerator
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PasswordsFragment : Fragment() {

    private var _binding: FragmentPasswordsBinding? = null
    private val binding get() = _binding!!
    private val db by lazy { ChuBrowserApp.instance.database }
    private lateinit var adapter: PasswordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnGenerate.setOnClickListener { showGeneratePasswordDialog() }

        adapter = PasswordAdapter(
            onItemClick = { item -> showPasswordDetail(item) },
            onDeleteClick = { item ->
                lifecycleScope.launch { db.passwordDao().delete(item) }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            db.passwordDao().getAll().collectLatest { passwords ->
                adapter.submitList(passwords)
                binding.emptyView.visibility = if (passwords.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showGeneratePasswordDialog() {
        val generatedPassword = PasswordGenerator.generate(32)

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val passwordView = EditText(requireContext()).apply {
            setText(generatedPassword)
            isFocusable = false
            textSize = 16f
        }

        val siteInput = EditText(requireContext()).apply {
            hint = "网站地址"
        }

        val usernameInput = EditText(requireContext()).apply {
            hint = "用户名"
        }

        layout.addView(passwordView)
        layout.addView(siteInput)
        layout.addView(usernameInput)

        AlertDialog.Builder(requireContext())
            .setTitle("生成强密码")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val site = siteInput.text.toString().trim()
                val username = usernameInput.text.toString().trim()
                if (site.isNotEmpty() && username.isNotEmpty()) {
                    val encrypted = EncryptionHelper.encrypt(generatedPassword)
                    lifecycleScope.launch {
                        db.passwordDao().insert(
                            PasswordItem(
                                site = site,
                                username = username,
                                encryptedPassword = encrypted
                            )
                        )
                    }
                }
            }
            .setNegativeButton("重新生成") { _, _ -> showGeneratePasswordDialog() }
            .setNeutralButton("取消", null)
            .show()
    }

    private fun showPasswordDetail(item: PasswordItem) {
        try {
            val decrypted = EncryptionHelper.decrypt(item.encryptedPassword)
            AlertDialog.Builder(requireContext())
                .setTitle(item.site)
                .setMessage("用户名: ${item.username}\n密码: $decrypted")
                .setPositiveButton("确定", null)
                .show()
        } catch (e: Exception) {
            AlertDialog.Builder(requireContext())
                .setTitle("错误")
                .setMessage("无法解密密码")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
