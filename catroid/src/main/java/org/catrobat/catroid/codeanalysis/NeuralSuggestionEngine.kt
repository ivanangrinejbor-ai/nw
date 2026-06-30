package org.catrobat.catroid.codeanalysis

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NeuralSuggestionEngine {
    @Volatile
    private var interpreter: Interpreter? = null
    private var word2id: Map<String, Int> = emptyMap()
    private var id2word: Map<Int, String> = emptyMap()
    private var window: Int = 500
    private var vocabSize: Int = 0
    @Volatile
    var loaded = false
        private set

    fun isLoaded(): Boolean = loaded

    private const val PAD_ID = 0
    private const val UNK_ID = 1
    private const val SEP_ID = 2
    private const val START_ID = 3

    fun init(context: Context) {
        if (loaded) return
        try {
            val modelBytes = loadModelFile(context)
            interpreter = Interpreter(modelBytes)

            val meta = loadMetadata(context)
            if (meta != null) {
                window = meta.optInt("window", 500)
                vocabSize = meta.optInt("vocab_size", 0)
            }

            val vocab = loadVocab(context)
            if (vocab != null) {
                word2id = vocab.keys().asSequence()
                    .filter { vocab.getInt(it) >= 0 }
                    .associate { it to vocab.getInt(it) }
                id2word = word2id.entries.associate { (k, v) -> v to k }
            }

            loaded = interpreter != null && word2id.isNotEmpty() && vocabSize > 0
        } catch (e: Exception) {
            loaded = false
        }
    }

    private fun loadModelFile(context: Context): ByteBuffer {
        val afd = context.assets.openFd("model.tflite")
        val inputStream = afd.createInputStream()
        val bytes = inputStream.readBytes()
        inputStream.close()
        afd.close()
        return ByteBuffer.wrap(bytes)
    }

    private fun loadMetadata(context: Context): JSONObject? {
        return try {
            val input = context.assets.open("model_metadata.json")
            val text = BufferedReader(InputStreamReader(input)).readText()
            input.close()
            JSONObject(text)
        } catch (e: Exception) { null }
    }

    private fun loadVocab(context: Context): JSONObject? {
        return try {
            val input = context.assets.open("vocab.json")
            val text = BufferedReader(InputStreamReader(input)).readText()
            input.close()
            JSONObject(text).optJSONObject("word2id")
        } catch (e: Exception) { null }
    }

    /**
     * Async prediction using the new AipContextManager.
     * Runs TFLite inference on Dispatchers.Default to avoid UI freezes.
     */
    suspend fun predictNextAsync(script: Script, topN: Int = 3, parentBrick: Brick? = null): List<Suggestion> =
        withContext(Dispatchers.Default) {
            predictNext(script, topN, parentBrick)
        }

    fun predictNext(script: Script, topN: Int = 3, parentBrick: Brick? = null): List<Suggestion> {
        val engine = interpreter ?: return emptyList()
        val maxTokens = AiConfig.maxTokens

        val contextTokens = buildContextTokens(script, parentBrick)
        val actualWindow = maxTokens.coerceIn(1, window)

        val existingTypes = if (parentBrick is CompositeBrick) {
            parentBrick.nestedBricks.map { it.javaClass.simpleName }.toMutableSet()
        } else {
            script.brickList.map { it.javaClass.simpleName }.toMutableSet()
        }

        val inputBuffer = ByteBuffer.allocateDirect(4 * actualWindow).apply {
            order(ByteOrder.nativeOrder())
            for (i in 0 until actualWindow) {
                putInt(if (i < contextTokens.size) contextTokens[i] else PAD_ID)
            }
            rewind()
        }

        val outputBuffer = Array(1) { FloatArray(vocabSize) }
        try {
            engine.run(inputBuffer, outputBuffer)
        } catch (e: Exception) {
            return emptyList()
        }

        val probs = outputBuffer[0]
        return probs.indices
            .filter { id2word[it] != null }
            .map { it to probs[it] }
            .sortedByDescending { it.second }
            .filter { (_, p) -> p > 0.01f }
            .filter { (id) ->
                val bt = id2word[id] ?: ""
                bt !in existingTypes && bt !in AiProjectAssistant.rejectedSuggestions && !bt.startsWith("[")
            }
            .take(topN)
            .map { (id, prob) ->
                val bt = id2word[id] ?: "Unknown"
                Suggestion(bt, "Neural (${(prob*100).toInt().coerceIn(1,99)}%)", (prob*100).toInt().coerceIn(1,99))
            }
    }

    private fun buildContextTokens(script: Script, parentBrick: Brick? = null): List<Int> {
        val maxTokens = AiConfig.maxTokens.coerceIn(1, window)
        val tokens = mutableListOf(START_ID)

        val ctx = AipContextManager.buildContextForParent(script, parentBrick)
        for (brickType in ctx) {
            tokens.add(encode(brickType))
        }
        tokens.add(SEP_ID)

        return if (tokens.size <= maxTokens) {
            tokens
        } else {
            listOf(START_ID) + tokens.takeLast(maxTokens - 1)
        }
    }

    private fun encode(brickType: String): Int = word2id[brickType] ?: UNK_ID

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
