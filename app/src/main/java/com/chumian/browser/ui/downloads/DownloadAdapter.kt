package com.chumian.browser.ui.downloads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chumian.browser.R
import com.chumian.browser.data.model.DownloadItem

class DownloadAdapter(
    private val onItemClick: (DownloadItem) -> Unit
) : ListAdapter<DownloadItem, DownloadAdapter.ViewHolder>(DownloadDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val filename: TextView = view.findViewById(R.id.filename)
        val status: TextView = view.findViewById(R.id.status)
        val progress: ProgressBar = view.findViewById(R.id.progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.filename.text = item.filename
        val progress = if (item.totalSize > 0) (item.downloadedSize * 100 / item.totalSize).toInt() else 0

        when (item.status) {
            DownloadItem.STATUS_PENDING -> {
                holder.status.text = "等待中..."
                holder.progress.visibility = View.GONE
            }
            DownloadItem.STATUS_DOWNLOADING -> {
                holder.status.text = "下载中... $progress%"
                holder.progress.visibility = View.VISIBLE
                holder.progress.progress = progress
            }
            DownloadItem.STATUS_COMPLETED -> {
                holder.status.text = "已完成 - ${item.filePath}"
                holder.progress.visibility = View.GONE
            }
            DownloadItem.STATUS_FAILED -> {
                holder.status.text = "下载失败"
                holder.progress.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    class DownloadDiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem) = oldItem == newItem
    }
}
