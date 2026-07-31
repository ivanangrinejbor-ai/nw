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
package org.catrobat.catroid.apkbuild

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object PayloadIntegrity {
    private const val TAG = "PayloadIntegrity"
    private const val HMAC_ALGORITHM = "HmacSHA256"

    fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun hmacHex(data: ByteArray, keyBytes: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(keyBytes, HMAC_ALGORITHM))
        return toHex(mac.doFinal(data))
    }

    fun certHashFromKeystore(keystore: File, storePassword: String, alias: String): ByteArray? {
        return try {
            val ks = KeyStore.getInstance("PKCS12")
            keystore.inputStream().use { ks.load(it, storePassword.toCharArray()) }
            val resolvedAlias = if (ks.containsAlias(alias)) alias else ks.aliases().nextElement()
            val cert = ks.getCertificate(resolvedAlias) ?: return null
            sha256(cert.encoded)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read signing certificate from keystore", e)
            null
        }
    }

    fun ownCertHash(context: Context): ByteArray? {
        return try {
            val pm = context.packageManager
            val pkgName = context.packageName
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo ?: return null
                signingInfo.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures
            }
            val first = signatures?.firstOrNull() ?: return null
            sha256(first.toByteArray())
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read own signing certificate", e)
            null
        }
    }

    fun buildSigContent(datBytes: ByteArray, certHash: ByteArray): String {
        val certHex = toHex(certHash)
        val hmac = hmacHex(datBytes, certHash)
        return "$certHex\n$hmac\n"
    }

    fun verify(sigContent: String, datBytes: ByteArray, ownCertHash: ByteArray): Boolean {
        val lines = sigContent.trim().lines()
        if (lines.size < 2) {
            Log.e(TAG, "Malformed integrity signature")
            return false
        }
        val expectedCertHex = lines[0].trim().lowercase()
        val expectedHmac = lines[1].trim().lowercase()
        val ownCertHex = toHex(ownCertHash)
        if (!constantTimeEquals(ownCertHex, expectedCertHex)) {
            Log.e(TAG, "Signing certificate mismatch — APK repacked/resigned")
            return false
        }
        val actualHmac = hmacHex(datBytes, ownCertHash)
        if (!constantTimeEquals(actualHmac, expectedHmac)) {
            Log.e(TAG, "Payload HMAC mismatch — project substituted")
            return false
        }
        return true
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
