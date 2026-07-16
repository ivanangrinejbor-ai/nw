package org.catrobat.catroid.apkbuildV3

import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ThreadLocalRandom

/**
 * Manages dynamic key generation for APK Builder V3.
 *
 * Security model:
 * 1. Generate N random candidate keys (N ~ 50).
 * 2. Compute a deterministic selector index from a seed derived from
 *    (projectNameHash + timestamp_chunk + nonce).
 * 3. The selected key encrypts the project.
 * 4. The selected key is stored in assets under a random filename
 *    (obfuscated with XOR + base64 to hinder casual inspection).
 *
 * This is NOT military-grade security (the key still ships in the APK),
 * but it raises the bar significantly compared to a single hardcoded
 * constant — every build uses a different key at a different path.
 */
object DynamicKeyManager {
    private const val TAG = "DynamicKeyManager"
    private const val CANDIDATE_COUNT = 50
    private const val KEY_LENGTH_BYTES = 32 // AES-256
    private const val XOR_MASK: Byte = 0x7A.toByte()

    /**
     * Generates a dynamic key and its metadata for a given project.
     *
     * @param projectName  Project name (used as seed input)
     * @return  KeyGenerationResult with the selected key, filename, and all candidates (for verification)
     */
    fun generateKey(projectName: String): KeyGenerationResult {
        val random = SecureRandom()
        val candidates = Array(CANDIDATE_COUNT) {
            val key = ByteArray(KEY_LENGTH_BYTES)
            random.nextBytes(key)
            key
        }

        // Deterministic selector: hash(projectName + nonce) % CANDIDATE_COUNT
        val nonce = ByteArray(8).also { random.nextBytes(it) }
        val selectorSeed = projectName.toByteArray() + nonce
        val digest = MessageDigest.getInstance("SHA-256").digest(selectorSeed)
        val selectorIndex = (digest[0].toInt() and 0xFF) % CANDIDATE_COUNT
        val selectedKey = candidates[selectorIndex]

        // Random filename: 16 hex characters + ".k3y"
        val filenameBytes = ByteArray(16).also { random.nextBytes(it) }
        val filename = filenameBytes.joinToString("") { "%02x".format(it) } + ".k3y"

        // Obfuscate the key for storage: XOR with mask + base64 encode
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

    /**
     * Resolve (re-derive) the actual key bytes from the stored obfuscated string.
     */
    fun resolveStoredKey(storedKeyString: String): ByteArray {
        val obfuscated = android.util.Base64.decode(storedKeyString, android.util.Base64.NO_WRAP)
        return obfuscated.map { (it.toInt() xor XOR_MASK.toInt()).toByte() }.toByteArray()
    }

    /**
     * Verifies that the stored key can be resolved and has the correct length.
     */
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
        /** The nonce encoded as hex (for embedding in the payload header). */
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
