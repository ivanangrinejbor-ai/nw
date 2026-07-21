package org.catrobat.catroid.ai.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.catrobat.catroid.ai.settings.AiPreferences
import org.catrobat.catroid.content.CustomDns
import org.catrobat.catroid.content.GeminiManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cloud replacement for the local (GGUF/JNI) [ModelRuntime]. Instead of running a
 * model on-device it talks to the Google Gemini REST API
 * (`generativelanguage.googleapis.com`). The public [generate] signature mirrors
 * [ModelRuntime.generate] so the agent loop in AiAgentManager can call either one.
 *
 * The API key is read from the shared, encrypted [GeminiManager] storage (the same
 * key used by the Gemini bricks). The active model id comes from [AiPreferences].
 */
object CloudModelRuntime {

    private const val TAG = "CloudModelRuntime"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    private val generateMutex = Mutex()
    private var appContext: Context? = null

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(CustomDns())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getApiKey(): String? {
        val ctx = appContext ?: return GeminiManager.api_key
        return GeminiManager.getApiKey(ctx) ?: GeminiManager.api_key
    }

    fun setApiKey(key: String) {
        val ctx = appContext
        if (ctx != null) {
            GeminiManager.setApiKey(ctx, key)
        }
        @Suppress("DEPRECATION")
        GeminiManager.api_key = key
    }

    /** The cloud agent is "ready" as soon as a non-empty API key is available. */
    fun isReady(): Boolean = !getApiKey().isNullOrBlank()

    suspend fun generate(
        input: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): String = generateMutex.withLock {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withLock "Error: no Gemini API key set. Open the chat menu and set your API key first."
        }
        val model = AiPreferences.getCloudModelId()
        try {
            withContext(Dispatchers.IO) { requestGemini(apiKey, model, input, temperature, maxTokens) }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud generation failed", e)
            "Error: cloud request failed - ${e.message}"
        }
    }

    private fun requestGemini(
        apiKey: String,
        model: String,
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        val normalizedModel = if (model.startsWith("models/")) model else "models/$model"
        val url = BASE_URL + normalizedModel + ":generateContent"

        val partObj = JSONObject().put("text", prompt)
        val partsArray = JSONArray().put(partObj)
        val contentObj = JSONObject().put("parts", partsArray).put("role", "user")
        val generationConfig = JSONObject()
            .put("temperature", temperature.toDouble())
            .put("maxOutputTokens", maxTokens)
        val jsonBody = JSONObject()
            .put("contents", JSONArray().put(contentObj))
            .put("generationConfig", generationConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("x-goog-api-key", apiKey)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini error ${response.code}: $bodyStr")
                return "Error ${response.code}: ${extractError(bodyStr) ?: response.message}"
            }
            return parseText(bodyStr)
        }
    }

    private fun parseText(bodyStr: String): String {
        return try {
            val root = JSONObject(bodyStr)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                val feedback = root.optJSONObject("promptFeedback")
                val blockReason = feedback?.optString("blockReason")
                return if (!blockReason.isNullOrBlank()) {
                    "Error: request blocked by Gemini ($blockReason)"
                } else {
                    "Error: empty response from Gemini"
                }
            }
            val parts = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                return "Error: response had no text parts"
            }
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                sb.append(parts.getJSONObject(i).optString("text", ""))
            }
            val text = sb.toString().trim()
            if (text.isEmpty()) "Error: model returned empty text" else text
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error", e)
            "Error: failed to parse Gemini response - ${e.message}"
        }
    }

    private fun extractError(bodyStr: String): String? {
        return try {
            JSONObject(bodyStr).optJSONObject("error")?.optString("message")
        } catch (_: Exception) {
            null
        }
    }
}
