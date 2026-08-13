package com.chumian.browser.ui.downloads

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.browser.ChuBrowserApp
import com.chumian.browser.R
import com.chumian.browser.databinding.FragmentDownloadsBinding
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private val db by lazy { ChuBrowserApp.instance.database }
    private lateinit var adapter: DownloadAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = DownloadAdapter { item ->
            if (item.status == com.chumian.browser.data.model.DownloadItem.STATUS_COMPLETED) {
                openFile(item.filePath)
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            db.downloadDao().getAll().collectLatest { downloads ->
                adapter.submitList(downloads)
                binding.emptyView.visibility = if (downloads.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun openFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(filePath))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".pdf") -> "application/pdf"
            path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) -> "image/jpeg"
            path.endsWith(".png", true) -> "image/png"
            path.endsWith(".mp4", true) -> "video/mp4"
            path.endsWith(".mp3", true) -> "audio/mpeg"
            path.endsWith(".txt", true) -> "text/plain"
            path.endsWith(".html", true) || path.endsWith(".htm", true) -> "text/html"
            else -> "*/*"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
