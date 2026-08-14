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
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CloudModelRuntime {

    private const val TAG = "CloudModelRuntime"
    private val generateMutex = Mutex()
    private var appContext: Context? = null

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        AiPreferences.init(context)
    }

    fun getActiveProvider(): AiProvider {
        return AiProvider.fromId(AiPreferences.getProvider())
    }

    fun getApiKey(): String? {
        val provider = getActiveProvider()
        return AiPreferences.getApiKeyForProvider(provider.id)
    }

    fun setApiKey(key: String, providerId: String = getActiveProvider().id) {
        AiPreferences.setApiKeyForProvider(providerId, key)
    }

    fun isReady(): Boolean = !getApiKey().isNullOrBlank()

    suspend fun generate(
        systemPrompt: String,
        userContent: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): String = generateMutex.withLock {
        val provider = getActiveProvider()
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withLock "Error: No API key configured for provider ${provider.displayName}. Open AI Assistant settings to set your key."
        }
        val model = AiPreferences.getCloudModelId()
        try {
            withContext(Dispatchers.IO) {
                when (provider) {
                    AiProvider.GEMINI -> requestGemini(apiKey, model, systemPrompt, userContent, temperature, maxTokens)
                    AiProvider.OPENAI, AiProvider.DEEPSEEK, AiProvider.OPENROUTER, AiProvider.OPENCODE ->
                        requestOpenAiFormat(provider, apiKey, model, systemPrompt, userContent, temperature, maxTokens)
                    AiProvider.CLAUDE -> requestClaude(apiKey, model, systemPrompt, userContent, temperature, maxTokens)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed for ${provider.displayName}", e)
            "Error: Cloud request failed - ${e.message}"
        }
    }

    suspend fun generateForProvider(
        provider: AiProvider,
        model: String,
        systemPrompt: String,
        userContent: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): String = generateMutex.withLock {
        val apiKey = AiPreferences.getApiKeyForProvider(provider.id)
        if (apiKey.isNullOrBlank()) {
            return@withLock "Error: No API key configured for provider ${provider.displayName}."
        }
        val resolvedModel = model.ifBlank { provider.defaultModels.firstOrNull() ?: "" }
        try {
            withContext(Dispatchers.IO) {
                when (provider) {
                    AiProvider.GEMINI -> requestGemini(apiKey, resolvedModel, systemPrompt, userContent, temperature, maxTokens)
                    AiProvider.OPENAI, AiProvider.DEEPSEEK, AiProvider.OPENROUTER, AiProvider.OPENCODE ->
                        requestOpenAiFormat(provider, apiKey, resolvedModel, systemPrompt, userContent, temperature, maxTokens)
                    AiProvider.CLAUDE -> requestClaude(apiKey, resolvedModel, systemPrompt, userContent, temperature, maxTokens)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed for ${provider.displayName}", e)
            "Error: Cloud request failed - ${e.message}"
        }
    }

    private fun requestGemini(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        val normalizedModel = if (model.startsWith("models/")) model else "models/$model"
        val url = "https://generativelanguage.googleapis.com/v1beta/$normalizedModel:generateContent"

        val partObj = JSONObject().put("text", userContent)
        val partsArray = JSONArray().put(partObj)
        val contentObj = JSONObject().put("parts", partsArray).put("role", "user")
        val generationConfig = JSONObject()
            .put("temperature", temperature.toDouble())
            .put("maxOutputTokens", maxTokens)
        val jsonBody = JSONObject()
            .put("contents", JSONArray().put(contentObj))
            .put("generationConfig", generationConfig)
        if (systemPrompt.isNotBlank()) {
            jsonBody.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
        }

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
            return parseGeminiText(bodyStr)
        }
    }

    private fun requestOpenAiFormat(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        val url = when (provider) {
            AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
            AiProvider.DEEPSEEK -> "https://api.deepseek.com/v1/chat/completions"
            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AiProvider.OPENCODE -> "https://opencode.ai/zen/v1/chat/completions"
            else -> provider.baseUrl + "chat/completions"
        }

        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", userContent))
        val jsonBody = JSONObject()
            .put("model", model)
            .put("messages", messages)

        val isReasoning = model.contains("reasoner") || model.startsWith("o1") || model.startsWith("o3")
        if (!isReasoning) {
            jsonBody.put("temperature", temperature.toDouble())
        }
        jsonBody.put(if (model.startsWith("o1") || model.startsWith("o3")) "max_completion_tokens" else "max_tokens", maxTokens)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer $apiKey")

        if (provider == AiProvider.OPENROUTER) {
            requestBuilder.header("HTTP-Referer", "https://catroid.org")
            requestBuilder.header("X-Title", "NeoCatroid AI Assistant")
        }

        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "${provider.displayName} error ${response.code}: $bodyStr")
                return "Error ${response.code}: ${extractError(bodyStr) ?: response.message}"
            }
            return parseOpenAiText(bodyStr)
        }
    }

    private fun requestClaude(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        val url = "https://api.anthropic.com/v1/messages"

        val msgObj = JSONObject().put("role", "user").put("content", userContent)
        val jsonBody = JSONObject()
            .put("model", model)
            .put("messages", JSONArray().put(msgObj))
            .put("max_tokens", maxTokens)
            .put("temperature", temperature.toDouble())
        if (systemPrompt.isNotBlank()) {
            jsonBody.put("system", systemPrompt)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "Claude error ${response.code}: $bodyStr")
                return "Error ${response.code}: ${extractError(bodyStr) ?: response.message}"
            }
            return parseClaudeText(bodyStr)
        }
    }

    private fun parseGeminiText(jsonStr: String): String {
        val root = JSONObject(jsonStr)
        val candidates = root.optJSONArray("candidates") ?: return "Error: empty response"
        if (candidates.length() == 0) return "Error: no candidate generated"
        val first = candidates.getJSONObject(0)
        val content = first.optJSONObject("content") ?: return "Error: no content"
        val parts = content.optJSONArray("parts") ?: return "Error: no parts"
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.getJSONObject(i).optString("text", ""))
        }
        return sb.toString()
    }

    private fun parseOpenAiText(jsonStr: String): String {
        val root = JSONObject(jsonStr)
        val choices = root.optJSONArray("choices") ?: return "Error: empty response"
        if (choices.length() == 0) return "Error: no choices"
        val message = choices.getJSONObject(0).optJSONObject("message") ?: return "Error: no message"
        return message.optString("content", "")
    }

    private fun parseClaudeText(jsonStr: String): String {
        val root = JSONObject(jsonStr)
        val content = root.optJSONArray("content") ?: return "Error: empty response"
        if (content.length() == 0) return "Error: no content"
        return content.getJSONObject(0).optString("text", "")
    }

    private fun extractError(bodyStr: String): String? {
        return try {
            val root = JSONObject(bodyStr)
            val err = root.optJSONObject("error")
            err?.optString("message", null)
        } catch (e: Exception) {
            null
        }
    }
}
