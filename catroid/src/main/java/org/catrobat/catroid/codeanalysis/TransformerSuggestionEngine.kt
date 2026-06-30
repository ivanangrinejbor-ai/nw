package org.catrobat.catroid.codeanalysis

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.NN.OnnxSessionManager
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object TransformerSuggestionEngine {
    @Volatile
    private var word2id: Map<String, Int> = emptyMap()
    private var id2word: Map<Int, String> = emptyMap()
    private var window: Int = 256
    private var vocabSize: Int = 0
    @Volatile
    var loaded = false
        private set

    fun isLoaded(): Boolean = loaded

    private const val PAD_ID = 0
    private const val UNK_ID = 1

    private var scriptStartId: Int = 3
    private var scriptEndId: Int = 2

    private const val ONNX_ASSET = "transformer_model.onnx"
    private const val METADATA_ASSET = "model_metadata.json"
    private const val VOCAB_ASSET = "vocab.json"

    fun init(context: Context) {
        if (loaded) return
        try {
            val onnxFile = copyAssetToCache(context, ONNX_ASSET)
                ?: run { loaded = false; return }

            val meta = loadJSON(context, METADATA_ASSET)
            if (meta != null) {
                window = meta.optInt("window", 256)
                vocabSize = meta.optInt("vocab_size", 0)
            }

            val vocab = loadJSON(context, VOCAB_ASSET)
            if (vocab != null) {
                val w2id = vocab.optJSONObject("word2id")
                if (w2id != null) {
                    word2id = w2id.keys().asSequence()
                        .filter { w2id.getInt(it) >= 0 }
                        .associate { it to w2id.getInt(it) }
                    id2word = word2id.entries.associate { (k, v) -> v to k }
                }
            }

            if (word2id.isEmpty() || vocabSize == 0) {
                loaded = false
                return
            }
            scriptStartId = word2id["<script_start>"] ?: 3
            scriptEndId = word2id["<script_end>"] ?: 2

            val ok = OnnxSessionManager.loadModel(onnxFile.absolutePath)
            loaded = ok
        } catch (e: Exception) {
            loaded = false
        }
    }

    private fun copyAssetToCache(context: Context, assetName: String): File? {
        return try {
            val cacheFile = File(context.cacheDir, assetName)
            if (!cacheFile.exists()) {
                context.assets.open(assetName).use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            cacheFile
        } catch (e: Exception) { null }
    }

    private fun loadJSON(context: Context, assetName: String): JSONObject? {
        return try {
            val input = context.assets.open(assetName)
            val text = BufferedReader(InputStreamReader(input)).readText()
            input.close()
            JSONObject(text)
        } catch (e: Exception) { null }
    }

    suspend fun predictNextAsync(script: Script, topN: Int = 3, parentBrick: Brick? = null): List<Suggestion> =
        withContext(Dispatchers.Default) {
            predictNext(script, topN, parentBrick)
        }

    fun predictNext(script: Script, topN: Int = 3, parentBrick: Brick? = null): List<Suggestion> {
        if (!loaded || !OnnxSessionManager.isWorking) return emptyList()

        val contextTokens = buildContextTokens(script, parentBrick)

        val existingTypes = if (parentBrick is CompositeBrick) {
            parentBrick.nestedBricks.map { it.javaClass.simpleName }.toMutableSet()
        } else {
            script.brickList.map { it.javaClass.simpleName }.toMutableSet()
        }

        val inputArray = FloatArray(window) { i ->
            if (i < contextTokens.size) contextTokens[i].toFloat() else PAD_ID.toFloat()
        }

        val future = OnnxSessionManager.predictAsync(inputArray)
        val outputArray = try {
            future?.get(5, TimeUnit.SECONDS)
        } catch (_: Exception) {
            null
        }

        val probs = outputArray ?: return emptyList()
        if (probs.size < vocabSize) return emptyList()

        softmax(probs)

        return probs.indices
            .filter { id2word[it] != null }
            .map { it to probs[it] }
            .sortedByDescending { it.second }
            .filter { (_, p) -> p > 0.01f }
            .filter { (id) ->
                val bt = id2word[id] ?: ""
                bt !in existingTypes && bt !in AiProjectAssistant.rejectedSuggestions && !bt.startsWith("[") && !bt.startsWith("<")
            }
            .take(topN)
            .map { (id, prob) ->
                val bt = id2word[id] ?: "Unknown"
                Suggestion(bt, "Transformer (${(prob*100).toInt().coerceIn(1,99)}%)", (prob*100).toInt().coerceIn(1,99))
            }
    }

    private fun buildContextTokens(script: Script, parentBrick: Brick? = null): List<Int> {
        val tokens = mutableListOf(scriptStartId)

        val ctx = AipContextManager.buildContextForParent(script, parentBrick)
        for (brickType in ctx) {
            tokens.add(encode(brickType))
        }
        tokens.add(scriptEndId)

        return if (tokens.size <= window) {
            tokens
        } else {
            listOf(scriptStartId) + tokens.takeLast(window - 1)
        }
    }

    private fun encode(brickType: String): Int = word2id[brickType] ?: UNK_ID

    private fun softmax(arr: FloatArray) {
        var maxVal = Float.NEGATIVE_INFINITY
        for (v in arr) if (v > maxVal) maxVal = v
        var sum = 0.0
        for (i in arr.indices) {
            arr[i] = Math.exp((arr[i] - maxVal).toDouble()).toFloat()
            sum += arr[i].toDouble()
        }
        if (sum > 0.0) for (i in arr.indices) arr[i] = (arr[i].toDouble() / sum).toFloat()
    }

    fun getSuggestionsForAllScripts(): List<Pair<Script, List<Suggestion>>> {
        if (!loaded) return emptyList()
        val sprite = ProjectManager.getInstance().getCurrentSprite() ?: return emptyList()
        return sprite.scriptList.mapNotNull { s ->
            val suggs = predictNext(s, 2)
            if (suggs.isNotEmpty()) s to suggs else null
        }
    }

    suspend fun getSuggestionsForAllScriptsAsync(): List<Pair<Script, List<Suggestion>>> =
        withContext(Dispatchers.Default) {
            getSuggestionsForAllScripts()
        }
}
