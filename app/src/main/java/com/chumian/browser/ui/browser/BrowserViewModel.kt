package com.chumian.browser.ui.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chumian.browser.ChuBrowserApp
import com.chumian.browser.data.model.Bookmark
import com.chumian.browser.data.model.HistoryItem
import com.chumian.browser.data.model.Tab
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as ChuBrowserApp).database
    val settings = (application as ChuBrowserApp).settingsManager

    private val _tabs = MutableLiveData<MutableList<Tab>>(mutableListOf())
    val tabs: LiveData<MutableList<Tab>> = _tabs

    private val _currentTabIndex = MutableLiveData(0)
    val currentTabIndex: LiveData<Int> = _currentTabIndex

    private val _currentUrl = MutableLiveData("")
    val currentUrl: LiveData<String> = _currentUrl

    private val _currentTitle = MutableLiveData("")
    val currentTitle: LiveData<String> = _currentTitle

    private val _loadingProgress = MutableLiveData(0)
    val loadingProgress: LiveData<Int> = _loadingProgress

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _canGoBack = MutableLiveData(false)
    val canGoBack: LiveData<Boolean> = _canGoBack

    private val _canGoForward = MutableLiveData(false)
    val canGoForward: LiveData<Boolean> = _canGoForward

    lateinit var runtime: GeckoRuntime
        private set

    fun initializeRuntime() {
        if (!::runtime.isInitialized) {
            runtime = GeckoRuntime.create(getApplication())
        }
    }

    fun newTab(url: String = "about:home") {
        val session = GeckoSession()
        session.open(runtime)
        val tab = Tab(session = session, url = url, title = "新标签页")
        _tabs.value?.add(tab)
        _currentTabIndex.value = (_tabs.value?.size ?: 1) - 1
        if (url != "about:home") {
            session.loadUri(url)
        }
        _tabs.postValue(_tabs.value)
    }

    fun closeTab(index: Int) {
        val tabs = _tabs.value ?: return
        if (index < 0 || index >= tabs.size) return
        val tab = tabs.removeAt(index)
        tab.session.close()
        if (tabs.isEmpty()) {
            newTab()
        } else {
            val newIndex = if (index >= tabs.size) tabs.size - 1 else index
            _currentTabIndex.value = newIndex
        }
        _tabs.postValue(tabs)
    }

    fun selectTab(index: Int) {
        val tabs = _tabs.value ?: return
        if (index < 0 || index >= tabs.size) return
        _currentTabIndex.value = index
        val tab = tabs[index]
        _currentUrl.value = tab.url
        _currentTitle.value = tab.title
    }

    fun getCurrentTab(): Tab? {
        val tabs = _tabs.value ?: return null
        val index = _currentTabIndex.value ?: return null
        return if (index in tabs.indices) tabs[index] else null
    }

    fun loadUrl(url: String) {
        val tab = getCurrentTab() ?: return
        val finalUrl = if (url.contains(".") && !url.contains(" ")) {
            if (url.startsWith("http")) url else "https://$url"
        } else {
            settings.getSearchUrl(url)
        }
        tab.session.loadUri(finalUrl)
        _currentUrl.value = finalUrl
    }

    fun goBack() {
        getCurrentTab()?.session?.goBack()
    }

    fun goForward() {
        getCurrentTab()?.session?.goForward()
    }

    fun reload() {
        getCurrentTab()?.session?.reload()
    }

    fun stopLoading() {
        getCurrentTab()?.session?.stop()
    }

    fun updateCurrentTab(url: String, title: String, progress: Int, loading: Boolean) {
        val tab = getCurrentTab() ?: return
        tab.url = url
        tab.title = title
        _currentUrl.value = url
        _currentTitle.value = title
        _loadingProgress.value = progress
        _isLoading.value = loading
        _tabs.postValue(_tabs.value)
    }

    fun updateNavigationState(back: Boolean, forward: Boolean) {
        _canGoBack.value = back
        _canGoForward.value = forward
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            if (db.bookmarkDao().exists(url) == 0) {
                db.bookmarkDao().insert(Bookmark(title = title, url = url))
            }
        }
    }

    fun addHistory(title: String, url: String) {
        if (settings.privacyModeEnabled) return
        viewModelScope.launch {
            db.historyDao().insert(HistoryItem(title = title, url = url))
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            db.historyDao().clearAll()
            db.bookmarkDao().clearAll()
            db.passwordDao().clearAll()
        }
    }
}
