package org.catrobat.catroid.apkbuildV3

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object IntegrityValidator {
    private const val TAG = "IntegrityValidator"
    private const val SHA256 = "SHA-256"

    fun validateAgainst(encryptedFile: File, plainFile: File): Boolean {
        return try {
            val storedHash = ProjectEncryptorV3.readIntegrityHash(encryptedFile)
            val computedHash = computeSha256(plainFile)
            val match = MessageDigest.isEqual(storedHash, computedHash)
            if (!match) {
                Log.e(TAG, "Integrity mismatch!")
            }
            match
        } catch (e: Exception) {
            Log.e(TAG, "Integrity validation failed", e)
            false
        }
    }

    fun validate(encryptedFile: File, key: ByteArray): Boolean {
        return try {
            val storedHash = ProjectEncryptorV3.readIntegrityHash(encryptedFile)

            val tempFile = File.createTempFile("v3_verify_", ".tmp")
            tempFile.setReadable(true, true)
            tempFile.setWritable(true, true)
            try {
                if (!ProjectEncryptorV3.decryptAll(encryptedFile, key, tempFile)) {
                    return false
                }

                val computedHash = computeSha256(tempFile)
                val match = MessageDigest.isEqual(storedHash, computedHash)

                if (!match) {
                    Log.e(TAG, "Integrity mismatch!")
                }

                match
            } finally {
                try {
                    val zeroBuf = ByteArray(64 * 1024)
                    var remaining = tempFile.length()
                    tempFile.outputStream().use { out ->
                        while (remaining > 0) {
                            val toWrite = minOf(remaining, zeroBuf.size.toLong()).toInt()
                            out.write(zeroBuf, 0, toWrite)
                            remaining -= toWrite
                        }
                    }
                } catch (ignored: Exception) {
                }
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
