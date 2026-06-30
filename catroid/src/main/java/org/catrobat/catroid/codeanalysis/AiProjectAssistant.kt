package org.catrobat.catroid.codeanalysis

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class Suggestion(
    val brickType: String,
    val reason: String,
    val confidence: Int,
    val scriptIndex: Int = 0
)

object AiProjectAssistant {
    private var ngrams: Map<Int, Map<String, Map<String, Double>>> = emptyMap()
    private var scriptSpecNgrams: Map<String, Map<Int, Map<String, Map<String, Double>>>> = emptyMap()
    private var firstBricks: Map<String, Int> = emptyMap()
    private var modelVersion: Int = 0
    private var loaded = false

    val rejectedSuggestions = mutableSetOf<String>()

    fun init(context: Context) {
        if (loaded) return
        AiConfig.init(context)

        // 1) Transformer ONNX (modelVersion = 4)
        try {
            TransformerSuggestionEngine.init(context)
            if (TransformerSuggestionEngine.isLoaded()) {
                loaded = true
                modelVersion = 4
                return
            }
        } catch (_: Exception) {
        }

        // 2) TFLite neural (modelVersion = 3)
        try {
            NeuralSuggestionEngine.init(context)
            if (NeuralSuggestionEngine.isLoaded()) {
                loaded = true
                modelVersion = 3
                return
            }
        } catch (_: Exception) {
        }

        // 3) N-gram patterns (modelVersion = 1/2)
        try {
            val inputStream = context.assets.open("patterns.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val text = reader.readText()
            val root = JSONObject(text)
            reader.close()

            modelVersion = root.optInt("model_version", 1)

            if (modelVersion >= 2) {
                parseV2Model(root)
            } else {
                parseV1Model(root)
            }

            loaded = true
        } catch (e: Exception) {
            loaded = false
        }
    }

    private fun parseV2Model(root: JSONObject) {
        val ngramsObj = root.optJSONObject("ngrams")
        if (ngramsObj != null) {
            val result = mutableMapOf<Int, MutableMap<String, MutableMap<String, Double>>>()
            for (nKey in ngramsObj.keys()) {
                val n = nKey.toIntOrNull() ?: continue
                val ctxObj = ngramsObj.getJSONObject(nKey)
                val ctxMap = mutableMapOf<String, MutableMap<String, Double>>()
                for (ctx in ctxObj.keys()) {
                    val predObj = ctxObj.getJSONObject(ctx)
                    val predMap = mutableMapOf<String, Double>()
                    for (pred in predObj.keys()) {
                        predMap[pred] = predObj.getDouble(pred)
                    }
                    ctxMap[ctx] = predMap
                }
                result[n] = ctxMap
            }
            ngrams = result
        }

        val ssObj = root.optJSONObject("script_specific")
        if (ssObj != null) {
            val ssResult = mutableMapOf<String, MutableMap<Int, MutableMap<String, MutableMap<String, Double>>>>()
            for (stype in ssObj.keys()) {
                val ngramsObj2 = ssObj.getJSONObject(stype)
                val nResult = mutableMapOf<Int, MutableMap<String, MutableMap<String, Double>>>()
                for (nKey in ngramsObj2.keys()) {
                    val n = nKey.toIntOrNull() ?: continue
                    val ctxObj = ngramsObj2.getJSONObject(nKey)
                    val ctxMap = mutableMapOf<String, MutableMap<String, Double>>()
                    for (ctx in ctxObj.keys()) {
                        val predObj = ctxObj.getJSONObject(ctx)
                        val predMap = mutableMapOf<String, Double>()
                        for (pred in predObj.keys()) {
                            predMap[pred] = predObj.getDouble(pred)
                        }
                        ctxMap[ctx] = predMap
                    }
                    nResult[n] = ctxMap
                }
                ssResult[stype] = nResult
            }
            scriptSpecNgrams = ssResult
        }

        val fbObj = root.optJSONObject("first_bricks")
        if (fbObj != null) {
            firstBricks = fbObj.keys().asSequence().map { it to fbObj.getInt(it) }.toMap()
        }
    }

    private fun parseV1Model(root: JSONObject) {
        ngrams = emptyMap()
        scriptSpecNgrams = emptyMap()

        val transitions = root.optJSONObject("transitions")
        if (transitions != null) {
            val n2 = mutableMapOf<String, MutableMap<String, Double>>()
            for (ctx in transitions.keys()) {
                val preds = transitions.getJSONObject(ctx)
                val total = preds.keys().asSequence().sumOf { preds.optInt(it, 0) }.toDouble()
                if (total > 0) {
                    val predMap = mutableMapOf<String, Double>()
                    for (pred in preds.keys()) {
                        predMap[pred] = preds.optInt(pred, 0) / total
                    }
                    n2[ctx] = predMap
                }
            }
            ngrams = mapOf(2 to n2)
        }

        val coOccur = root.optJSONObject("co_occurrence")
        if (coOccur != null) {
            val n2 = ngrams[2]?.toMutableMap() ?: mutableMapOf()
            for (ctx in coOccur.keys()) {
                val preds = coOccur.getJSONObject(ctx)
                val total = preds.keys().asSequence().sumOf { preds.optInt(it, 0) }.toDouble()
                if (total > 0 && !n2.containsKey(ctx)) {
                    val predMap = mutableMapOf<String, Double>()
                    for (pred in preds.keys()) {
                        predMap[pred] = preds.optInt(pred, 0) / total
                    }
                    n2[ctx] = predMap
                }
            }
            ngrams = mapOf(2 to n2)
        }

        val fbObj = root.optJSONObject("most_common_first_bricks")
        if (fbObj != null) {
            firstBricks = fbObj.keys().asSequence().mapNotNull { key ->
                val arr = fbObj.optJSONArray(key)
                if (arr != null && arr.length() >= 2) key to arr.optInt(1, 0) else null
            }.toMap()
        }
    }

    fun predictNext(script: Script, maxN: Int = 3, parentBrick: Brick? = null): List<Suggestion> {
        if (!loaded) return emptyList()

        if (modelVersion >= 4 && TransformerSuggestionEngine.isLoaded()) {
            return TransformerSuggestionEngine.predictNext(script, maxN, parentBrick)
        }

        if (modelVersion >= 3 && NeuralSuggestionEngine.isLoaded()) {
            return NeuralSuggestionEngine.predictNext(script, maxN, parentBrick)
        }

        val brickTypes = if (parentBrick is CompositeBrick) {
            parentBrick.nestedBricks.map { it.javaClass.simpleName }
        } else {
            AipContextManager.buildSimpleContext(script)
        }
        val result = mutableListOf<Suggestion>()
        val seen = mutableSetOf<String>()
        val existingTypes = brickTypes.toSet()

        if (brickTypes.isEmpty()) {
            for ((btype, count) in firstBricks.entries.sortedByDescending { it.value }.take(maxN)) {
                if (btype !in seen && btype !in existingTypes) {
                    seen.add(btype)
                    result.add(Suggestion(btype, "Common first brick in projects", count.coerceAtMost(100), 0))
                }
            }
            return result
        }

        val scriptType = script.javaClass.simpleName
        val ssNgrams = scriptSpecNgrams[scriptType]

        for (n in listOf(5, 4, 3, 2).filter { it <= brickTypes.size + 1 }) {
            val context = brickTypes.takeLast(n - 1)
            if (context.size < n - 1) continue
            val contextKey = context.joinToString("|")

            var predictions = ngrams[n]?.get(contextKey)
            if (ssNgrams != null) {
                val ssPreds = ssNgrams[n]?.get(contextKey)
                if (ssPreds != null) {
                    if (predictions != null) {
                        val merged = predictions.toMutableMap()
                        for ((k, v) in ssPreds) {
                            merged[k] = (merged[k] ?: 0.0) + v * 0.3
                        }
                        predictions = merged
                    } else {
                        predictions = ssPreds
                    }
                }
            }

            if (predictions != null) {
                val sorted = predictions.entries
                    .filter { it.key !in existingTypes && it.key !in seen && it.key !in rejectedSuggestions }
                    .sortedByDescending { it.value }
                    .take(maxN)

                for ((btype, prob) in sorted) {
                    seen.add(btype)
                    val confPct = (prob * 100).toInt().coerceIn(1, 99)
                    result.add(Suggestion(btype, "Predicts after: ${context.joinToString(", ")}", confPct, 0))
                }
                if (result.isNotEmpty()) return result
            }
        }

        val lastType = brickTypes.last()
        for (n in listOf(5, 4, 3, 2)) {
            val fallbackCtx = brickTypes.takeLast(n - 1)
            val fbkKey = fallbackCtx.joinToString("|")
            val fallbackPreds = ngrams[n]?.get(fbkKey)
            if (fallbackPreds != null) {
                val sorted = fallbackPreds.entries
                    .filter { it.key !in existingTypes && it.key !in seen && it.key !in rejectedSuggestions }
                    .sortedByDescending { it.value }
                    .take(maxN)
                for ((btype, prob) in sorted) {
                    seen.add(btype)
                    val confPct = (prob * 100).toInt().coerceIn(1, 99)
                    result.add(Suggestion(btype, "Often placed after $lastType", confPct, 0))
                }
                break
            }
        }

        return result
    }

    fun getSuggestionsForAllScripts(): List<Pair<Script, List<Suggestion>>> {
        val sprite = ProjectManager.getInstance().getCurrentSprite() ?: return emptyList()
        if (modelVersion >= 4 && TransformerSuggestionEngine.isLoaded()) {
            return TransformerSuggestionEngine.getSuggestionsForAllScripts()
        }
        if (modelVersion >= 3 && NeuralSuggestionEngine.isLoaded()) {
            return NeuralSuggestionEngine.getSuggestionsForAllScripts()
        }
        return sprite.scriptList.mapNotNull { script ->
            val suggestions = predictNext(script, 2)
            if (suggestions.isNotEmpty()) script to suggestions else null
        }
    }

    fun generateAnalysisResults(): Map<Brick, AnalysisResult> {
        val sprite = ProjectManager.getInstance().getCurrentSprite() ?: return emptyMap()
        val results = mutableMapOf<Brick, AnalysisResult>()
        val allBricks = mutableListOf<Brick>()
        for (script in sprite.scriptList) {
            script.addToFlatList(allBricks)
        }
        val existingTypes = allBricks.map { it.javaClass.simpleName }.toSet()

        for (script in sprite.scriptList) {
            val suggestions = predictNext(script, 1)
            if (script.brickList.isNotEmpty()) {
                val lastBrick = script.brickList.last()
                for (s in suggestions) {
                    if (s.brickType !in existingTypes) {
                        results[lastBrick] = AnalysisResult(
                            Severity.SUGGESTION,
                            "AI: +${s.brickType} (${s.reason})"
                        )
                    }
                }
            }
        }
        return results
    }

    fun isLoaded(): Boolean = loaded

    fun rejectSuggestion(brickType: String) {
        rejectedSuggestions.add(brickType)
    }

    fun clearRejectedSuggestions() {
        rejectedSuggestions.clear()
    }

    fun addSuggestedBrick(brickType: String): Brick? {
        val fullName = "org.catrobat.catroid.content.bricks.$brickType"
        val cls = try {
            Class.forName(fullName)
        } catch (e: Exception) { return null }
        return try {
            val constructor = cls.getDeclaredConstructor()
            constructor.isAccessible = true
            constructor.newInstance() as? Brick
        } catch (e: Exception) {
            try {
                val constructor = cls.getDeclaredConstructor(String::class.java)
                constructor.isAccessible = true
                constructor.newInstance("") as? Brick
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Async version of getSuggestionsForAllScripts.
     * Builds full context via AipContextManager, runs inference on Dispatchers.Default.
     * Checks RAM before running high-token-count inference.
     */
    suspend fun getSuggestionsForAllScriptsAsync(): List<Pair<Script, List<Suggestion>>> =
        withContext(Dispatchers.Default) {
            if (!loaded) return@withContext emptyList()
            if (!AiConfig.hasEnoughRamForInference()) return@withContext emptyList()
            getSuggestionsForAllScripts()
        }
}
