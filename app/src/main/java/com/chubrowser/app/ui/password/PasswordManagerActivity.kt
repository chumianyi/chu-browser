package com.chubrowser.app.ui.password

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chubrowser.app.ChuBrowserApp
import com.chubrowser.app.R
import com.chubrowser.app.password.PasswordEntity
import com.chubrowser.app.password.PasswordManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PasswordManagerActivity : AppCompatActivity() {

    private lateinit var passwordManager: PasswordManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: PasswordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_manager)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        passwordManager = PasswordManager(this)
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PasswordAdapter(
            onItemClick = { entity -> showPasswordDetail(entity) },
            onDeleteClick = { entity -> deletePassword(entity) }
        )
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddPasswordDialog()
        }

        findViewById<Button>(R.id.btnGenerate).setOnClickListener {
            showGeneratePasswordDialog()
        }

        loadPasswords()
    }

    private fun loadPasswords() {
        Thread {
            val passwords = passwordManager.getAllPasswords()
            runOnUiThread {
                adapter.updateData(passwords)
                emptyView.visibility = if (passwords.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }.start()
    }

    private fun showAddPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_password, null)
        val etUrl = dialogView.findViewById<EditText>(R.id.etUrl)
        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)

        MaterialAlertDialogBuilder(this)
            .setTitle("添加密码")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val url = etUrl.text.toString().trim()
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString()
                val title = etTitle.text.toString().trim()

                if (url.isBlank() || username.isBlank() || password.isBlank()) {
                    Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                Thread {
                    passwordManager.savePassword(url, username, password, title)
                    runOnUiThread {
                        loadPasswords()
                        Toast.makeText(this, "密码已保存", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showGeneratePasswordDialog() {
        val generated = passwordManager.generateStrongPassword(64)
        val strength = passwordManager.calculatePasswordStrength(generated)

        MaterialAlertDialogBuilder(this)
            .setTitle("强密码生成器")
            .setMessage(
                "生成的密码:\n\n$generated\n\n" +
                "密码强度: ${strength.label}\n" +
                "长度: ${generated.length} 位"
            )
            .setPositiveButton("复制") { _, _ ->
                com.chubrowser.app.utils.Utils.copyToClipboard(this, generated)
                Toast.makeText(this, "密码已复制", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("重新生成") { _, _ ->
                showGeneratePasswordDialog()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showPasswordDetail(entity: PasswordEntity) {
        Thread {
            try {
                val decrypted = passwordManager.decrypt(entity.encryptedPassword)
                runOnUiThread {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(entity.title.ifEmpty { entity.domain })
                        .setMessage(
                            "网址: ${entity.url}\n" +
                            "用户名: ${entity.username}\n" +
                            "密码: $decrypted\n" +
                            "域名: ${entity.domain}"
                        )
                        .setPositiveButton("复制密码") { _, _ ->
                            com.chubrowser.app.utils.Utils.copyToClipboard(this, decrypted)
                            Toast.makeText(this, "密码已复制", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("关闭", null)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun deletePassword(entity: PasswordEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除密码")
            .setMessage("确定要删除 ${entity.domain} 的密码吗？")
            .setPositiveButton("删除") { _, _ ->
                Thread {
                    passwordManager.deletePassword(entity.id)
                    runOnUiThread {
                        loadPasswords()
                        Toast.makeText(this, "密码已删除", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    inner class PasswordAdapter(
        private val onItemClick: (PasswordEntity) -> Unit,
        private val onDeleteClick: (PasswordEntity) -> Unit
    ) : RecyclerView.Adapter<PasswordAdapter.ViewHolder>() {

        private var data: List<PasswordEntity> = emptyList()

        fun updateData(newData: List<PasswordEntity>) {
            data = newData
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
            val tvUrl: TextView = itemView.findViewById(R.id.tvUrl)
            val btnDelete: android.view.View = itemView.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_password, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            holder.tvTitle.text = item.title.ifEmpty { item.domain }
            holder.tvUsername.text = item.username
            holder.tvUrl.text = item.url
            holder.itemView.setOnClickListener { onItemClick(item) }
            holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        }

        override fun getItemCount() = data.size
    }
}
