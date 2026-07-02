package org.catrobat.catroid.utils.lunoscript.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.catrobat.catroid.CatroidApplication
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object LunoSecurity {

    private const val KEYSTORE_ALIAS = "NeoCatroidLunoKey"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE = 12
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    private val secretKey: SecretKey? by lazy {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            android.util.Log.e("LunoSecurity", "Failed to use AndroidKeyStore, falling back", e)
            null
        }
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
            val key = secretKey ?: throw IllegalStateException("Encryption key not available")
            val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val ciphertext = cipher.doFinal(input.toByteArray(StandardCharsets.UTF_8))
            return iv + ciphertext
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed", e)
        }
    }

    private fun decrypt(input: ByteArray): String {
        try {
            val key = secretKey ?: throw IllegalStateException("Decryption key not available")
            val iv = input.copyOfRange(0, IV_SIZE)
            val ciphertext = input.copyOfRange(IV_SIZE, input.size)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val decodedBytes = cipher.doFinal(ciphertext)
            return String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed (Wrong key or corrupted file)", e)
        }
    }
}
