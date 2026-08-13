package com.chumian.browser.ui.history

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.browser.ChuBrowserApp
import com.chumian.browser.databinding.FragmentHistoryBinding
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val db by lazy { ChuBrowserApp.instance.database }
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnClear.setOnClickListener { showClearDialog() }

        adapter = HistoryAdapter { item ->
            val result = Bundle().apply { putString("url", item.url) }
            parentFragmentManager.setFragmentResult("load_url", result)
            findNavController().popBackStack()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            db.historyDao().getAll().collectLatest { history ->
                adapter.submitList(history)
                binding.emptyView.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showClearDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("清除历史记录")
            .setMessage("确定要清除所有浏览历史记录吗？")
            .setPositiveButton("清除") { _, _ ->
                lifecycleScope.launch { db.historyDao().clearAll() }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
