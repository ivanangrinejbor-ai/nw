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

    data class CloudGeneration(val content: String, val reasoning: String? = null)

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
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
        maxTokens: Int = 2048
    ): String = generateWithMeta(systemPrompt, userContent, maxTokens).content

    suspend fun generateForProvider(
        provider: AiProvider,
        model: String,
        systemPrompt: String,
        userContent: String,
        maxTokens: Int = 2048
    ): String = generateWithMeta(provider, model, systemPrompt, userContent, maxTokens).content

    suspend fun generateVisionForProvider(
        provider: AiProvider,
        model: String,
        systemPrompt: String,
        userContent: String,
        imageBase64Png: String,
        maxTokens: Int = 2048
    ): String = generateWithMeta(
        provider, model, systemPrompt, userContent, maxTokens, imageBase64Png
    ).content

    suspend fun generateWithMeta(
        systemPrompt: String,
        userContent: String,
        maxTokens: Int = 2048
    ): CloudGeneration = generateWithMeta(getActiveProvider(), AiPreferences.getCloudModelId(), systemPrompt, userContent, maxTokens)

    suspend fun generateWithMeta(
        provider: AiProvider,
        model: String,
        systemPrompt: String,
        userContent: String,
        maxTokens: Int = 2048,
        imageBase64Png: String? = null
    ): CloudGeneration = generateMutex.withLock {
        val apiKey = AiPreferences.getApiKeyForProvider(provider.id)
        if (apiKey.isNullOrBlank()) {
            return@withLock CloudGeneration("Error: No API key configured for provider ${provider.displayName}.")
        }
        val resolvedModel = model.ifBlank { provider.defaultModels.firstOrNull() ?: "" }
        try {
            withContext(Dispatchers.IO) {
                when (provider) {
                    AiProvider.GEMINI -> requestGemini(apiKey, resolvedModel, systemPrompt, userContent, maxTokens, imageBase64Png)
                    AiProvider.OPENAI, AiProvider.DEEPSEEK, AiProvider.OPENROUTER, AiProvider.OPENCODE ->
                        requestOpenAiFormat(provider, apiKey, resolvedModel, systemPrompt, userContent, maxTokens, imageBase64Png)
                    AiProvider.CLAUDE -> requestClaude(apiKey, resolvedModel, systemPrompt, userContent, maxTokens, imageBase64Png)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed for ${provider.displayName}", e)
            CloudGeneration("Error: Cloud request failed - ${e.message}")
        }
    }

    private fun reasoningLevel(): String = AiPreferences.getReasoningLevel()

    private fun geminiThinkingBudget(level: String): Int? = when (level) {
        AiPreferences.REASONING_OFF -> 0
        AiPreferences.REASONING_LOW -> 1024
        AiPreferences.REASONING_MEDIUM -> 8192
        AiPreferences.REASONING_HIGH -> 24576
        else -> null
    }

    private fun requestGemini(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        maxTokens: Int,
        imageBase64Png: String? = null
    ): CloudGeneration {
        val normalizedModel = if (model.startsWith("models/")) model else "models/$model"
        val url = "https://generativelanguage.googleapis.com/v1beta/$normalizedModel:generateContent"

        val partObj = JSONObject().put("text", userContent)
        val partsArray = JSONArray().put(partObj)
        if (!imageBase64Png.isNullOrBlank()) {
            val imageData = JSONObject()
                .put("mime_type", "image/png")
                .put("data", imageBase64Png)
            partsArray.put(JSONObject().put("inline_data", imageData))
        }
        val contentObj = JSONObject().put("parts", partsArray).put("role", "user")
        val generationConfig = JSONObject()
            .put("maxOutputTokens", maxTokens)
        geminiThinkingBudget(reasoningLevel())?.let { budget ->
            generationConfig.put("thinkingConfig", JSONObject().put("thinkingBudget", budget))
        }
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
                return CloudGeneration("Error ${response.code}: ${extractError(bodyStr) ?: response.message}")
            }
            return parseGeminiResult(bodyStr)
        }
    }

    private fun requestOpenAiFormat(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        maxTokens: Int,
        imageBase64Png: String? = null
    ): CloudGeneration {
        val url = when (provider) {
            AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
            AiProvider.DEEPSEEK -> "https://api.deepseek.com/v1/chat/completions"
            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AiProvider.OPENCODE -> "https://opencode.ai/zen/v1/chat/completions"
            else -> provider.baseUrl + "chat/completions"
        }

        val userMessage = JSONObject().put("role", "user")
        if (imageBase64Png.isNullOrBlank()) {
            userMessage.put("content", userContent)
        } else {
            val contentArray = JSONArray()
                .put(JSONObject().put("type", "text").put("text", userContent))
                .put(
                    JSONObject().put("type", "image_url").put(
                        "image_url",
                        JSONObject().put("url", "data:image/png;base64,$imageBase64Png")
                    )
                )
            userMessage.put("content", contentArray)
        }
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(userMessage)
        val jsonBody = JSONObject()
            .put("model", model)
            .put("messages", messages)

        val level = reasoningLevel()
        val isReasoning = model.contains("reasoner") || model.startsWith("o1") || model.startsWith("o3") ||
            model.startsWith("o4") || model.contains("gpt-5") || model.contains("r1")
        jsonBody.put(if (model.startsWith("o1") || model.startsWith("o3")) "max_completion_tokens" else "max_tokens", maxTokens)

        when (level) {
            AiPreferences.REASONING_LOW, AiPreferences.REASONING_MEDIUM, AiPreferences.REASONING_HIGH -> {
                val effort = level
                if (provider == AiProvider.OPENROUTER) {
                    jsonBody.put("reasoning", JSONObject().put("effort", effort))
                } else if (provider == AiProvider.OPENAI || isReasoning) {
                    jsonBody.put("reasoning_effort", effort)
                }
            }
            else -> {}
        }

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
                return CloudGeneration("Error ${response.code}: ${extractError(bodyStr) ?: response.message}")
            }
            return parseOpenAiResult(bodyStr)
        }
    }

    private fun requestClaude(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        maxTokens: Int,
        imageBase64Png: String? = null
    ): CloudGeneration {
        val url = "https://api.anthropic.com/v1/messages"

        val msgObj = JSONObject().put("role", "user")
        if (imageBase64Png.isNullOrBlank()) {
            msgObj.put("content", userContent)
        } else {
            val contentArray = JSONArray()
                .put(
                    JSONObject().put("type", "image").put(
                        "source",
                        JSONObject()
                            .put("type", "base64")
                            .put("media_type", "image/png")
                            .put("data", imageBase64Png)
                    )
                )
                .put(JSONObject().put("type", "text").put("text", userContent))
            msgObj.put("content", contentArray)
        }
        val jsonBody = JSONObject()
            .put("model", model)
            .put("messages", JSONArray().put(msgObj))
            .put("max_tokens", maxTokens)

        val level = reasoningLevel()
        if (level == AiPreferences.REASONING_LOW || level == AiPreferences.REASONING_MEDIUM || level == AiPreferences.REASONING_HIGH) {
            val budget = when (level) {
                AiPreferences.REASONING_LOW -> 2048
                AiPreferences.REASONING_MEDIUM -> 8192
                else -> 16384
            }
            jsonBody.put("max_tokens", maxTokens + budget)
            jsonBody.put(
                "thinking",
                JSONObject().put("type", "enabled").put("budget_tokens", budget)
            )
        }
        if (systemPrompt.isNotBlank()) {
            jsonBody.put(
                "system", JSONArray().put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", systemPrompt)
                        .put("cache_control", JSONObject().put("type", "ephemeral"))
                )
            )
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
                return CloudGeneration("Error ${response.code}: ${extractError(bodyStr) ?: response.message}")
            }
            return parseClaudeResult(bodyStr)
        }
    }

    suspend fun generateStreaming(
        systemPrompt: String,
        userContent: String,
        maxTokens: Int = 2048,
        onDelta: suspend (String) -> Unit
    ): CloudGeneration {
        val provider = getActiveProvider()
        val apiKey = AiPreferences.getApiKeyForProvider(provider.id)
        if (apiKey.isNullOrBlank()) {
            return CloudGeneration("Error: No API key configured for provider ${provider.displayName}.")
        }
        val model = AiPreferences.getCloudModelId()
        val resolvedModel = model.ifBlank { provider.defaultModels.firstOrNull() ?: "" }
        return try {
            withContext(Dispatchers.IO) {
                when (provider) {
                    AiProvider.GEMINI -> streamGemini(apiKey, resolvedModel, systemPrompt, userContent, maxTokens, onDelta)
                    AiProvider.OPENAI, AiProvider.DEEPSEEK, AiProvider.OPENROUTER, AiProvider.OPENCODE ->
                        streamOpenAiFormat(provider, apiKey, resolvedModel, systemPrompt, userContent, maxTokens, onDelta)
                    AiProvider.CLAUDE -> streamClaude(apiKey, resolvedModel, systemPrompt, userContent, maxTokens, onDelta)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Streaming failed, falling back to blocking call", e)
            generateWithMeta(provider, model, systemPrompt, userContent, maxTokens)
        }
    }

    private fun sseLines(body: okhttp3.ResponseBody, onEvent: (String) -> Unit) {
        val source = body.source()
        while (true) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data:")) {
                val payload = line.removePrefix("data:").trim()
                if (payload.isNotEmpty() && payload != "[DONE]") onEvent(payload)
            }
        }
    }

    private fun streamOpenAiFormat(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): CloudGeneration {
        val url = when (provider) {
            AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
            AiProvider.DEEPSEEK -> "https://api.deepseek.com/v1/chat/completions"
            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            else -> provider.baseUrl + "chat/completions"
        }
        val jsonBody = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put(
                "messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userContent))
            )
            .put("max_tokens", maxTokens)

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Authorization", "Bearer $apiKey")
            .build()

        val content = StringBuilder()
        val reasoning = StringBuilder()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("${provider.displayName} ${response.code}: ${response.body?.string()?.take(300)}")
            }
            val body = response.body ?: throw java.io.IOException("Empty body")
            sseLines(body) { payload ->
                val obj = JSONObject(payload)
                val delta = obj.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") ?: return@sseLines
                delta.optString("reasoning_content", "").takeIf { it.isNotEmpty() }?.let { reasoning.append(it) }
                delta.optString("reasoning", "").takeIf { it.isNotEmpty() }?.let { reasoning.append(it) }
                val piece = delta.optString("content", "")
                if (piece.isNotEmpty()) {
                    content.append(piece)
                    kotlinx.coroutines.runBlocking { onDelta(piece) }
                }
            }
        }
        return CloudGeneration(content.toString(), reasoning.toString().takeIf { it.isNotBlank() })
    }

    private fun streamGemini(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): CloudGeneration {
        val normalizedModel = if (model.startsWith("models/")) model else "models/$model"
        val url = "https://generativelanguage.googleapis.com/v1beta/$normalizedModel:streamGenerateContent?alt=sse"

        val partObj = JSONObject().put("text", userContent)
        val contentObj = JSONObject().put("parts", JSONArray().put(partObj)).put("role", "user")
        val generationConfig = JSONObject().put("maxOutputTokens", maxTokens)
        geminiThinkingBudget(reasoningLevel())?.let { budget ->
            generationConfig.put("thinkingConfig", JSONObject().put("thinkingBudget", budget))
        }
        val jsonBody = JSONObject()
            .put("contents", JSONArray().put(contentObj))
            .put("generationConfig", generationConfig)
        if (systemPrompt.isNotBlank()) {
            jsonBody.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("x-goog-api-key", apiKey)
            .build()

        val content = StringBuilder()
        val thoughts = StringBuilder()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Gemini ${response.code}: ${response.body?.string()?.take(300)}")
            }
            val body = response.body ?: throw java.io.IOException("Empty body")
            sseLines(body) { payload ->
                val candidates = JSONObject(payload).optJSONArray("candidates") ?: return@sseLines
                val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: return@sseLines
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val text = part.optString("text", "")
                    if (text.isEmpty()) continue
                    if (part.optBoolean("thought", false)) {
                        thoughts.append(text)
                    } else {
                        content.append(text)
                        kotlinx.coroutines.runBlocking { onDelta(text) }
                    }
                }
            }
        }
        return CloudGeneration(content.toString(), thoughts.toString().takeIf { it.isNotBlank() })
    }

    private fun streamClaude(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): CloudGeneration {
        val url = "https://api.anthropic.com/v1/messages"

        val msgObj = JSONObject().put("role", "user").put("content", userContent)
        val jsonBody = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put("messages", JSONArray().put(msgObj))
            .put("max_tokens", maxTokens)

        val level = reasoningLevel()
        if (level == AiPreferences.REASONING_LOW || level == AiPreferences.REASONING_MEDIUM || level == AiPreferences.REASONING_HIGH) {
            val budget = when (level) {
                AiPreferences.REASONING_LOW -> 2048
                AiPreferences.REASONING_MEDIUM -> 8192
                else -> 16384
            }
            jsonBody.put("max_tokens", maxTokens + budget)
            jsonBody.put("thinking", JSONObject().put("type", "enabled").put("budget_tokens", budget))
        }
        if (systemPrompt.isNotBlank()) {
            jsonBody.put(
                "system", JSONArray().put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", systemPrompt)
                        .put("cache_control", JSONObject().put("type", "ephemeral"))
                )
            )
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .build()

        val content = StringBuilder()
        val thinking = StringBuilder()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Claude ${response.code}: ${response.body?.string()?.take(300)}")
            }
            val body = response.body ?: throw java.io.IOException("Empty body")
            var currentBlockType = "text"
            sseLines(body) { payload ->
                val obj = JSONObject(payload)
                when (obj.optString("type")) {
                    "content_block_start" -> {
                        currentBlockType = obj.optJSONObject("content_block")?.optString("type", "text") ?: "text"
                    }
                    "content_block_delta" -> {
                        val delta = obj.optJSONObject("delta") ?: return@sseLines
                        when (delta.optString("type")) {
                            "thinking_delta" -> thinking.append(delta.optString("thinking", ""))
                            "text_delta" -> {
                                val piece = delta.optString("text", "")
                                if (piece.isNotEmpty() && currentBlockType == "text") {
                                    content.append(piece)
                                    kotlinx.coroutines.runBlocking { onDelta(piece) }
                                }
                            }
                        }
                    }
                }
            }
        }
        return CloudGeneration(content.toString(), thinking.toString().takeIf { it.isNotBlank() })
    }

    private fun parseGeminiResult(jsonStr: String): CloudGeneration {
        val root = JSONObject(jsonStr)
        val candidates = root.optJSONArray("candidates") ?: return CloudGeneration("Error: empty response")
        if (candidates.length() == 0) return CloudGeneration("Error: no candidate generated")
        val first = candidates.getJSONObject(0)
        val content = first.optJSONObject("content") ?: return CloudGeneration("Error: no content")
        val parts = content.optJSONArray("parts") ?: return CloudGeneration("Error: no parts")
        val sb = StringBuilder()
        val thoughts = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.optBoolean("thought", false)) {
                thoughts.append(part.optString("text", ""))
            } else {
                sb.append(part.optString("text", ""))
            }
        }
        return CloudGeneration(sb.toString(), thoughts.toString().takeIf { it.isNotBlank() })
    }

    private fun parseOpenAiResult(jsonStr: String): CloudGeneration {
        val root = JSONObject(jsonStr)
        val choices = root.optJSONArray("choices") ?: return CloudGeneration("Error: empty response")
        if (choices.length() == 0) return CloudGeneration("Error: no choices")
        val message = choices.getJSONObject(0).optJSONObject("message") ?: return CloudGeneration("Error: no message")
        val reasoning = message.optString("reasoning_content", "")
            .ifBlank { message.optString("reasoning", "") }
            .ifBlank { null }
        return CloudGeneration(message.optString("content", ""), reasoning)
    }

    private fun parseClaudeResult(jsonStr: String): CloudGeneration {
        val root = JSONObject(jsonStr)
        val content = root.optJSONArray("content") ?: return CloudGeneration("Error: empty response")
        if (content.length() == 0) return CloudGeneration("Error: no content")
        val sb = StringBuilder()
        val thinking = StringBuilder()
        for (i in 0 until content.length()) {
            val block = content.getJSONObject(i)
            when (block.optString("type")) {
                "thinking" -> thinking.append(block.optString("thinking", ""))
                else -> sb.append(block.optString("text", ""))
            }
        }
        return CloudGeneration(sb.toString(), thinking.toString().takeIf { it.isNotBlank() })
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
