package org.catrobat.catroid.apkbuildV3

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Validates the integrity of an encrypted project payload.
 *
 * Workflow:
 * 1. Extract the stored SHA-256 hash from the encrypted file header.
 * 2. Decrypt and compute SHA-256 of the plaintext.
 * 3. Compare — a mismatch means corruption or tampering.
 */
object IntegrityValidator {
    private const val TAG = "IntegrityValidator"
    private const val SHA256 = "SHA-256"

    /**
     * Validates that the decrypted content matches the embedded integrity hash.
     *
     * @param encryptedFile  The V3 encrypted file
     * @param key            AES-256 key bytes
     * @return  true if integrity check passes
     */
    fun validate(encryptedFile: File, key: ByteArray): Boolean {
        return try {
            val storedHash = ProjectEncryptorV3.readIntegrityHash(encryptedFile)

            // Decrypt the entire file to a temp location for hashing
            val tempFile = File.createTempFile("v3_verify_", ".tmp")
            try {
                if (!ProjectEncryptorV3.decryptAll(encryptedFile, key, tempFile)) {
                    return false
                }

                val computedHash = computeSha256(tempFile)
                val match = storedHash.contentEquals(computedHash)

                if (!match) {
                    Log.e(TAG, "Integrity mismatch! Stored=${storedHash.joinToString("") { "%02x".format(it) }}, " +
                            "Computed=${computedHash.joinToString("") { "%02x".format(it) }}")
                }

                match
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Integrity validation failed", e)
            false
        }
    }

    /**
     * Computes SHA-256 hash of a file in streaming fashion (low memory).
     */
    fun computeSha256(file: File): ByteArray {
        val digest = MessageDigest.getInstance(SHA256)
        val buffer = ByteArray(64 * 1024)
        FileInputStream(file).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                if (bytesRead > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
        }
        return digest.digest()
    }

    /**
     * Streaming hash for use during encryption (avoids reading the file twice).
     */
    fun streamingHash(): MessageDigest = MessageDigest.getInstance(SHA256)
}
