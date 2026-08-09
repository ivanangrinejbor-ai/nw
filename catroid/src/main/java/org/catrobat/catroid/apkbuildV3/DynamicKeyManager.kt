package org.catrobat.catroid.apkbuildV3

import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ThreadLocalRandom

object DynamicKeyManager {
    private const val TAG = "DynamicKeyManager"
    private const val CANDIDATE_COUNT = 50
    private const val KEY_LENGTH_BYTES = 32
    private const val XOR_MASK: Byte = 0x7A.toByte()

    fun generateKey(projectName: String): KeyGenerationResult {
        val random = SecureRandom()
        val candidates = Array(CANDIDATE_COUNT) {
            val key = ByteArray(KEY_LENGTH_BYTES)
            random.nextBytes(key)
            key
        }

        val nonce = ByteArray(8).also { random.nextBytes(it) }
        val selectorSeed = projectName.toByteArray() + nonce
        val digest = MessageDigest.getInstance("SHA-256").digest(selectorSeed)
        val selectorIndex = (digest[0].toInt() and 0xFF) % CANDIDATE_COUNT
        val selectedKey = candidates[selectorIndex]

        val filenameBytes = ByteArray(16).also { random.nextBytes(it) }
        val filename = filenameBytes.joinToString("") { "%02x".format(it) } + ".k3y"

        val obfuscated = selectedKey.map { (it.toInt() xor XOR_MASK.toInt()).toByte() }.toByteArray()
        val storedKey = android.util.Base64.encodeToString(obfuscated, android.util.Base64.NO_WRAP)

        return KeyGenerationResult(
            selectedKey = selectedKey,
            storedKeyString = storedKey,
            keyFileName = filename,
            selectorIndex = selectorIndex,
            selectorNonce = nonce,
            candidateCount = CANDIDATE_COUNT
        )
    }

    fun resolveStoredKey(storedKeyString: String): ByteArray {
        val obfuscated = android.util.Base64.decode(storedKeyString, android.util.Base64.NO_WRAP)
        return obfuscated.map { (it.toInt() xor XOR_MASK.toInt()).toByte() }.toByteArray()
    }

    fun verifyKeyIntegrity(storedKeyString: String): Boolean {
        return try {
            val key = resolveStoredKey(storedKeyString)
            key.size == KEY_LENGTH_BYTES
        } catch (e: Exception) {
            Log.e(TAG, "Key integrity check failed", e)
            false
        }
    }

    data class KeyGenerationResult(
        val selectedKey: ByteArray,
        val storedKeyString: String,
        val keyFileName: String,
        val selectorIndex: Int,
        val selectorNonce: ByteArray,
        val candidateCount: Int
    ) {
        val selectorNonceHex: String get() = selectorNonce.joinToString("") { "%02x".format(it) }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is KeyGenerationResult) return false
            return keyFileName == other.keyFileName &&
                    selectorIndex == other.selectorIndex &&
                    selectedKey.contentEquals(other.selectedKey) &&
                    selectorNonce.contentEquals(other.selectorNonce)
        }

        override fun hashCode(): Int {
            var result = selectedKey.contentHashCode()
            result = 31 * result + keyFileName.hashCode()
            result = 31 * result + selectorIndex
            result = 31 * result + selectorNonce.contentHashCode()
            return result
        }
    }
}
