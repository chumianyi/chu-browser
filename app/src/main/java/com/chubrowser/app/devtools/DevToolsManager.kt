package com.chubrowser.app.devtools

import android.content.Context
import com.chubrowser.app.ChuBrowserApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class ConsoleMessage(
    val level: String,
    val message: String,
    val source: String = "",
    val lineNumber: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class NetworkRequest(
    val url: String,
    val method: String,
    val statusCode: Int = 0,
    val contentType: String = "",
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val requestBody: String = "",
    val responseBody: String = "",
    val startTime: Long = 0,
    val endTime: Long = 0,
    val size: Long = 0
) {
    val duration: Long get() = if (endTime > 0) endTime - startTime else 0
}

class DevToolsManager(private val context: Context) {

    companion object {
        private const val TAG = "DevToolsManager"
        private const val MAX_CONSOLE_MESSAGES = 1000
        private const val MAX_NETWORK_REQUESTS = 500
    }

    private val consoleMessages = mutableListOf<ConsoleMessage>()
    private val networkRequests = mutableListOf<NetworkRequest>()
    private val breakpoints = mutableSetOf<String>()

    var isDevToolsEnabled: Boolean = false
        private set

    fun setDevToolsEnabled(enabled: Boolean) {
        isDevToolsEnabled = enabled
    }

    fun isRemoteDebuggingEnabled(): Boolean {
        return ChuBrowserApp.settingsManager.isRemoteDebuggingEnabled()
    }

    // 控制台
    fun addConsoleMessage(level: String, message: String, source: String = "", line: Int = 0) {
        if (!isDevToolsEnabled) return
        val msg = ConsoleMessage(level = level, message = message, source = source, lineNumber = line)
        consoleMessages.add(msg)
        if (consoleMessages.size > MAX_CONSOLE_MESSAGES) {
            consoleMessages.removeAt(0)
        }
    }

    fun getConsoleMessages(): List<ConsoleMessage> = consoleMessages.toList()

    fun getConsoleMessagesByLevel(level: String): List<ConsoleMessage> =
        consoleMessages.filter { it.level.equals(level, true) }

    fun clearConsole() {
        consoleMessages.clear()
    }

    // 网络请求
    fun addNetworkRequest(request: NetworkRequest) {
        if (!isDevToolsEnabled) return
        networkRequests.add(request)
        if (networkRequests.size > MAX_NETWORK_REQUESTS) {
            networkRequests.removeAt(0)
        }
    }

    fun updateNetworkRequest(url: String, updates: NetworkRequest.() -> NetworkRequest) {
        val index = networkRequests.indexOfFirst { it.url == url }
        if (index >= 0) {
            networkRequests[index] = networkRequests[index].updates()
        }
    }

    fun getNetworkRequests(): List<NetworkRequest> = networkRequests.toList()

    fun getNetworkRequestsByType(contentType: String): List<NetworkRequest> =
        networkRequests.filter { it.contentType.contains(contentType, true) }

    fun clearNetworkRequests() {
        networkRequests.clear()
    }

    // 断点
    fun addBreakpoint(url: String, line: Int) {
        breakpoints.add("$url:$line")
    }

    fun removeBreakpoint(url: String, line: Int) {
        breakpoints.remove("$url:$line")
    }

    fun getBreakpoints(): Set<String> = breakpoints.toSet()

    fun clearBreakpoints() {
        breakpoints.clear()
    }

    fun isBreakpointHit(url: String, line: Int): Boolean {
        return breakpoints.contains("$url:$line")
    }

    // 执行JavaScript
    suspend fun evaluateJavaScript(script: String): String = withContext(Dispatchers.Default) {
        // 实际执行通过GeckoSession.evaluateJS完成
        // 这里返回占位，实际在MainActivity中实现
        "// JavaScript execution handled by GeckoSession"
    }

    // 查看页面源代码
    suspend fun getPageSource(): String = withContext(Dispatchers.Default) {
        // 实际通过GeckoSession获取
        "// Page source retrieval handled by GeckoSession"
    }

    // 查看DOM
    suspend fun getDOM(): String = withContext(Dispatchers.Default) {
        "// DOM retrieval handled by GeckoSession"
    }

    // 性能分析
    data class PerformanceMetrics(
        val domContentLoaded: Long = 0,
        val loadEvent: Long = 0,
        val firstPaint: Long = 0,
        val firstContentfulPaint: Long = 0,
        val totalResources: Int = 0,
        val totalSize: Long = 0,
        val requests: Int = 0
    )

    fun getPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            totalResources = networkRequests.size,
            totalSize = networkRequests.sumOf { it.size },
            requests = networkRequests.size
        )
    }

    // 存储检查
    data class StorageInfo(
        val localStorage: Map<String, String> = emptyMap(),
        val sessionStorage: Map<String, String> = emptyMap(),
        val indexedDB: List<String> = emptyList(),
        val cacheStorage: List<String> = emptyList()
    )

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.Default) {
        StorageInfo()
    }

    // 导出日志
    fun exportConsoleLog(): String {
        return buildString {
            appendLine("=== Chu浏览器开发者工具 - 控制台日志 ===")
            appendLine("导出时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine()
            consoleMessages.forEach { msg ->
                val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                    .format(java.util.Date(msg.timestamp))
                appendLine("[$time] [${msg.level.uppercase()}] ${msg.message}")
                if (msg.source.isNotBlank()) {
                    appendLine("  at ${msg.source}:${msg.lineNumber}")
                }
            }
        }
    }

    fun exportNetworkLog(): String {
        return buildString {
            appendLine("=== Chu浏览器开发者工具 - 网络请求日志 ===")
            appendLine("导出时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine()
            networkRequests.forEachIndexed { index, req ->
                appendLine("--- 请求 #${index + 1} ---")
                appendLine("URL: ${req.url}")
                appendLine("方法: ${req.method}")
                appendLine("状态码: ${req.statusCode}")
                appendLine("Content-Type: ${req.contentType}")
                appendLine("大小: ${req.size} bytes")
                appendLine("耗时: ${req.duration}ms")
                appendLine()
            }
        }
    }

    fun clearAll() {
        clearConsole()
        clearNetworkRequests()
        clearBreakpoints()
    }

    fun saveLogsToFile(): File? {
        return try {
            val logDir = File(context.filesDir, "devtools_logs")
            if (!logDir.exists()) logDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val consoleFile = File(logDir, "console_$timestamp.log")
            consoleFile.writeText(exportConsoleLog())

            val networkFile = File(logDir, "network_$timestamp.log")
            networkFile.writeText(exportNetworkLog())

            consoleFile
        } catch (e: Exception) {
            null
        }
    }
}
