package org.catrobat.catroid.apkbuildV3

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Streaming encryptor/decryptor for APK Builder V3.
 *
 * Format (V3 payload):
 *   [magic: 4 bytes = "NCV3"]
 *   [version: 2 bytes, unsigned short = 1]
 *   [flags: 2 bytes]
 *   [reserved: 4 bytes]
 *   [totalChunks: 4 bytes (big-endian int)]
 *   [integrityHash: 32 bytes = SHA-256 of plaintext]
 *   [chunkCount map: totalChunks * (chunkIndex:4 + offset:8 + encryptedLen:4)]
 *   [...chunks...]
 *
 * Each chunk:
 *   [iv: 12 bytes]
 *   [encrypted data: N bytes (incl. 16-byte GCM tag)]
 *
 * This format supports random-access decryption (needed for Light Template)
 * because each chunk can be decrypted independently.
 */
object ProjectEncryptorV3 {
    private const val TAG = "ProjectEncryptorV3"
    private val MAGIC = byteArrayOf('N'.code.toByte(), 'C'.code.toByte(), 'V'.code.toByte(), '3'.code.toByte())
    private const val FORMAT_VERSION: Short = 1
    private const val CHUNK_SIZE = 1024 * 1024 // 1 MB per chunk
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_LENGTH = 128
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val SHA256 = "SHA-256"

    /** Header size before chunk data (fixed). */
    const val HEADER_SIZE = 4 + 2 + 2 + 4 + 4 + 32

    /**
     * Encrypts a source file into the V3 encrypted format.
     * Uses streaming to keep memory usage low.
     *
     * @param sourceFile  Plaintext file to encrypt
     * @param destFile    Output encrypted file
     * @param key         AES-256 key bytes
     * @param onProgress  Optional progress callback (0.0 - 1.0)
     */
    fun encrypt(
        sourceFile: File,
        destFile: File,
        key: ByteArray,
        onProgress: ((Float) -> Unit)? = null
    ) {
        val aesKey = SecretKeySpec(key, "AES")
        val totalSize = sourceFile.length()
        val totalChunks = ((totalSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        val chunkOffsets = IntArray(totalChunks) // relative to chunk data start
        val chunkEncLens = IntArray(totalChunks)

        destFile.parentFile?.mkdirs()

        FileOutputStream(destFile).use { out ->
            // Write header placeholder
            val headerSizePos = writeHeaderPlaceholder(out, totalChunks)

            // Compute SHA-256 of plaintext by streaming the source file so the whole
            // (possibly very large) project is never buffered in memory at once.
            val digest = java.security.MessageDigest.getInstance(SHA256)
            FileInputStream(sourceFile).use { hashIn ->
                val hashBuf = ByteArray(64 * 1024)
                var hashN: Int
                while (hashIn.read(hashBuf).also { hashN = it } != -1) {
                    digest.update(hashBuf, 0, hashN)
                }
            }
            val hash = digest.digest()

            // Write integrity hash
            out.write(hash)

                // Write chunk index table placeholder
                val chunkTablePos = out.channel.position()
                for (i in 0 until totalChunks) {
                    writeInt(out, 0)    // chunk index
                    writeLong(out, 0L)  // offset
                    writeInt(out, 0)    // encrypted length
                }

                // Process chunks
                FileInputStream(sourceFile).use { sourceIn ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var bytesRead: Int
                    var chunkIndex = 0
                    var dataStartPos = out.channel.position()

                    while (sourceIn.read(buffer).also { bytesRead = it } != -1) {
                        val cipher = Cipher.getInstance(ALGORITHM)
                        val iv = ByteArray(GCM_IV_SIZE).also { SecureRandom().nextBytes(it) }
                        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

                        val plaintext = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                        val ciphertext = cipher.doFinal(plaintext)

                        // Write chunk: [iv:12][ciphertext:N]
                        val chunkStart = out.channel.position()
                        out.write(iv)
                        out.write(ciphertext)

                        val chunkEnd = out.channel.position()
                        chunkOffsets[chunkIndex] = (chunkStart - dataStartPos).toInt()
                        chunkEncLens[chunkIndex] = (chunkEnd - chunkStart).toInt()
                        chunkIndex++

                        onProgress?.invoke(chunkIndex.toFloat() / totalChunks)
                    }

                    // Go back and write the actual chunk table
                    val finalDataEnd = out.channel.position()
                    out.channel.position(chunkTablePos)
                    for (i in 0 until totalChunks) {
                        writeInt(out, i)
                        writeLong(out, chunkOffsets[i].toLong())
                        writeInt(out, chunkEncLens[i])
                    }

                    // Seek back to end (not strictly needed but clean)
                    out.channel.position(finalDataEnd)
                }

                // Update header with actual chunk count
                out.channel.position(headerSizePos - 4)
                writeInt(out, totalChunks)
        }

        Log.d(TAG, "Encrypted ${sourceFile.name} → ${destFile.name} " +
                "($totalChunks chunks, ${destFile.length() / (1024 * 1024)} MB)")
    }

    /**
     * Decrypts a specific chunk from the encrypted file.
     * Used by Light Template for on-demand scene loading.
     *
     * @param encryptedFile  The V3 encrypted file
     * @param key            AES-256 key bytes
     * @param chunkIndex     Which chunk to decrypt (0-based)
     * @return  Decrypted chunk bytes
     */
    fun decryptChunk(encryptedFile: File, key: ByteArray, chunkIndex: Int): ByteArray {
        val aesKey = SecretKeySpec(key, "AES")
        val totalChunks = readTotalChunks(encryptedFile)

        require(chunkIndex in 0 until totalChunks) {
            "Chunk index $chunkIndex out of range (0..${totalChunks - 1})"
        }

        val chunkTableOffset = HEADER_SIZE.toLong()
        val chunkEntrySize = 4 + 8 + 4 // index + offset + encLen
        val entryPos = chunkTableOffset + chunkIndex * chunkEntrySize

        FileInputStream(encryptedFile).use { fileIn ->
            fileIn.channel.position(entryPos)

            val idx = readInt(fileIn) // chunk index
            val offset = readLong(fileIn) // offset from data start
            val encLen = readInt(fileIn)

            val dataStartPos = chunkTableOffset + totalChunks * chunkEntrySize
            val chunkStart = dataStartPos + offset

            // Read chunk: [iv:12][ciphertext:N]
            fileIn.channel.position(chunkStart)
            val iv = ByteArray(GCM_IV_SIZE).also { fileIn.read(it) }
            val ciphertext = ByteArray(encLen - GCM_IV_SIZE).also { fileIn.read(it) }

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            return cipher.doFinal(ciphertext)
        }
    }

    /**
     * Decrypts the entire file (used by Full Template).
     */
    fun decryptAll(encryptedFile: File, key: ByteArray, destFile: File): Boolean {
        return try {
            val totalChunks = readTotalChunks(encryptedFile)
            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile).use { out ->
                for (i in 0 until totalChunks) {
                    val chunk = decryptChunk(encryptedFile, key, i)
                    out.write(chunk)
                }
            }
            Log.d(TAG, "Decrypted all $totalChunks chunks to ${destFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Full decryption failed", e)
            destFile.delete()
            false
        }
    }

    /**
     * Reads the integrity hash from the encrypted file header.
     */
    fun readIntegrityHash(encryptedFile: File): ByteArray {
        return FileInputStream(encryptedFile).use { fileIn ->
            val magic = ByteArray(4)
            fileIn.read(magic)
            require(magic.contentEquals(MAGIC)) { "Not a V3 encrypted file" }

            // Skip version(2) + flags(2) + reserved(4) + totalChunks(4)
            fileIn.skip(12)
            val hash = ByteArray(32)
            fileIn.read(hash)
            hash
        }
    }

    private fun readTotalChunks(encryptedFile: File): Int {
        return FileInputStream(encryptedFile).use { fileIn ->
            val magic = ByteArray(4)
            fileIn.read(magic)
            require(magic.contentEquals(MAGIC)) { "Not a V3 encrypted file" }

            // Skip version(2) + flags(2) + reserved(4)
            fileIn.skip(8)
            readInt(fileIn)
        }
    }

    private fun writeHeaderPlaceholder(out: FileOutputStream, totalChunks: Int): Long {
        out.write(MAGIC)
        writeShort(out, FORMAT_VERSION.toInt())
        writeShort(out, 0) // flags
        writeInt(out, 0)   // reserved
        writeInt(out, totalChunks) // will be updated later
        return out.channel.position()
    }

    private fun writeShort(out: FileOutputStream, value: Int) {
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeInt(out: FileOutputStream, value: Int) {
        out.write((value shr 24) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeLong(out: FileOutputStream, value: Long) {
        out.write(((value shr 56) and 0xFF).toInt())
        out.write(((value shr 48) and 0xFF).toInt())
        out.write(((value shr 40) and 0xFF).toInt())
        out.write(((value shr 32) and 0xFF).toInt())
        out.write(((value shr 24) and 0xFF).toInt())
        out.write(((value shr 16) and 0xFF).toInt())
        out.write(((value shr 8) and 0xFF).toInt())
        out.write((value and 0xFF).toInt())
    }

    private fun readInt(fileIn: FileInputStream): Int {
        val b = ByteArray(4)
        fileIn.read(b)
        return ((b[0].toInt() and 0xFF) shl 24) or
                ((b[1].toInt() and 0xFF) shl 16) or
                ((b[2].toInt() and 0xFF) shl 8) or
                (b[3].toInt() and 0xFF)
    }

    private fun readLong(fileIn: FileInputStream): Long {
        val b = ByteArray(8)
        fileIn.read(b)
        return ((b[0].toLong() and 0xFF) shl 56) or
                ((b[1].toLong() and 0xFF) shl 48) or
                ((b[2].toLong() and 0xFF) shl 40) or
                ((b[3].toLong() and 0xFF) shl 32) or
                ((b[4].toLong() and 0xFF) shl 24) or
                ((b[5].toLong() and 0xFF) shl 16) or
                ((b[6].toLong() and 0xFF) shl 8) or
                (b[7].toLong() and 0xFF)
    }
}
