package com.chumian.browser.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.browser.databinding.FragmentTabsBinding
import com.chumian.browser.ui.browser.BrowserViewModel
import androidx.navigation.fragment.findNavController

class TabsFragment : Fragment() {

    private var _binding: FragmentTabsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BrowserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnNewTab.setOnClickListener {
            viewModel.newTab("https://www.bing.com")
            findNavController().popBackStack()
        }

        val tabs = viewModel.tabs.value ?: mutableListOf()
        val currentIndex = viewModel.currentTabIndex.value ?: 0

        val adapter = TabAdapter(
            tabs = tabs,
            currentIndex = currentIndex,
            onItemClick = { index ->
                viewModel.selectTab(index)
                findNavController().popBackStack()
            },
            onCloseClick = { index ->
                viewModel.closeTab(index)
                val updatedTabs = viewModel.tabs.value ?: mutableListOf()
                if (updatedTabs.isEmpty()) {
                    findNavController().popBackStack()
                } else {
                    binding.recyclerView.adapter = TabAdapter(
                        updatedTabs,
                        viewModel.currentTabIndex.value ?: 0,
                        { i -> viewModel.selectTab(i); findNavController().popBackStack() },
                        { i -> viewModel.closeTab(i) }
                    )
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.emptyView.visibility = if (tabs.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
