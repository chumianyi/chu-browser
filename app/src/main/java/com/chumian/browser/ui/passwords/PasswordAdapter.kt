package com.chumian.browser.ui.passwords

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chumian.browser.R
import com.chumian.browser.data.model.PasswordItem
import com.chumian.browser.util.EncryptionHelper

class PasswordAdapter(
    private val onItemClick: (PasswordItem) -> Unit,
    private val onDeleteClick: (PasswordItem) -> Unit
) : ListAdapter<PasswordItem, PasswordAdapter.ViewHolder>(PasswordDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val site: TextView = view.findViewById(R.id.site)
        val username: TextView = view.findViewById(R.id.username)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.site.text = item.site
        holder.username.text = item.username
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    class PasswordDiffCallback : DiffUtil.ItemCallback<PasswordItem>() {
        override fun areItemsTheSame(oldItem: PasswordItem, newItem: PasswordItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PasswordItem, newItem: PasswordItem) = oldItem == newItem
    }
}
