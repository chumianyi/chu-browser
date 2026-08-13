package com.chubrowser.app.security

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.chubrowser.app.ChuBrowserApp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class CaptchaRecognizer(private val context: Context) {

    companion object {
        private const val TAG = "CaptchaRecognizer"
    }

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun isCaptchaAutoFillEnabled(): Boolean {
        return ChuBrowserApp.settingsManager.isCaptchaAutoFillEnabled()
    }

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        if (!isCaptchaAutoFillEnabled()) return@withContext ""

        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        val text = result.text
                        continuation.resume(text)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Text recognition failed: ${e.message}")
                        continuation.resume("")
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recognize text error: ${e.message}")
            ""
        }
    }

    suspend fun recognizeCaptcha(bitmap: Bitmap): String {
        val text = recognizeText(bitmap)
        return cleanCaptchaText(text)
    }

    private fun cleanCaptchaText(text: String): String {
        if (text.isBlank()) return ""

        // 移除常见的非验证码字符
        var cleaned = text.trim()

        // 移除换行和多余空格
        cleaned = cleaned.replace(Regex("""\s+"""), "")

        // 如果包含字母和数字，可能是验证码
        val alphanumeric = cleaned.filter { it.isLetterOrDigit() }

        // 验证码通常是4-8位
        if (alphanumeric.length in 4..8) {
            return alphanumeric
        }

        // 尝试提取4-8位的字母数字组合
        val matchResult = Regex("""[A-Za-z0-9]{4,8}""").find(cleaned)
        if (matchResult != null) {
            return matchResult.value
        }

        return alphanumeric.take(8)
    }

    // 检测页面中是否可能包含验证码
    fun detectCaptchaInPage(html: String): Boolean {
        val captchaIndicators = listOf(
            "captcha", "验证码", "verification code", "security code",
            "recaptcha", "hcaptcha", "turnstile", "g-recaptcha",
            "captcha_image", "verify_code", "vcode", "code_img"
        )

        val lowerHtml = html.lowercase()
        return captchaIndicators.any { indicator ->
            lowerHtml.contains(indicator.lowercase())
        }
    }

    // 自动填充验证码到剪贴板
    fun copyToClipboard(text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("captcha", text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    // 获取剪贴板内容
    fun getFromClipboard(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    // 关闭资源
    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            // 忽略
        }
    }
}
