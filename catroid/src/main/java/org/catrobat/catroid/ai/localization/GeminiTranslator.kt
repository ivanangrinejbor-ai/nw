package org.catrobat.catroid.ai.localization

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.catrobat.catroid.content.GeminiManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiTranslator {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    private const val MODEL = "gemini-2.5-flash"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    data class TranslationResult(
        val success: Boolean,
        val translatedTexts: List<String> = emptyList(),
        val errorMessage: String? = null
    )

    fun translateBatch(
        context: Context,
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String = "auto"
    ): TranslationResult {
        if (texts.isEmpty()) return TranslationResult(true, emptyList())

        val apiKey = GeminiManager.getApiKey(context)
        if (apiKey.isNullOrEmpty()) return TranslationResult(false, errorMessage = "Gemini API key not set")

        val prompt = buildString {
            append("You are a translator. Translate the following texts")
            if (sourceLanguage != "auto") append(" from $sourceLanguage")
            append(" to $targetLanguage.\n")
            append("Return ONLY a JSON array of translated strings in exact same order.\n")
            append("Preserve variables like {name}, {coins} unchanged.\n")
            append("Do NOT add explanations, notes, or formatting.\n\n")
            append("Input texts:\n")
            texts.forEachIndexed { i, t -> append("""[$i] "$t"""" + "\n") }
            append("\nOutput format: [\"translated1\", \"translated2\", ...]")
        }

        return try {
            val body = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("topP", 0.95)
                    put("maxOutputTokens", 2048)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/models/$MODEL:generateContent?key=$apiKey")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return TranslationResult(false, errorMessage = "Empty response")

            if (!response.isSuccessful) {
                return TranslationResult(false, errorMessage = "Gemini API error: ${response.code} ${response.message}")
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return TranslationResult(false, errorMessage = "No candidates in Gemini response")
            }

            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                return TranslationResult(false, errorMessage = "No parts in Gemini response")
            }

            val responseText = parts.getJSONObject(0).optString("text", "")
            val translated = parseTranslationResponse(responseText, texts.size)

            if (translated.size != texts.size) {
                return TranslationResult(false,
                    errorMessage = "Translation count mismatch: expected ${texts.size}, got ${translated.size}",
                    translatedTexts = translated)
            }

            TranslationResult(true, translated)
        } catch (e: Exception) {
            TranslationResult(false, errorMessage = "Translation failed: ${e.message}")
        }
    }

    private fun parseTranslationResponse(text: String, expectedCount: Int): List<String> {
        val cleaned = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(cleaned, type)
        } catch (e1: Exception) {
            try {
                val arr = JSONArray(cleaned)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e2: Exception) {
                cleaned.lines()
                    .mapNotNull { line ->
                        val trimmed = line.trim()
                            .removePrefix("\"")
                            .removeSuffix("\"")
                            .removeSuffix(",")
                            .trim()
                        if (trimmed.isNotEmpty() && trimmed != "[" && trimmed != "]") trimmed else null
                    }
            }
        }
    }
}
