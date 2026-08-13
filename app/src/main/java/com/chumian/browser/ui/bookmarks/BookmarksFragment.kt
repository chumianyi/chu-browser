package com.chumian.browser.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.browser.ChuBrowserApp
import com.chumian.browser.databinding.FragmentBookmarksBinding
import com.chumian.browser.ui.browser.BrowserViewModel
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!
    private val db by lazy { ChuBrowserApp.instance.database }
    private lateinit var adapter: BookmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = BookmarkAdapter(
            onItemClick = { bookmark ->
                val result = Bundle().apply { putString("url", bookmark.url) }
                parentFragmentManager.setFragmentResult("load_url", result)
                findNavController().popBackStack()
            },
            onDeleteClick = { bookmark ->
                lifecycleScope.launch { db.bookmarkDao().delete(bookmark) }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            db.bookmarkDao().getAll().collectLatest { bookmarks ->
                adapter.submitList(bookmarks)
                binding.emptyView.visibility = if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
