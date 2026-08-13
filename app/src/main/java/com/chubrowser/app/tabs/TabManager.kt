package com.chubrowser.app.tabs

import com.chubrowser.app.ChuBrowserApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

data class Tab(
    val id: String,
    val session: GeckoSession,
    var title: String = "",
    var url: String = "",
    var favicon: String = "",
    var isPrivate: Boolean = false,
    var isLoading: Boolean = false,
    var progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var lastAccessed: Long = System.currentTimeMillis()
)

class TabManager {

    companion object {
        private const val TAG = "TabManager"
        private const val MAX_TABS = 50
    }

    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs

    private val _currentTabIndex = MutableStateFlow(-1)
    val currentTabIndex: StateFlow<Int> = _currentTabIndex

    fun createTab(
        url: String = "",
        isPrivate: Boolean = false,
        select: Boolean = true
    ): Tab {
        if (_tabs.value.size >= MAX_TABS) {
            // 关闭最旧的标签页
            val oldest = _tabs.value.minByOrNull { it.lastAccessed }
            if (oldest != null) closeTab(oldest.id)
        }

        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate)
            .build()

        val session = GeckoSession(settings)
        val tabId = "tab_${System.currentTimeMillis()}_${_tabs.value.size}"

        val tab = Tab(
            id = tabId,
            session = session,
            url = url,
            isPrivate = isPrivate
        )

        val newTabs = _tabs.value.toMutableList()
        newTabs.add(tab)
        _tabs.value = newTabs

        if (select) {
            selectTab(tabId)
        }

        if (url.isNotBlank()) {
            session.loadUri(url)
        }

        return tab
    }

    fun closeTab(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        val index = _tabs.value.indexOfFirst { it.id == tabId }

        try {
            tab.session.close()
        } catch (e: Exception) {
            // 忽略
        }

        val newTabs = _tabs.value.toMutableList()
        newTabs.removeAt(index)
        _tabs.value = newTabs

        // 如果关闭的是当前标签页，选择相邻的
        if (_currentTabIndex.value == index) {
            if (newTabs.isNotEmpty()) {
                val newIndex = if (index < newTabs.size) index else newTabs.size - 1
                _currentTabIndex.value = newIndex
            } else {
                _currentTabIndex.value = -1
            }
        } else if (_currentTabIndex.value > index) {
            _currentTabIndex.value -= 1
        }
    }

    fun selectTab(tabId: String) {
        val index = _tabs.value.indexOfFirst { it.id == tabId }
        if (index >= 0) {
            _currentTabIndex.value = index
            val tab = _tabs.value[index]
            tab.lastAccessed = System.currentTimeMillis()
        }
    }

    fun getCurrentTab(): Tab? {
        val index = _currentTabIndex.value
        return if (index >= 0 && index < _tabs.value.size) {
            _tabs.value[index]
        } else null
    }

    fun getTab(tabId: String): Tab? {
        return _tabs.value.find { it.id == tabId }
    }

    fun getTabCount(): Int = _tabs.value.size

    fun getNormalTabs(): List<Tab> = _tabs.value.filter { !it.isPrivate }

    fun getPrivateTabs(): List<Tab> = _tabs.value.filter { it.isPrivate }

    fun closeAllTabs() {
        _tabs.value.forEach { tab ->
            try {
                tab.session.close()
            } catch (e: Exception) {
                // 忽略
            }
        }
        _tabs.value = emptyList()
        _currentTabIndex.value = -1
    }

    fun closePrivateTabs() {
        val privateTabs = _tabs.value.filter { it.isPrivate }
        privateTabs.forEach { tab ->
            try {
                tab.session.close()
            } catch (e: Exception) {
                // 忽略
            }
        }
        val normalTabs = _tabs.value.filter { !it.isPrivate }
        _tabs.value = normalTabs

        if (_currentTabIndex.value >= normalTabs.size) {
            _currentTabIndex.value = if (normalTabs.isNotEmpty()) normalTabs.size - 1 else -1
        }
    }

    fun updateTab(tabId: String, updates: Tab.() -> Tab) {
        val index = _tabs.value.indexOfFirst { it.id == tabId }
        if (index >= 0) {
            val newTabs = _tabs.value.toMutableList()
            newTabs[index] = newTabs[index].updates()
            _tabs.value = newTabs
        }
    }

    fun moveTab(from: Int, to: Int) {
        if (from < 0 || from >= _tabs.value.size || to < 0 || to >= _tabs.value.size) return
        val newTabs = _tabs.value.toMutableList()
        val tab = newTabs.removeAt(from)
        newTabs.add(to, tab)
        _tabs.value = newTabs

        // 更新当前索引
        if (_currentTabIndex.value == from) {
            _currentTabIndex.value = to
        } else if (_currentTabIndex.value in (from + 1)..to) {
            _currentTabIndex.value -= 1
        } else if (_currentTabIndex.value in to until from) {
            _currentTabIndex.value += 1
        }
    }

    fun duplicateTab(tabId: String): Tab? {
        val tab = getTab(tabId) ?: return null
        return createTab(url = tab.url, isPrivate = tab.isPrivate, select = true)
    }

    fun reloadCurrentTab() {
        getCurrentTab()?.session?.reload()
    }

    fun goBack() {
        getCurrentTab()?.session?.goBack()
    }

    fun goForward() {
        getCurrentTab()?.session?.goForward()
    }

    fun stopLoading() {
        getCurrentTab()?.session?.stop()
    }

    fun canGoBack(): Boolean {
        return getCurrentTab()?.session?.canGoBack ?: false
    }

    fun canGoForward(): Boolean {
        return getCurrentTab()?.session?.canGoForward ?: false
    }
}
