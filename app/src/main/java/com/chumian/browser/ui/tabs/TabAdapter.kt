package com.chumian.browser.ui.tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chumian.browser.R
import com.chumian.browser.data.model.Tab

class TabAdapter(
    private val tabs: List<Tab>,
    private val currentIndex: Int,
    private val onItemClick: (Int) -> Unit,
    private val onCloseClick: (Int) -> Unit
) : RecyclerView.Adapter<TabAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val url: TextView = view.findViewById(R.id.url)
        val btnClose: ImageButton = view.findViewById(R.id.btnClose)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tab, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tab = tabs[position]
        holder.title.text = tab.title.ifEmpty { "新标签页" }
        holder.url.text = tab.url
        holder.itemView.setOnClickListener { onItemClick(position) }
        holder.btnClose.setOnClickListener { onCloseClick(position) }
    }

    override fun getItemCount() = tabs.size
}
