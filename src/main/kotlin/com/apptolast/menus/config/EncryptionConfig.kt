package com.apptolast.menus.config

import org.springframework.context.annotation.Configuration
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_LENGTH = 128

@Configuration
class EncryptionConfig(private val appConfig: AppConfig) {

    private fun secretKey(): SecretKeySpec {
        val rawKey = appConfig.encryption.key.toByteArray(Charsets.UTF_8)
        require(rawKey.size >= 16) { "Encryption key must be at least 16 bytes" }
        val keyBytes = rawKey.copyOf(32)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptEmail(email: String): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val cipherText = cipher.doFinal(email.toByteArray(Charsets.UTF_8))
        return iv + cipherText
    }

    fun decryptEmail(encrypted: ByteArray): String {
        require(encrypted.size > GCM_IV_LENGTH) { "Invalid encrypted data" }
        val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    fun hashEmail(email: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(email.lowercase().toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
