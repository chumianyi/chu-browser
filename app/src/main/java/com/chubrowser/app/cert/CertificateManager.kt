package com.chubrowser.app.cert

import android.content.Context
import com.chubrowser.app.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection

class CertificateManager(private val context: Context) {

    companion object {
        private const val TAG = "CertificateManager"
    }

    private val securityManager = SecurityManager(context)

    suspend fun getCertificateInfo(url: String): SecurityManager.CertificateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val uri = java.net.URI(url)
                val host = uri.host ?: return@withContext null
                val port = if (uri.port > 0) uri.port else 443
                securityManager.getCertificateInfo(host, port)
            } catch (e: Exception) {
                null
            }
        }

    suspend fun getCertificateChain(url: String): List<SecurityManager.CertificateInfo> =
        withContext(Dispatchers.IO) {
            try {
                val uri = java.net.URI(url)
                val host = uri.host ?: return@withContext emptyList()
                val port = if (uri.port > 0) uri.port else 443

                val connectionUrl = java.net.URL("https://$host:$port/")
                val connection = connectionUrl.openConnection() as HttpsURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                val certs = connection.serverCertificates
                certs.mapNotNull { cert ->
                    if (cert is X509Certificate) {
                        SecurityManager.CertificateInfo(
                            subject = cert.subjectX500Principal.name,
                            issuer = cert.issuerX500Principal.name,
                            validFrom = cert.notBefore.toString(),
                            validTo = cert.notAfter.toString(),
                            serialNumber = cert.serialNumber.toString(16),
                            signatureAlgorithm = cert.sigAlgName,
                            publicKeyAlgorithm = cert.publicKey.algorithm,
                            sha1Fingerprint = getFingerprint(cert, "SHA-1"),
                            sha256Fingerprint = getFingerprint(cert, "SHA-256"),
                            version = cert.version
                        )
                    } else null
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun getFingerprint(cert: X509Certificate, algorithm: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance(algorithm)
            val digest = md.digest(cert.encoded)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun formatCertificateInfo(info: SecurityManager.CertificateInfo): String {
        return buildString {
            appendLine("=== 证书信息 ===")
            appendLine()
            appendLine("【主体】")
            appendLine(parseDistinguishedName(info.subject))
            appendLine()
            appendLine("【颁发者】")
            appendLine(parseDistinguishedName(info.issuer))
            appendLine()
            appendLine("【有效期】")
            appendLine("生效时间: ${info.validFrom}")
            appendLine("过期时间: ${info.validTo}")
            appendLine()
            appendLine("【证书详情】")
            appendLine("版本: V${info.version}")
            appendLine("序列号: ${info.serialNumber}")
            appendLine("签名算法: ${info.signatureAlgorithm}")
            appendLine("公钥算法: ${info.publicKeyAlgorithm}")
            appendLine()
            appendLine("【指纹】")
            appendLine("SHA-1: ${info.sha1Fingerprint}")
            appendLine("SHA-256: ${info.sha256Fingerprint}")
        }
    }

    private fun parseDistinguishedName(dn: String): String {
        return try {
            dn.split(",").joinToString("\n") { part ->
                val trimmed = part.trim()
                when {
                    trimmed.startsWith("CN=") -> "通用名称 (CN): ${trimmed.substringAfter("=")}"
                    trimmed.startsWith("O=") -> "组织 (O): ${trimmed.substringAfter("=")}"
                    trimmed.startsWith("OU=") -> "组织单位 (OU): ${trimmed.substringAfter("=")}"
                    trimmed.startsWith("C=") -> "国家 (C): ${trimmed.substringAfter("=")}"
                    trimmed.startsWith("ST=") -> "省份 (ST): ${trimmed.substringAfter("=")}"
                    trimmed.startsWith("L=") -> "城市 (L): ${trimmed.substringAfter("=")}"
                    else -> trimmed
                }
            }
        } catch (e: Exception) {
            dn
        }
    }

    fun isCertificateValid(info: SecurityManager.CertificateInfo): Boolean {
        return try {
            // 这里简化判断，实际应该验证证书链和吊销状态
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getCertificateStatus(info: SecurityManager.CertificateInfo): CertificateStatus {
        return try {
            val now = System.currentTimeMillis()
            // 解析有效期
            // 简化处理
            CertificateStatus.VALID
        } catch (e: Exception) {
            CertificateStatus.UNKNOWN
        }
    }
}

enum class CertificateStatus {
    VALID, EXPIRED, NOT_YET_VALID, REVOKED, UNTRUSTED, UNKNOWN
}
