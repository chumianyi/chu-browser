package com.chumian.browser.ui.bookmarks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chumian.browser.R
import com.chumian.browser.data.model.Bookmark

class BookmarkAdapter(
    private val onItemClick: (Bookmark) -> Unit,
    private val onDeleteClick: (Bookmark) -> Unit
) : ListAdapter<Bookmark, BookmarkAdapter.ViewHolder>(BookmarkDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val url: TextView = view.findViewById(R.id.url)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = getItem(position)
        holder.title.text = bookmark.title
        holder.url.text = bookmark.url
        holder.itemView.setOnClickListener { onItemClick(bookmark) }
        holder.btnDelete.setOnClickListener { onDeleteClick(bookmark) }
    }

    class BookmarkDiffCallback : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark) = oldItem == newItem
    }
}
