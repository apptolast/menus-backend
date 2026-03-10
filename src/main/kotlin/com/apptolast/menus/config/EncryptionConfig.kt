package com.apptolast.menus.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import java.security.SecureRandom

@Configuration
class EncryptionConfig(private val appConfig: AppConfig) {

    private val secureRandom = SecureRandom()

    /**
     * Validates that the encryption key meets minimum entropy requirements at startup.
     * The key must be at least 32 characters long to provide adequate entropy for AES-256.
     */
    @EventListener(ContextRefreshedEvent::class)
    fun validateEncryptionKey() {
        require(appConfig.encryption.key.length >= 32) {
            "app.encryption.key must be at least 32 characters long to ensure adequate entropy for AES-256"
        }
    }

    /**
     * Derives a 256-bit AES key by hashing the configured encryption key with SHA-256.
     * The configured key must be a high-entropy secret (minimum 32 characters).
     */
    private fun createAesKey(): javax.crypto.SecretKey {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(appConfig.encryption.key.toByteArray(Charsets.UTF_8))
        return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
    }

    fun encryptEmail(email: String): ByteArray {
        val key = createAesKey()
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(email.toByteArray(Charsets.UTF_8))
        return iv + ciphertext
    }

    fun decryptEmail(encrypted: ByteArray): String {
        require(encrypted.size > GCM_IV_LENGTH) { "Invalid encrypted data" }
        val key = createAesKey()
        val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    fun hashEmail(email: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(email.lowercase().toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
    }
}
