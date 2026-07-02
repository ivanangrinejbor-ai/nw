package org.catrobat.catroid.utils.lunoscript.security

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object LunoSecurity {

    private const val KEY_RAW = "NeoCatroidSecure"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE = 12

    private val secretKey: SecretKeySpec by lazy {
        val keyBytes = KEY_RAW.toByteArray(StandardCharsets.UTF_8)
        val key = ByteArray(32)
        for (i in keyBytes.indices) key[i] = keyBytes[i]
        SecretKeySpec(key, "AES")
    }

    fun saveEncrypted(file: File, code: String) {
        val encryptedBytes = encrypt(code)
        file.writeBytes(encryptedBytes)
    }

    fun loadDecrypted(file: File): String {
        val bytes = file.readBytes()
        return decrypt(bytes)
    }

    private fun encrypt(input: String): ByteArray {
        try {
            val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val ciphertext = cipher.doFinal(input.toByteArray(StandardCharsets.UTF_8))
            return iv + ciphertext
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed", e)
        }
    }

    private fun decrypt(input: ByteArray): String {
        try {
            val iv = input.copyOfRange(0, IV_SIZE)
            val ciphertext = input.copyOfRange(IV_SIZE, input.size)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val decodedBytes = cipher.doFinal(ciphertext)
            return String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed (Wrong key or corrupted file)", e)
        }
    }
}