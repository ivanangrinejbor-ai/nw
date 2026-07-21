package org.catrobat.catroid.apkbuildV3.runtime

import android.content.Context
import android.util.Log
import org.catrobat.catroid.apkbuildV3.DynamicKeyManager
import java.io.File

object DynamicKeyResolver {
    private const val TAG = "DynamicKeyResolver"
    private const val KEY_FILE_EXTENSION = ".k3y"
    private const val PAYLOAD_ASSET_NAME = "project.ncv3"

    fun resolveKey(context: Context): ByteArray? {
        return try {
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

    fun hasV3Payload(context: Context): Boolean {
        return try {
            context.assets.list("")?.any { it == PAYLOAD_ASSET_NAME } == true
        } catch (e: Exception) {
            false
        }
    }
}
