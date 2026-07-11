package org.catrobat.catroid.content

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.KeyGenerator

class GeminiManager {
    companion object {
        private const val PREF_API_KEY = "gemini_api_key"
        private const val PREFS_NAME = "gemini_secure_prefs"

        fun getApiKey(context: Context): String? {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val prefs = EncryptedSharedPreferences.create(
                    context, PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                prefs.getString(PREF_API_KEY, null)
            } catch (e: Exception) {
                null
            }
        }

        fun setApiKey(context: Context, key: String) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val prefs = EncryptedSharedPreferences.create(
                    context, PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                prefs.edit().putString(PREF_API_KEY, key).apply()
            } catch (e: Exception) {
                android.util.Log.e("GeminiManager", "Failed to save API key", e)
            }
        }

        @Deprecated("Use getApiKey(context) / setApiKey(context, key) instead. This field will be removed.")
        var api_key: String? = null
            set(value) {
                field = value
                // Sync to secure storage if app context is available
                try {
                    val ctx = org.catrobat.catroid.CatroidApplication.current
                    if (ctx != null && value != null && value.isNotEmpty()) {
                        setApiKey(ctx, value)
                    }
                } catch (_: Exception) { }
            }
            get() {
                if (field == null || field!!.isEmpty()) {
                    try {
                        val ctx = org.catrobat.catroid.CatroidApplication.current
                        if (ctx != null) {
                            field = getApiKey(ctx)
                        }
                    } catch (_: Exception) { }
                }
                return field
            }

        var dns_server: String? = null
    }
}