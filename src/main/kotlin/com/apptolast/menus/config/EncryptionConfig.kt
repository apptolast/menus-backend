package com.apptolast.menus.config

import org.springframework.context.annotation.Configuration

@Configuration
class EncryptionConfig(private val appConfig: AppConfig) {

    fun encryptEmail(email: String): ByteArray {
        val key = appConfig.encryption.key.padEnd(32, '0').substring(0, 32).toByteArray()
        val cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"))
        return cipher.doFinal(email.toByteArray(Charsets.UTF_8))
    }

    fun decryptEmail(encrypted: ByteArray): String {
        val key = appConfig.encryption.key.padEnd(32, '0').substring(0, 32).toByteArray()
        val cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun hashEmail(email: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(email.lowercase().toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
