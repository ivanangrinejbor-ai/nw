package org.catrobat.catroid.ai.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AiPreferences {

    private const val KEY_ENABLED = "ai_agent_enabled"
    private const val KEY_SELECTED_MODEL = "ai_agent_model_id"
    private const val KEY_AUTO_READ = "ai_agent_auto_read"
    private const val KEY_AUTO_MODIFY = "ai_agent_auto_modify"
    private const val KEY_CONFIRM_CHANGES = "ai_agent_confirm_changes"
    private const val KEY_TEMPERATURE = "ai_agent_temperature"
    private const val KEY_MAX_CONTEXT = "ai_agent_max_context"
    private const val KEY_MAX_TOOL_CALLS = "ai_agent_max_tool_calls"
    private const val KEY_REASONING_LEVEL = "ai_agent_reasoning_level"
    private const val KEY_CLOUD_MODEL = "ai_agent_cloud_model"
    private const val KEY_CLOUD_MODEL_PROVIDER = "ai_agent_cloud_model_provider"
    private const val KEY_BACKEND = "ai_agent_backend"
    private const val KEY_PROVIDER = "ai_agent_provider"
    private const val KEY_PROVIDER_KEY_PREFIX = "ai_agent_key_provider_"

    private const val SECURE_PREFS_NAME = "ai_agent_secure_prefs"

    const val BACKEND_CLOUD = "cloud"
    const val BACKEND_LOCAL = "local"

    private const val DEFAULT_TEMPERATURE = 0.7f
    private const val DEFAULT_MAX_CONTEXT = 4096
    private const val DEFAULT_MAX_TOOL_CALLS = 10
    private const val DEFAULT_CLOUD_MODEL = "gemini-2.5-flash"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun securePrefs(): SharedPreferences? {
        val ctx = appContext ?: return null
        return try {
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                ctx,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getProvider(): String {
        return prefs?.getString(KEY_PROVIDER, "gemini") ?: "gemini"
    }

    fun setProvider(providerId: String) {
        prefs?.edit()?.putString(KEY_PROVIDER, providerId)?.apply()
    }

    fun getApiKeyForProvider(providerId: String): String? {
        val secure = securePrefs()?.getString(KEY_PROVIDER_KEY_PREFIX + providerId, null)
        if (!secure.isNullOrBlank()) return secure
        val legacy = prefs?.getString(KEY_PROVIDER_KEY_PREFIX + providerId, null)
        if (!legacy.isNullOrBlank()) return legacy
        return null
    }

    fun setApiKeyForProvider(providerId: String, key: String) {
        val secure = securePrefs()
        if (secure != null) {
            try {
                secure.edit().putString(KEY_PROVIDER_KEY_PREFIX + providerId, key).apply()
                prefs?.edit()?.remove(KEY_PROVIDER_KEY_PREFIX + providerId)?.apply()
            } catch (_: Exception) {}
        } else {
            prefs?.edit()?.putString(KEY_PROVIDER_KEY_PREFIX + providerId, key)?.apply()
        }
        if (providerId.equals("gemini", ignoreCase = true)) {
            @Suppress("DEPRECATION")
            org.catrobat.catroid.content.GeminiManager.api_key = key
        }
    }

    fun isEnabled(): Boolean {
        return prefs?.getBoolean(KEY_ENABLED, false) ?: false
    }

    fun setEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }

    fun getSelectedModelId(): String? {
        return prefs?.getString(KEY_SELECTED_MODEL, null)
    }

    fun setSelectedModelId(id: String?) {
        prefs?.edit()?.putString(KEY_SELECTED_MODEL, id)?.apply()
    }

    fun isAutoReadEnabled(): Boolean {
        return prefs?.getBoolean(KEY_AUTO_READ, true) ?: true
    }

    fun setAutoReadEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_AUTO_READ, enabled)?.apply()
    }

    fun isAutoModifyEnabled(): Boolean {
        return prefs?.getBoolean(KEY_AUTO_MODIFY, false) ?: false
    }

    fun setAutoModifyEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_AUTO_MODIFY, enabled)?.apply()
    }

    fun isConfirmEnabled(): Boolean {
        return prefs?.getBoolean(KEY_CONFIRM_CHANGES, true) ?: true
    }

    fun setConfirmEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_CONFIRM_CHANGES, enabled)?.apply()
    }

    fun getTemperature(): Float {
        val raw = prefs?.getString(KEY_TEMPERATURE, null) ?: return DEFAULT_TEMPERATURE
        return raw.toFloatOrNull()?.coerceIn(0f, 2f) ?: DEFAULT_TEMPERATURE
    }

    fun setTemperature(temp: Float) {
        prefs?.edit()?.putString(KEY_TEMPERATURE, temp.coerceIn(0f, 2f).toString())?.apply()
    }

    fun getMaxContext(): Int {
        val raw = prefs?.getString(KEY_MAX_CONTEXT, null) ?: return DEFAULT_MAX_CONTEXT
        return raw.toIntOrNull()?.coerceIn(512, 32000) ?: DEFAULT_MAX_CONTEXT
    }

    fun setMaxContext(ctx: Int) {
        prefs?.edit()?.putString(KEY_MAX_CONTEXT, ctx.coerceIn(512, 32000).toString())?.apply()
    }

    fun getMaxToolCalls(): Int {
        val raw = prefs?.getString(KEY_MAX_TOOL_CALLS, null) ?: return DEFAULT_MAX_TOOL_CALLS
        return raw.toIntOrNull()?.coerceIn(1, 50) ?: DEFAULT_MAX_TOOL_CALLS
    }

    fun setMaxToolCalls(max: Int) {
        prefs?.edit()?.putString(KEY_MAX_TOOL_CALLS, max.coerceIn(1, 50).toString())?.apply()
    }

    const val REASONING_DEFAULT = "default"
    const val REASONING_OFF = "off"
    const val REASONING_LOW = "low"
    const val REASONING_MEDIUM = "medium"
    const val REASONING_HIGH = "high"

    val reasoningLevels = listOf(REASONING_DEFAULT, REASONING_OFF, REASONING_LOW, REASONING_MEDIUM, REASONING_HIGH)

    fun getReasoningLevel(): String {
        val raw = prefs?.getString(KEY_REASONING_LEVEL, REASONING_DEFAULT) ?: REASONING_DEFAULT
        return if (raw in reasoningLevels) raw else REASONING_DEFAULT
    }

    fun setReasoningLevel(level: String) {
        if (level in reasoningLevels) {
            prefs?.edit()?.putString(KEY_REASONING_LEVEL, level)?.apply()
        }
    }

    fun getCloudModelId(): String {
        return prefs?.getString(KEY_CLOUD_MODEL, DEFAULT_CLOUD_MODEL) ?: DEFAULT_CLOUD_MODEL
    }

    fun setCloudModelId(id: String) {
        prefs?.edit()?.putString(KEY_CLOUD_MODEL, id)?.apply()
    }

    fun getCloudModelProviderId(): String? {
        return prefs?.getString(KEY_CLOUD_MODEL_PROVIDER, null)
    }

    fun setCloudModelProviderId(providerId: String?) {
        if (providerId == null) {
            prefs?.edit()?.remove(KEY_CLOUD_MODEL_PROVIDER)?.apply()
        } else {
            prefs?.edit()?.putString(KEY_CLOUD_MODEL_PROVIDER, providerId)?.apply()
        }
    }

    fun getBackend(): String {
        return prefs?.getString(KEY_BACKEND, BACKEND_CLOUD) ?: BACKEND_CLOUD
    }

    fun setBackend(backend: String) {
        prefs?.edit()?.putString(KEY_BACKEND, backend)?.apply()
    }

    fun isLocalBackend(): Boolean = getBackend() == BACKEND_LOCAL
}
