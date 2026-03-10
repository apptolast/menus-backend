package com.apptolast.menus.config

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.springframework.context.annotation.Configuration

@Configuration
class EncryptionConfig(private val appConfig: AppConfig) {

    private fun getAesKey(): SecretKeySpec {
        val keyBytes = appConfig.encryption.key.toByteArray(StandardCharsets.UTF_8)
        if (keyBytes.size != 16 && keyBytes.size != 24 && keyBytes.size != 32) {
            throw IllegalArgumentException(
                "Encryption key must be 16, 24, or 32 bytes (AES-128/192/256), but was ${keyBytes.size} bytes."
            )
        }
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptEmail(email: String): ByteArray {
        val key = getAesKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        val ciphertext = cipher.doFinal(email.toByteArray(StandardCharsets.UTF_8))
        return iv + ciphertext
    }

    fun decryptEmail(encrypted: ByteArray): String {
        // Minimum valid size: 12-byte IV + 16-byte GCM authentication tag (for empty plaintext)
        if (encrypted.size < 28) {
            throw IllegalArgumentException("Encrypted data is too short to contain IV and ciphertext.")
        }
        val key = getAesKey()
        val iv = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.copyOfRange(12, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    fun hashEmail(email: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(email.lowercase().toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
