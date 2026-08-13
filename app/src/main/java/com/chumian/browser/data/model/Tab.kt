package com.chumian.browser.data.model

import org.mozilla.geckoview.GeckoSession

data class Tab(
    val session: GeckoSession,
    var url: String = "about:home",
    var title: String = "新标签页"
)
