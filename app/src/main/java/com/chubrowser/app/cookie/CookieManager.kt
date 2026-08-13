package com.chubrowser.app.cookie

import android.content.Context
import com.chubrowser.app.ChuBrowserApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class CookieInfo(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val expires: Long = -1,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val sameSite: String = "Lax",
    val size: Int = 0
) {
    fun isSessionCookie(): Boolean = expires <= 0

    fun getFormattedExpires(): String {
        if (expires <= 0) return "会话结束"
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(expires * 1000))
    }
}

class CookieManager(private val context: Context) {

    companion object {
        private const val TAG = "CookieManager"
    }

    // 注意：实际Cookie操作通过GeckoView的CookieStorage API完成
    // 这里提供辅助方法和本地缓存

    private val cookieCache = mutableMapOf<String, List<CookieInfo>>()

    suspend fun getCookiesForDomain(domain: String): List<CookieInfo> =
        withContext(Dispatchers.IO) {
            cookieCache[domain] ?: emptyList()
        }

    suspend fun getAllCookies(): List<CookieInfo> = withContext(Dispatchers.IO) {
        cookieCache.values.flatten()
    }

    fun updateCookieCache(domain: String, cookies: List<CookieInfo>) {
        cookieCache[domain] = cookies
    }

    fun addCookieToCache(domain: String, cookie: CookieInfo) {
        val list = cookieCache[domain]?.toMutableList() ?: mutableListOf()
        list.removeAll { it.name == cookie.name && it.path == cookie.path }
        list.add(cookie)
        cookieCache[domain] = list
    }

    fun removeCookieFromCache(domain: String, name: String, path: String = "/") {
        val list = cookieCache[domain]?.toMutableList() ?: return
        list.removeAll { it.name == name && it.path == path }
        cookieCache[domain] = list
    }

    fun clearCookieCache() {
        cookieCache.clear()
    }

    fun clearCookiesForDomain(domain: String) {
        cookieCache.remove(domain)
    }

    // 解析Set-Cookie头
    fun parseSetCookieHeader(header: String, defaultDomain: String): CookieInfo? {
        return try {
            val parts = header.split(";").map { it.trim() }
            if (parts.isEmpty()) return null

            val nameValue = parts[0].split("=", limit = 2)
            if (nameValue.size < 2) return null

            val name = nameValue[0].trim()
            val value = nameValue[1].trim()

            var domain = defaultDomain
            var path = "/"
            var expires = -1L
            var secure = false
            var httpOnly = false
            var sameSite = "Lax"

            for (i in 1 until parts.size) {
                val part = parts[i]
                when {
                    part.startsWith("Domain=", true) -> {
                        domain = part.substringAfter("=").trim()
                    }
                    part.startsWith("Path=", true) -> {
                        path = part.substringAfter("=").trim()
                    }
                    part.startsWith("Expires=", true) -> {
                        try {
                            val dateStr = part.substringAfter("=").trim()
                            val format = java.text.SimpleDateFormat(
                                "EEE, dd MMM yyyy HH:mm:ss z",
                                java.util.Locale.US
                            )
                            expires = format.parse(dateStr)?.time?.div(1000) ?: -1
                        } catch (e: Exception) {
                            // 忽略解析错误
                        }
                    }
                    part.startsWith("Max-Age=", true) -> {
                        try {
                            val maxAge = part.substringAfter("=").trim().toLong()
                            expires = System.currentTimeMillis() / 1000 + maxAge
                        } catch (e: Exception) {
                            // 忽略
                        }
                    }
                    part.equals("Secure", true) -> secure = true
                    part.equals("HttpOnly", true) -> httpOnly = true
                    part.startsWith("SameSite=", true) -> {
                        sameSite = part.substringAfter("=").trim()
                    }
                }
            }

            CookieInfo(
                name = name,
                value = value,
                domain = domain,
                path = path,
                expires = expires,
                secure = secure,
                httpOnly = httpOnly,
                sameSite = sameSite,
                size = name.length + value.length
            )
        } catch (e: Exception) {
            null
        }
    }

    fun formatCookieForDisplay(cookie: CookieInfo): String {
        return buildString {
            appendLine("名称: ${cookie.name}")
            appendLine("值: ${cookie.value}")
            appendLine("域名: ${cookie.domain}")
            appendLine("路径: ${cookie.path}")
            appendLine("过期时间: ${cookie.getFormattedExpires()}")
            appendLine("安全: ${if (cookie.secure) "是" else "否"}")
            appendLine("HttpOnly: ${if (cookie.httpOnly) "是" else "否"}")
            appendLine("SameSite: ${cookie.sameSite}")
            appendLine("大小: ${cookie.size} 字节")
        }
    }

    fun exportCookiesToJson(cookies: List<CookieInfo>): String {
        return buildString {
            appendLine("[")
            cookies.forEachIndexed { index, cookie ->
                val json = JSONObject().apply {
                    put("name", cookie.name)
                    put("value", cookie.value)
                    put("domain", cookie.domain)
                    put("path", cookie.path)
                    put("expires", cookie.expires)
                    put("secure", cookie.secure)
                    put("httpOnly", cookie.httpOnly)
                    put("sameSite", cookie.sameSite)
                }
                append("  $json")
                if (index < cookies.size - 1) append(",")
                appendLine()
            }
            append("]")
        }
    }

    fun getCookieCount(): Int {
        return cookieCache.values.sumOf { it.size }
    }

    fun getDomainCount(): Int {
        return cookieCache.keys.size
    }
}
