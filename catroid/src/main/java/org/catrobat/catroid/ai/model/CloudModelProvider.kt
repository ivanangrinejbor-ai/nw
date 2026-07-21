package org.catrobat.catroid.ai.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.catrobat.catroid.content.CustomDns
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Loads the list of available Gemini models from the live API
 * (`GET /v1beta/models`) so the user can pick any model their key has access to,
 * instead of relying on a hard-coded list. Only models that advertise support for
 * `generateContent` are returned.
 */
object CloudModelProvider {

    private const val TAG = "CloudModelProvider"
    private const val MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=200"

    /** Sensible fallback if the network call fails or no key is set. */
    val FALLBACK_MODELS = listOf(
        "models/gemini-2.5-flash",
        "models/gemini-2.5-pro",
        "models/gemini-2.0-flash",
        "models/gemini-1.5-flash",
        "models/gemini-1.5-pro"
    )

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(CustomDns())
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * @return list of model ids (e.g. "models/gemini-2.5-flash") that support
     *         `generateContent`, or [FALLBACK_MODELS] on any failure.
     */
    suspend fun fetchModels(apiKey: String?): List<String> {
        if (apiKey.isNullOrBlank()) return FALLBACK_MODELS
        return try {
            withContext(Dispatchers.IO) { requestModels(apiKey) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch model list", e)
            FALLBACK_MODELS
        }
    }

    private fun requestModels(apiKey: String): List<String> {
        val request = Request.Builder()
            .url(MODELS_URL)
            .get()
            .header("x-goog-api-key", apiKey)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Model list error ${response.code}")
                return FALLBACK_MODELS
            }
            val bodyStr = response.body?.string() ?: return FALLBACK_MODELS
            val root = JSONObject(bodyStr)
            val models = root.optJSONArray("models") ?: return FALLBACK_MODELS
            val result = mutableListOf<String>()
            for (i in 0 until models.length()) {
                val model = models.optJSONObject(i) ?: continue
                val name = model.optString("name", "")
                if (name.isBlank()) continue
                val methods = model.optJSONArray("supportedGenerationMethods")
                var supportsGenerate = false
                if (methods != null) {
                    for (j in 0 until methods.length()) {
                        if (methods.optString(j) == "generateContent") {
                            supportsGenerate = true
                            break
                        }
                    }
                }
                if (supportsGenerate) {
                    result.add(name)
                }
            }
            return if (result.isEmpty()) FALLBACK_MODELS else result.sorted()
        }
    }
}
