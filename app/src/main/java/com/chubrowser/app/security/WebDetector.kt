package com.chubrowser.app.security

import android.content.Context
import com.chubrowser.app.ChuBrowserApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebDetector(private val context: Context) {

    companion object {
        private const val TAG = "WebDetector"

        // 已知恶意网站域名（示例列表）
        private val MALICIOUS_DOMAINS = setOf(
            "malware.example.com",
            "phishing.example.com",
            "scam.example.com",
            "virus.example.com",
            "trojan.example.com",
            "ransomware.example.com"
        )

        // 危险文件类型
        private val DANGEROUS_EXTENSIONS = setOf(
            ".exe", ".bat", ".cmd", ".vbs", ".js", ".jse",
            ".wsf", ".wsh", ".ps1", ".psm1", ".scr", ".cpl",
            ".jar", ".apk", ".msi", ".dll", ".sys", ".drv"
        )

        // 钓鱼网站常见特征
        private val PHISHING_INDICATORS = listOf(
            "verify your account",
            "account suspended",
            "security alert",
            "password expired",
            "login to continue",
            "confirm your identity",
            "unusual activity detected",
            "your account will be closed"
        )
    }

    private val securityManager = SecurityManager(context)

    data class DetectionResult(
        val url: String,
        val isMalicious: Boolean,
        val threatType: ThreatType,
        val reason: String,
        val confidence: Int,
        val shouldBlock: Boolean
    )

    enum class ThreatType {
        NONE, MALWARE, PHISHING, SCAM, DANGEROUS_DOWNLOAD, SUSPICIOUS, UNKNOWN
    }

    suspend fun detect(url: String): DetectionResult = withContext(Dispatchers.IO) {
        if (!ChuBrowserApp.settingsManager.isWebDetectorEnabled()) {
            return@withContext DetectionResult(
                url = url,
                isMalicious = false,
                threatType = ThreatType.NONE,
                reason = "网页检测已关闭",
                confidence = 0,
                shouldBlock = false
            )
        }

        val lowerUrl = url.lowercase()

        // 检查已知恶意域名
        try {
            val host = java.net.URI(lowerUrl).host ?: ""
            if (MALICIOUS_DOMAINS.contains(host)) {
                return@withContext DetectionResult(
                    url = url,
                    isMalicious = true,
                    threatType = ThreatType.MALWARE,
                    reason = "已知恶意网站域名",
                    confidence = 95,
                    shouldBlock = true
                )
            }
        } catch (e: Exception) {
            // 忽略
        }

        // 检查危险文件下载
        val dangerousExt = DANGEROUS_EXTENSIONS.firstOrNull { ext ->
            lowerUrl.contains(ext) && !isTrustedDomain(lowerUrl)
        }
        if (dangerousExt != null) {
            return@withContext DetectionResult(
                url = url,
                isMalicious = true,
                threatType = ThreatType.DANGEROUS_DOWNLOAD,
                reason = "检测到可疑的可执行文件下载: $dangerousExt",
                confidence = 70,
                shouldBlock = false // 不直接拦截，提示用户
            )
        }

        // 检查钓鱼特征
        val phishingMatch = PHISHING_INDICATORS.firstOrNull { indicator ->
            lowerUrl.contains(indicator.replace(" ", "-")) ||
            lowerUrl.contains(indicator.replace(" ", "_"))
        }
        if (phishingMatch != null) {
            return@withContext DetectionResult(
                url = url,
                isMalicious = true,
                threatType = ThreatType.PHISHING,
                reason = "URL包含钓鱼网站特征: $phishingMatch",
                confidence = 60,
                shouldBlock = false
            )
        }

        // 使用SecurityManager进行深度检测
        val securityResult = securityManager.checkUrl(url)
        if (!securityResult.isSafe && securityResult.threats.isNotEmpty()) {
            val threat = securityResult.threats.first()
            return@withContext DetectionResult(
                url = url,
                isMalicious = true,
                threatType = when {
                    threat.contains("钓鱼", true) -> ThreatType.PHISHING
                    threat.contains("恶意", true) -> ThreatType.MALWARE
                    threat.contains("HTTPS", true) -> ThreatType.SUSPICIOUS
                    else -> ThreatType.UNKNOWN
                },
                reason = threat,
                confidence = securityResult.score,
                shouldBlock = securityResult.score < 40
            )
        }

        DetectionResult(
            url = url,
            isMalicious = false,
            threatType = ThreatType.NONE,
            reason = "未检测到威胁",
            confidence = 100,
            shouldBlock = false
        )
    }

    private fun isTrustedDomain(url: String): Boolean {
        val trustedDomains = listOf(
            "google.com", "microsoft.com", "apple.com", "amazon.com",
            "github.com", "gitlab.com", "bitbucket.org", "sourceforge.net",
            "play.google.com", "apps.apple.com", "f-droid.org",
            "baidu.com", "alibaba.com", "tencent.com", "bytedance.com"
        )
        return try {
            val host = java.net.URI(url).host ?: ""
            trustedDomains.any { host == it || host.endsWith(".$it") }
        } catch (e: Exception) {
            false
        }
    }

    fun shouldBlockQuick(url: String): Boolean {
        if (!ChuBrowserApp.settingsManager.isWebDetectorEnabled()) return false
        return try {
            val host = java.net.URI(url).host ?: ""
            MALICIOUS_DOMAINS.contains(host)
        } catch (e: Exception) {
            false
        }
    }

    fun getThreatLevel(result: DetectionResult): String {
        return when (result.threatType) {
            ThreatType.NONE -> "安全"
            ThreatType.MALWARE -> "恶意软件"
            ThreatType.PHISHING -> "钓鱼网站"
            ThreatType.SCAM -> "诈骗网站"
            ThreatType.DANGEROUS_DOWNLOAD -> "危险下载"
            ThreatType.SUSPICIOUS -> "可疑网站"
            ThreatType.UNKNOWN -> "未知威胁"
        }
    }
}
