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

        // Use a single RandomAccessFile for BOTH writing and seeking so the file
        // pointer stays consistent (mixing FileOutputStream.write() with
        // out.channel.position() desyncs the stream buffer and corrupts the layout).
        java.io.RandomAccessFile(destFile, "rw").use { raf ->
            raf.setLength(0)

            // ── Header ──
            raf.write(MAGIC)
            writeShort(raf, FORMAT_VERSION.toInt())
            writeShort(raf, 0) // flags
            writeInt(raf, 0)   // reserved
            writeInt(raf, totalChunks) // total chunks (final value)

            // ── Integrity hash (SHA-256 of plaintext) ──
            val digest = java.security.MessageDigest.getInstance(SHA256)
            FileInputStream(sourceFile).use { hashIn ->
                val hashBuf = ByteArray(64 * 1024)
                var hashN: Int
                while (hashIn.read(hashBuf).also { hashN = it } != -1) {
                    digest.update(hashBuf, 0, hashN)
                }
            }
            raf.write(digest.digest())

            // ── Chunk index table placeholder ──
            val chunkTablePos = raf.filePointer
            for (i in 0 until totalChunks) {
                writeInt(raf, 0)    // chunk index
                writeLong(raf, 0L)  // offset
                writeInt(raf, 0)    // encrypted length
            }

            // ── Chunk data ──
            val dataStartPos = raf.filePointer
            FileInputStream(sourceFile).use { sourceIn ->
                val buffer = ByteArray(CHUNK_SIZE)
                var bytesRead: Int
                var chunkIndex = 0
                while (sourceIn.read(buffer).also { bytesRead = it } != -1) {
                    val cipher = Cipher.getInstance(ALGORITHM)
                    val iv = ByteArray(GCM_IV_SIZE).also { SecureRandom().nextBytes(it) }
                    cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

                    val plaintext = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                    val ciphertext = cipher.doFinal(plaintext)

                    val chunkStart = raf.filePointer
                    raf.write(iv)
                    raf.write(ciphertext)
                    val chunkEnd = raf.filePointer

                    chunkOffsets[chunkIndex] = (chunkStart - dataStartPos).toInt()
                    chunkEncLens[chunkIndex] = (chunkEnd - chunkStart).toInt()
                    chunkIndex++

                    onProgress?.invoke(chunkIndex.toFloat() / maxOf(totalChunks, 1))
                }
            }

            // ── Rewrite the actual chunk table ──
            val finalDataEnd = raf.filePointer
            raf.seek(chunkTablePos)
            for (i in 0 until totalChunks) {
                writeInt(raf, i)
                writeLong(raf, chunkOffsets[i].toLong())
                writeInt(raf, chunkEncLens[i])
            }
            raf.seek(finalDataEnd)
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

    private fun writeShort(raf: java.io.RandomAccessFile, value: Int) {
        raf.write((value shr 8) and 0xFF)
        raf.write(value and 0xFF)
    }

    private fun writeInt(raf: java.io.RandomAccessFile, value: Int) {
        raf.write((value shr 24) and 0xFF)
        raf.write((value shr 16) and 0xFF)
        raf.write((value shr 8) and 0xFF)
        raf.write(value and 0xFF)
    }

    private fun writeLong(raf: java.io.RandomAccessFile, value: Long) {
        raf.write(((value shr 56) and 0xFF).toInt())
        raf.write(((value shr 48) and 0xFF).toInt())
        raf.write(((value shr 40) and 0xFF).toInt())
        raf.write(((value shr 32) and 0xFF).toInt())
        raf.write(((value shr 24) and 0xFF).toInt())
        raf.write(((value shr 16) and 0xFF).toInt())
        raf.write(((value shr 8) and 0xFF).toInt())
        raf.write((value and 0xFF).toInt())
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
