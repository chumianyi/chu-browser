package com.chumian.browser.security

import java.net.URI

object SecurityValidator {

    private val maliciousPatterns = listOf(
        "phishing", "malware", "ransomware", "trojan", "virus",
        "free-download", "crack", "keygen", "hack", "cheat",
        "adult-content", "gambling", "lottery", "casino"
    )

    private val suspiciousTlds = listOf(
        ".xyz", ".top", ".club", ".win", ".bid", ".stream",
        ".download", ".review", ".country", ".kim", ".cricket",
        ".science", ".work", ".party", ".gq", ".ml", ".cf", ".tk"
    )

    data class SecurityResult(
        val isSafe: Boolean,
        val reason: String,
        val riskLevel: RiskLevel
    )

    enum class RiskLevel {
        SAFE, LOW, MEDIUM, HIGH
    }

    fun validate(url: String): SecurityResult {
        val domain = extractDomain(url) ?: return SecurityResult(
            false, "无法解析域名", RiskLevel.HIGH
        )

        if (isIpAddress(domain)) {
            return SecurityResult(false, "IP地址直接访问存在安全风险", RiskLevel.MEDIUM)
        }

        if (hasSuspiciousTld(domain)) {
            return SecurityResult(false, "可疑顶级域名: ${getTld(domain)}", RiskLevel.MEDIUM)
        }

        if (containsMaliciousPattern(url)) {
            return SecurityResult(false, "URL包含恶意关键词", RiskLevel.HIGH)
        }

        if (hasTooManySubdomains(domain)) {
            return SecurityResult(false, "过多子域名，可能是钓鱼网站", RiskLevel.MEDIUM)
        }

        if (isHttp(url)) {
            return SecurityResult(true, "HTTP连接（非加密）", RiskLevel.LOW)
        }

        return SecurityResult(true, "网站安全", RiskLevel.SAFE)
    }

    private fun extractDomain(url: String): String? {
        return try {
            val cleanUrl = if (url.startsWith("http")) url else "https://$url"
            URI(cleanUrl).host?.lowercase()
        } catch (e: Exception) {
            null
        }
    }

    private fun isIpAddress(domain: String): Boolean {
        return domain.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))
    }

    private fun hasSuspiciousTld(domain: String): Boolean {
        val tld = getTld(domain)
        return suspiciousTlds.any { tld == it }
    }

    private fun getTld(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) ".${parts.last()}" else ""
    }

    private fun containsMaliciousPattern(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return maliciousPatterns.any { lowerUrl.contains(it) }
    }

    private fun hasTooManySubdomains(domain: String): Boolean {
        return domain.split(".").size > 4
    }

    private fun isHttp(url: String): Boolean {
        return url.startsWith("http://")
    }
}
