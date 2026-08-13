package com.chubrowser.app.password

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.chubrowser.app.ChuBrowserApp
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PasswordManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "chu_browser_password_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        // 密码字符集
        private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        private const val NUMBERS = "0123456789"
        private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?/~`"
        private const val ALL_CHARS = UPPERCASE + LOWERCASE + NUMBERS + SYMBOLS
    }

    private val secureRandom = SecureRandom()
    private val passwordDao = ChuBrowserApp.database.passwordDao()

    init {
        try {
            ensureKeyExists()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(GCM_IV_LENGTH + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH)
            System.arraycopy(encrypted, 0, combined, GCM_IV_LENGTH, encrypted.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    fun decrypt(encryptedText: String): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // 生成强密码 - 超长随机字符串
    fun generateStrongPassword(
        length: Int = 64,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val charPool = buildString {
            if (includeUppercase) append(UPPERCASE)
            if (includeLowercase) append(LOWERCASE)
            if (includeNumbers) append(NUMBERS)
            if (includeSymbols) append(SYMBOLS)
        }.ifEmpty { ALL_CHARS }

        val password = StringBuilder(length)
        for (i in 0 until length) {
            password.append(charPool[secureRandom.nextInt(charPool.length)])
        }

        // 确保至少包含每种类型的字符
        if (includeUppercase && length >= 4) {
            password[secureRandom.nextInt(length)] = UPPERCASE[secureRandom.nextInt(UPPERCASE.length)]
        }
        if (includeLowercase && length >= 4) {
            password[secureRandom.nextInt(length)] = LOWERCASE[secureRandom.nextInt(LOWERCASE.length)]
        }
        if (includeNumbers && length >= 4) {
            password[secureRandom.nextInt(length)] = NUMBERS[secureRandom.nextInt(NUMBERS.length)]
        }
        if (includeSymbols && length >= 4) {
            password[secureRandom.nextInt(length)] = SYMBOLS[secureRandom.nextInt(SYMBOLS.length)]
        }

        return password.toString()
    }

    // 生成超长密码（256位）
    fun generateUltraLongPassword(): String = generateStrongPassword(256)

    // 生成512位密码
    fun generateMegaPassword(): String = generateStrongPassword(512)

    // 计算密码强度
    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.EMPTY

        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 16) score++
        if (password.length >= 32) score++
        if (password.length >= 64) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.MEDIUM
            score <= 6 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }

    // 保存密码
    suspend fun savePassword(
        url: String,
        username: String,
        password: String,
        title: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val domain = extractDomain(url)
        val existing = passwordDao.findExisting(domain, username)
        if (existing != null) {
            val updated = existing.copy(
                url = url,
                username = username,
                encryptedPassword = encrypt(password),
                title = title,
                updatedAt = System.currentTimeMillis()
            )
            passwordDao.update(updated)
            existing.id
        } else {
            val entity = PasswordEntity(
                url = url,
                domain = domain,
                username = username,
                encryptedPassword = encrypt(password),
                title = title
            )
            passwordDao.insert(entity)
        }
    }

    // 获取域名密码
    suspend fun getPasswordsForDomain(domain: String): List<PasswordEntity> =
        withContext(Dispatchers.IO) {
            passwordDao.getByDomain(domain)
        }

    // 获取所有密码
    suspend fun getAllPasswords(): List<PasswordEntity> = withContext(Dispatchers.IO) {
        passwordDao.getAll()
    }

    // 搜索密码
    suspend fun searchPasswords(query: String): List<PasswordEntity> =
        withContext(Dispatchers.IO) {
            passwordDao.search(query)
        }

    // 删除密码
    suspend fun deletePassword(id: Long) = withContext(Dispatchers.IO) {
        passwordDao.deleteById(id)
    }

    // 删除所有密码
    suspend fun deleteAllPasswords() = withContext(Dispatchers.IO) {
        passwordDao.deleteAll()
    }

    // 获取密码数量
    suspend fun getPasswordCount(): Int = withContext(Dispatchers.IO) {
        passwordDao.getCount()
    }

    // 提取域名
    fun extractDomain(url: String): String {
        return try {
            val cleanUrl = if (url.startsWith("http")) url else "https://$url"
            val host = java.net.URI(cleanUrl).host ?: ""
            host.removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }

    // 主密码哈希
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    // 验证主密码
    fun verifyMasterPassword(password: String): Boolean {
        val storedHash = ChuBrowserApp.settingsManager.getMasterPasswordHash()
        if (storedHash.isEmpty()) return true
        return hashPassword(password) == storedHash
    }
}

enum class PasswordStrength(val label: String, val color: String) {
    EMPTY("空", "#9E9E9E"),
    WEAK("弱", "#F44336"),
    MEDIUM("中", "#FF9800"),
    STRONG("强", "#4CAF50"),
    VERY_STRONG("非常强", "#2E7D32")
}
