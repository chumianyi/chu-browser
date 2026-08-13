package com.chumian.browser.adblock

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.BufferedReader
import java.io.InputStreamReader

class AdBlocker(private val context: Context) {

    private val blockedDomains = mutableSetOf<String>()
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        loadBlocklist()
        isInitialized = true
    }

    private fun loadBlocklist() {
        try {
            val defaultBlocklist = setOf(
                "doubleclick.net", "googlesyndication.com", "googleadservices.com",
                "adservice.google.com", "pagead2.googlesyndication.com",
                "ads.yahoo.com", "adserver.yahoo.com", "ads.pubmatic.com",
                "adservice.google.com", "google-analytics.com", "googletagmanager.com",
                "facebook.com/tr", "connect.facebook.net", "platform.twitter.com",
                "ads.linkedin.com", "analytics.twitter.com", "ads.youtube.com",
                "static.ads-twitter.com", "ads.tiktok.com", "analytics.tiktok.com",
                "scorecardresearch.com", "quantserve.com", "chartbeat.com",
                "moatads.com", "adsrvr.org", "adnxs.com", "adsymptotic.com",
                "3lift.com", "adform.net", "advertising.com", "amazon-adsystem.com",
                "apnxdgt.com", "bidswitch.net", "casalemedia.com", "cdn.krxd.net",
                "contextweb.com", "criteo.com", "demdex.net", "doubleverify.com",
                "eyeota.net", "flashtalking.com", "fraud.metomic.io", "gumgum.com",
                "imrworldwide.com", "indexww.com", "innovid.com", "ipredictive.com",
                "lijit.com", "liveintent.com", "mathtag.com", "media.net",
                "moat.com", "mookie1.com", "narrative.io", "onetag.com",
                "openx.net", "outbrain.com", "permutive.com", "pixel.quantserve.com",
                "pubmatic.com", "purch.com", "rlcdn.com", "rkdms.com",
                "rubiconproject.com", "serving-sys.com", "sharethrough.com",
                "smartadserver.com", "smrtcntnt.com", "spotxchange.com",
                "spotx.tv", "taboola.com", "tremorhub.com", "triplelift.com",
                "turn.com", "undertone.com", "uniconsent.com", "yieldmo.com",
                "yieldoptimizer.com", "zemanta.com", "adsafeprotected.com",
                "acdn.adnxs.com", "ads.avazutracking.net", "analyticsengine.s3.amazonaws.com",
                "api.segment.io", "cdn.segment.com", "collector.githubapp.com",
                "events.gfe.nvidia.com", "logs.datadoghq.com", "rum.browser-intake-datadoghq.com",
                "sentry.io", "stats.g.doubleclick.net", "tag.bounceexchange.com",
                "trc.taboola.com", "widget.outbrain.com", "www.google-analytics.com",
                "www.googletagmanager.com", "x.bidswitch.net", "youtubei.googleapis.com"
            )
            blockedDomains.addAll(defaultBlocklist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isBlocked(url: String): Boolean {
        if (!isInitialized) initialize()
        val domain = extractDomain(url) ?: return false
        return blockedDomains.any { domain == it || domain.endsWith(".$it") }
    }

    private fun extractDomain(url: String): String? {
        return try {
            val cleanUrl = if (url.startsWith("http")) url else "https://$url"
            val host = java.net.URI(cleanUrl).host ?: return null
            host.lowercase()
        } catch (e: Exception) {
            null
        }
    }

    fun configureRuntime(runtime: GeckoRuntime, enabled: Boolean) {
        val settings = runtime.settings
        settings.contentBlocking.setUseTrackingProtection(enabled)
        settings.contentBlocking.setUseSafeBrowsing(enabled)
    }
}
