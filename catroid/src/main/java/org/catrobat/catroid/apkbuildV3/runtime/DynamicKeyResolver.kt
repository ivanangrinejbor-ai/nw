package org.catrobat.catroid.apkbuildV3.runtime

import android.content.Context
import android.util.Log
import org.catrobat.catroid.apkbuildV3.DynamicKeyManager
import java.io.File

object DynamicKeyResolver {
    private const val TAG = "DynamicKeyResolver"
    private const val PAYLOAD_ASSET_NAME = "project.ncv3"
    private val KEY_FILE_PREFIX = DynamicKeyManager.KEY_FILE_PREFIX
    private val KEY_FILE_SUFFIX = ".nk"

    private fun extractPartIndex(filename: String): Int {
        if (!filename.startsWith(KEY_FILE_PREFIX) || !filename.endsWith(KEY_FILE_SUFFIX)) return -1
        val core = filename.removePrefix(KEY_FILE_PREFIX).removeSuffix(KEY_FILE_SUFFIX)
        val underscoreIdx = core.indexOf('_')
        if (underscoreIdx <= 0) return -1
        return core.substring(0, underscoreIdx).toIntOrNull() ?: -1
    }

    fun resolveKey(context: Context): ByteArray? {
        return try {
            val assetFiles = context.assets.list("") ?: emptyArray()
            val keyFiles = assetFiles.filter { file ->
                file.startsWith(KEY_FILE_PREFIX) && file.endsWith(KEY_FILE_SUFFIX) &&
                        extractPartIndex(file) >= 0
            }.sortedBy { extractPartIndex(it) }

            if (keyFiles.isEmpty()) {
                Log.e(TAG, "No key files found in assets")
                return null
            }

            val parts = keyFiles.map { filename ->
                context.assets.open(filename).use { input ->
                    input.bufferedReader().readText().trim()
                }
            }

            if (!DynamicKeyManager.verifyKeyIntegrity(parts)) {
                Log.e(TAG, "Key files failed integrity check")
                return null
            }

            val keyBytes = DynamicKeyManager.resolveStoredKey(parts)
            Log.i(TAG, "Dynamic key resolved from ${keyFiles.size} files (${keyBytes.size} bytes)")
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
