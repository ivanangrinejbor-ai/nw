/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.io

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val STREAM_BUFFER = 64 * 1024

object ProjectCrypto {
    private const val TAG = "ProjectCrypto"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val AES_KEY_SIZE = 256
    private const val SALT_SIZE = 32
    private const val IV_SIZE = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private val MAGIC = byteArrayOf('N'.code.toByte(), 'C'.code.toByte(), 'P'.code.toByte(), 'P'.code.toByte())
    private val LOCKED_MAGIC = byteArrayOf('N'.code.toByte(), 'C'.code.toByte(), 'P'.code.toByte(), 'X'.code.toByte())
    private val STREAMING_MAGIC = byteArrayOf('N'.code.toByte(), 'C'.code.toByte(), 'P'.code.toByte(), 'S'.code.toByte())
    private const val SEGMENT_SIZE = 4 * 1024 * 1024

    fun isEncrypted(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return FileInputStream(file).use { input ->
            val header = ByteArray(4)
            input.read(header)
            header.contentEquals(MAGIC) || header.contentEquals(LOCKED_MAGIC)
        }
    }

    fun isLocked(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return FileInputStream(file).use { input ->
            val header = ByteArray(4)
            input.read(header)
            header.contentEquals(LOCKED_MAGIC)
        }
    }

    fun encrypt(sourceFile: File, destFile: File, password: String, locked: Boolean = false) {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        destFile.parentFile?.mkdirs()
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(destFile).use { out ->
                out.write(if (locked) LOCKED_MAGIC else MAGIC)
                out.write(salt)
                out.write(iv)
                val buffer = ByteArray(STREAM_BUFFER)
                var n: Int
                while (input.read(buffer).also { n = it } != -1) {
                    if (n > 0) {
                        val encoded = cipher.update(buffer, 0, n)
                        if (encoded != null && encoded.isNotEmpty()) out.write(encoded)
                    }
                }
                val finalBlock = cipher.doFinal()
                if (finalBlock != null && finalBlock.isNotEmpty()) out.write(finalBlock)
            }
        }
        Log.d(TAG, "Encrypted: ${sourceFile.name} -> ${destFile.name}")
    }

    fun decrypt(sourceFile: File, destFile: File, password: String): Boolean {
        return try {
            FileInputStream(sourceFile).use { input ->
                val header = ByteArray(4)
                if (input.read(header) < 4 ||
                    !(header.contentEquals(MAGIC) || header.contentEquals(LOCKED_MAGIC))) {
                    Log.e(TAG, "Not an encrypted project file")
                    return false
                }
                val salt = ByteArray(SALT_SIZE).also { input.read(it) }
                val iv = ByteArray(IV_SIZE).also { input.read(it) }

                val key = deriveKey(password, salt)
                val cipher = Cipher.getInstance(ALGORITHM)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { out ->
                    val buffer = ByteArray(STREAM_BUFFER)
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        if (n > 0) {
                            val decoded = cipher.update(buffer, 0, n)
                            if (decoded != null && decoded.isNotEmpty()) out.write(decoded)
                        }
                    }
                    val finalBlock = cipher.doFinal()
                    if (finalBlock != null && finalBlock.isNotEmpty()) out.write(finalBlock)
                }
                Log.d(TAG, "Decrypted: ${sourceFile.name} -> ${destFile.name}")
                true
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Decryption failed (wrong password or corrupted file)", e)
            destFile.delete()
            false
        }
    }

    fun encryptDirectory(
        sourceDir: File,
        destFile: File,
        password: String,
        filter: ((String) -> Boolean)? = null
    ) {
        destFile.parentFile?.mkdirs()
        FileOutputStream(destFile).use { fos ->
            encryptDirectoryToStream(sourceDir, fos, password, filter)
        }
        Log.d(TAG, "Encrypted directory: ${sourceDir.name} -> ${destFile.name}")
    }

    fun encryptDirectoryToStream(
        sourceDir: File,
        out: OutputStream,
        password: String,
        filter: ((String) -> Boolean)? = null
    ) {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        out.write(MAGIC)
        out.write(salt)
        out.write(iv)

        val guarded = object : FilterOutputStream(out) {
            override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len) }
            override fun close() { flush() }
        }
        CipherOutputStream(guarded, cipher).use { cos ->
            ZipOutputStream(cos).use { zos ->
                zos.setLevel(1)
                val buffer = ByteArray(STREAM_BUFFER)

                sourceDir.walk().filter { it != sourceDir }.forEach { file ->
                    val entryPath = file.relativeTo(sourceDir).path.replace('\\', '/')
                    if (filter != null && !filter(file.name)) return@forEach

                    if (file.isDirectory) {
                        zos.putNextEntry(ZipEntry("$entryPath/"))
                        zos.closeEntry()
                    } else {
                        zos.putNextEntry(ZipEntry(entryPath))
                        FileInputStream(file).use { fis ->
                            var n: Int
                            while (fis.read(buffer).also { n = it } != -1) {
                                if (n > 0) zos.write(buffer, 0, n)
                            }
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
        out.flush()
    }

    fun encryptDirectoryToStreamChunked(
        sourceDir: File,
        out: OutputStream,
        password: String,
        filter: ((String) -> Boolean)? = null,
        segmentSize: Int = SEGMENT_SIZE
    ) {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val ivPrefix = ByteArray(8).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        out.write(STREAMING_MAGIC)
        out.write(salt)
        out.write(ivPrefix)

        val chunked = ChunkedGcmOutputStream(out, key, ivPrefix, segmentSize)
        ZipOutputStream(chunked).use { zos ->
            zos.setLevel(1)
            val buffer = ByteArray(STREAM_BUFFER)
            sourceDir.walk().filter { it != sourceDir }.forEach { file ->
                val entryPath = file.relativeTo(sourceDir).path.replace('\\', '/')
                if (filter != null && !filter(file.name)) return@forEach
                if (file.isDirectory) {
                    zos.putNextEntry(ZipEntry("$entryPath/"))
                    zos.closeEntry()
                } else {
                    zos.putNextEntry(ZipEntry(entryPath))
                    FileInputStream(file).use { fis ->
                        var n: Int
                        while (fis.read(buffer).also { n = it } != -1) {
                            if (n > 0) zos.write(buffer, 0, n)
                        }
                    }
                    zos.closeEntry()
                }
            }
        }
        out.flush()
        Log.d(TAG, "Chunked-encrypted directory: ${sourceDir.name}")
    }

    private class ChunkedGcmOutputStream(
        private val out: OutputStream,
        private val key: SecretKey,
        private val ivPrefix: ByteArray,
        private val segmentSize: Int
    ) : OutputStream() {
        private val buffer = ByteArray(segmentSize)
        private var count = 0
        private var segmentIndex = 0
        private val lenBytes = ByteArray(4)

        override fun write(b: Int) {
            buffer[count++] = b.toByte()
            if (count == segmentSize) flushSegment()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            var o = off
            var remaining = len
            while (remaining > 0) {
                val n = minOf(segmentSize - count, remaining)
                System.arraycopy(b, o, buffer, count, n)
                count += n; o += n; remaining -= n
                if (count == segmentSize) flushSegment()
            }
        }

        private fun flushSegment() {
            if (count == 0) return
            val iv = ByteArray(IV_SIZE)
            System.arraycopy(ivPrefix, 0, iv, 0, 8)
            iv[8] = (segmentIndex ushr 24).toByte()
            iv[9] = (segmentIndex ushr 16).toByte()
            iv[10] = (segmentIndex ushr 8).toByte()
            iv[11] = segmentIndex.toByte()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val ct = cipher.doFinal(buffer, 0, count)
            lenBytes[0] = (ct.size ushr 24).toByte()
            lenBytes[1] = (ct.size ushr 16).toByte()
            lenBytes[2] = (ct.size ushr 8).toByte()
            lenBytes[3] = ct.size.toByte()
            out.write(lenBytes)
            out.write(ct)
            count = 0
            segmentIndex++
        }

        override fun flush() = out.flush()

        override fun close() {
            flushSegment()
            out.flush()
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val tmpKey = factory.generateSecret(spec)
        return SecretKeySpec(tmpKey.encoded, "AES")
    }
}
