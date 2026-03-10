package com.apptolast.menus.config

import org.springframework.context.annotation.Configuration
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Configuration
class EncryptionConfig(private val appConfig: AppConfig) {

    private val secureRandom: SecureRandom = SecureRandom()

    private fun deriveKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(appConfig.encryption.key.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptEmail(email: String): ByteArray {
        val keySpec = deriveKey()
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val cipherText = cipher.doFinal(email.toByteArray(Charsets.UTF_8))
        return iv + cipherText
    }

    fun decryptEmail(encrypted: ByteArray): String {
        require(encrypted.size > GCM_IV_LENGTH) { "Encrypted data is too short to contain IV and ciphertext" }
        val keySpec = deriveKey()
        val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    fun hashEmail(email: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(email.lowercase().toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }
}
