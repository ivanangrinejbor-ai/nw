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
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Project encryption/decryption using AES-256-GCM + PBKDF2 key derivation.
 *
 * Format of encrypted file:
 *   [magic: 4 bytes = "NCPP"]
 *   [salt: 32 bytes]
 *   [IV: 12 bytes]
 *   [ciphertext: N bytes]
 *
 * Password is NEVER stored. Only salt and IV are stored.
 * PBKDF2 iterations: 100_000 (OWASP 2023 recommendation)
 */
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

    fun isEncrypted(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return FileInputStream(file).use { input ->
            val header = ByteArray(4)
            input.read(header)
            header.contentEquals(MAGIC)
        }
    }

    fun encrypt(sourceFile: File, destFile: File, password: String) {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val plaintext = FileInputStream(sourceFile).use { it.readBytes() }
        val ciphertext = cipher.doFinal(plaintext)

        FileOutputStream(destFile).use { out ->
            out.write(MAGIC)
            out.write(salt)
            out.write(iv)
            out.write(ciphertext)
        }
        Log.d(TAG, "Encrypted: ${sourceFile.name} -> ${destFile.name} (${ciphertext.size} bytes)")
    }

    fun decrypt(sourceFile: File, destFile: File, password: String): Boolean {
        return try {
            FileInputStream(sourceFile).use { input ->
                val header = ByteArray(4)
                if (input.read(header) < 4 || !header.contentEquals(MAGIC)) {
                    Log.e(TAG, "Not an encrypted project file")
                    return false
                }
                val salt = ByteArray(SALT_SIZE).also { input.read(it) }
                val iv = ByteArray(IV_SIZE).also { input.read(it) }
                val ciphertext = ByteArray(input.available()).also { input.read(it) }

                val key = deriveKey(password, salt)
                val cipher = Cipher.getInstance(ALGORITHM)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

                val plaintext = cipher.doFinal(ciphertext)
                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { out -> out.write(plaintext) }
                Log.d(TAG, "Decrypted: ${sourceFile.name} -> ${destFile.name}")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed (wrong password or corrupted file)", e)
            destFile.delete()
            false
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val tmpKey = factory.generateSecret(spec)
        return SecretKeySpec(tmpKey.encoded, "AES")
    }
}
