package org.catrobat.catroid.apkbuildV3

import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom

object DynamicKeyManager {
    private const val TAG = "DynamicKeyManager"
    private const val KEY_LENGTH_BYTES = 32
    private const val KEY_PARTS = 4

    const val KEY_FILE_PREFIX = "nk_"
    private val KEY_FILE_SUFFIX = ".nk"
    private val DECOY_COUNT = 3

    fun generateKey(projectName: String): KeyGenerationResult {
        val random = SecureRandom()
        val key = ByteArray(KEY_LENGTH_BYTES)
        random.nextBytes(key)

        val partLength = KEY_LENGTH_BYTES / KEY_PARTS
        val parts = Array(KEY_PARTS) { partIdx ->
            val part = key.copyOfRange(partIdx * partLength, (partIdx + 1) * partLength)
            val mask = ByteArray(partLength).also { random.nextBytes(it) }
            val obfuscated = ByteArray(partLength)
            for (i in 0 until partLength) {
                obfuscated[i] = (part[i].toInt() xor mask[i].toInt()).toByte()
            }
            Triple(partIdx, obfuscated, mask)
        }

        val fileNames = mutableListOf<String>()
        val fileContents = mutableListOf<String>()

        for ((partIdx, obfuscated, mask) in parts) {
            val nameBytes = ByteArray(8).also { random.nextBytes(it) }
            val randomHex = nameBytes.joinToString("") { "%02x".format(it) }
            val filename = "${KEY_FILE_PREFIX}${partIdx}_${randomHex}${KEY_FILE_SUFFIX}"
            val stored = Base64Codec.encode(obfuscated + mask)
            fileNames.add(filename)
            fileContents.add(stored)
        }

        for (i in 0 until DECOY_COUNT) {
            val nameBytes = ByteArray(8).also { random.nextBytes(it) }
            val randomHex = nameBytes.joinToString("") { "%02x".format(it) }
            val filename = "${KEY_FILE_PREFIX}decoy_${randomHex}${KEY_FILE_SUFFIX}"
            val decoy = ByteArray(KEY_LENGTH_BYTES).also { random.nextBytes(it) }
            val stored = Base64Codec.encode(decoy)
            fileNames.add(filename)
            fileContents.add(stored)
        }

        return KeyGenerationResult(
            selectedKey = key,
            keyFileNames = fileNames,
            keyFileContents = fileContents,
            realPartCount = KEY_PARTS
        )
    }

    fun resolveStoredKey(parts: List<String>): ByteArray {
        val partLength = KEY_LENGTH_BYTES / KEY_PARTS
        val key = ByteArray(KEY_LENGTH_BYTES)
        for ((idx, partContent) in parts.withIndex()) {
            if (idx >= KEY_PARTS) break
            try {
                val decoded = Base64Codec.decode(partContent)
                if (decoded.size != partLength * 2) continue
                val obfuscated = decoded.copyOfRange(0, partLength)
                val mask = decoded.copyOfRange(partLength, decoded.size)
                for (i in 0 until partLength) {
                    key[idx * partLength + i] = (obfuscated[i].toInt() xor mask[i].toInt()).toByte()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode key part", e)
            }
        }
        return key
    }

    fun verifyKeyIntegrity(parts: List<String>): Boolean {
        return try {
            val partLength = KEY_LENGTH_BYTES / KEY_PARTS
            val realParts = parts.count { partContent ->
                try {
                    val decoded = Base64Codec.decode(partContent)
                    decoded.size == partLength * 2
                } catch (e: Exception) {
                    false
                }
            }
            realParts >= KEY_PARTS
        } catch (e: Exception) {
            Log.e(TAG, "Key integrity check failed", e)
            false
        }
    }

    data class KeyGenerationResult(
        val selectedKey: ByteArray,
        val keyFileNames: List<String>,
        val keyFileContents: List<String>,
        val realPartCount: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is KeyGenerationResult) return false
            return selectedKey.contentEquals(other.selectedKey) &&
                    keyFileNames == other.keyFileNames
        }

        override fun hashCode(): Int {
            var result = selectedKey.contentHashCode()
            result = 31 * result + keyFileNames.hashCode()
            return result
        }
    }
}
