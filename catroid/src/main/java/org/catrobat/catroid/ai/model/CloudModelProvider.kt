package org.catrobat.catroid.ai.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CloudModelProvider {

    private const val TAG = "CloudModelProvider"

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchModelsForProvider(provider: AiProvider, apiKey: String?): List<String> {
        if (apiKey.isNullOrBlank()) return provider.defaultModels
        return try {
            withContext(Dispatchers.IO) {
                val fetched = when (provider) {
                    AiProvider.GEMINI -> fetchGeminiModels(apiKey)
                    AiProvider.OPENAI -> fetchOpenAiModels(apiKey)
                    AiProvider.DEEPSEEK -> fetchDeepSeekModels(apiKey)
                    AiProvider.OPENROUTER -> fetchOpenRouterModels(apiKey)
                    AiProvider.CLAUDE -> fetchClaudeModels(apiKey)
                }
                if (fetched.isEmpty()) provider.defaultModels else fetched.distinct().sorted()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch models for provider ${provider.displayName}", e)
            provider.defaultModels
        }
    }

    private fun fetchGeminiModels(apiKey: String): List<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=200"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("x-goog-api-key", apiKey)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val bodyStr = response.body?.string() ?: return emptyList()
            val root = JSONObject(bodyStr)
            val models = root.optJSONArray("models") ?: return emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until models.length()) {
                val item = models.optJSONObject(i) ?: continue
                val rawName = item.optString("name", "")
                val name = if (rawName.startsWith("models/")) rawName.substring(7) else rawName
                if (name.isNotBlank()) {
                    result.add(name)
                }
            }
            return result
        }
    }

    private fun fetchOpenAiModels(apiKey: String): List<String> {
        val url = "https://api.openai.com/v1/models"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val bodyStr = response.body?.string() ?: return emptyList()
            val root = JSONObject(bodyStr)
            val data = root.optJSONArray("data") ?: return emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id", "") ?: ""
                if (id.isNotBlank()) {
                    result.add(id)
                }
            }
            return result
        }
    }

    private fun fetchDeepSeekModels(apiKey: String): List<String> {
        val url = "https://api.deepseek.com/models"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val bodyStr = response.body?.string() ?: return emptyList()
            val root = JSONObject(bodyStr)
            val data = root.optJSONArray("data") ?: return emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id", "") ?: ""
                if (id.isNotBlank()) result.add(id)
            }
            return result
        }
    }

    private fun fetchOpenRouterModels(apiKey: String): List<String> {
        val url = "https://openrouter.ai/api/v1/models"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val bodyStr = response.body?.string() ?: return emptyList()
            val root = JSONObject(bodyStr)
            val data = root.optJSONArray("data") ?: return emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id", "") ?: ""
                if (id.isNotBlank()) result.add(id)
            }
            return result
        }
    }

    private fun fetchClaudeModels(apiKey: String): List<String> {
        val url = "https://api.anthropic.com/v1/models"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val bodyStr = response.body?.string() ?: return emptyList()
            val root = JSONObject(bodyStr)
            val data = root.optJSONArray("data") ?: return emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id", "") ?: ""
                if (id.isNotBlank()) result.add(id)
            }
            return result
        }
    }
}
