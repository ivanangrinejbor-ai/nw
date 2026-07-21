package org.catrobat.catroid.apkbuildV3

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object IntegrityValidator {
    private const val TAG = "IntegrityValidator"
    private const val SHA256 = "SHA-256"

    fun validate(encryptedFile: File, key: ByteArray): Boolean {
        return try {
            val storedHash = ProjectEncryptorV3.readIntegrityHash(encryptedFile)

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

    fun streamingHash(): MessageDigest = MessageDigest.getInstance(SHA256)
}
