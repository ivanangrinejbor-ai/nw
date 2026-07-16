package org.catrobat.catroid.apkbuildV3.runtime

import android.content.Context
import android.util.Log
import org.catrobat.catroid.apkbuildV3.DynamicKeyManager
import java.io.File

/**
 * Resolves the dynamic encryption key from the built APK's assets.
 *
 * The key file is stored in assets under a randomly-generated filename
 * (e.g. "assets/a3f7c9e21b0d4f58.k3y"). This resolver:
 * 1. Scans assets directory for *.k3y files.
 * 2. Reads the first matching file.
 * 3. De-obfuscates and returns the raw AES-256 key.
 *
 * If no key file is found (legacy backward-compatible payload),
 * returns null — caller should fall back to the static password.
 */
object DynamicKeyResolver {
    private const val TAG = "DynamicKeyResolver"
    private const val KEY_FILE_EXTENSION = ".k3y"
    private const val PAYLOAD_ASSET_NAME = "project.ncv3"

    /**
     * Attempts to find and resolve the dynamic encryption key.
     *
     * @return  The raw AES-256 key bytes, or null if no key file is found
     *          (caller should use the static fallback password).
     */
    fun resolveKey(context: Context): ByteArray? {
        return try {
            // List all assets files to find the key
            val assetFiles = context.assets.list("") ?: emptyArray()
            val keyFile = assetFiles.firstOrNull { it.endsWith(KEY_FILE_EXTENSION) }
                ?: return null

            val keyString = context.assets.open(keyFile).use { input ->
                input.bufferedReader().readText().trim()
            }

            if (!DynamicKeyManager.verifyKeyIntegrity(keyString)) {
                Log.e(TAG, "Key file $keyFile failed integrity check")
                return null
            }

            val keyBytes = DynamicKeyManager.resolveStoredKey(keyString)
            Log.i(TAG, "Dynamic key resolved from $keyFile (${keyBytes.size} bytes)")
            keyBytes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve dynamic key", e)
            null
        }
    }

    /**
     * Checks whether the APK contains a V3 encrypted payload.
     */
    fun hasV3Payload(context: Context): Boolean {
        return try {
            context.assets.list("")?.any { it == PAYLOAD_ASSET_NAME } == true
        } catch (e: Exception) {
            false
        }
    }
}
