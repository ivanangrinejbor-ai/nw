package org.catrobat.catroid.codeanalysis

import android.content.Context
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.bricks.Brick
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NeuralSuggestionEngine {
    private var interpreter: Interpreter? = null
    private var word2id: Map<String, Int> = emptyMap()
    private var id2word: Map<Int, String> = emptyMap()
    private var window: Int = 500
    private var vocabSize: Int = 0
    private var loaded = false

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
                window = meta.optInt("window", 150)
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

    fun isLoaded(): Boolean = loaded

    /**
     * Predict next brick types given a script.
     * Builds a 500-token context from the ENTIRE project (all objects, all scripts)
     * to understand full context.
     */
    fun predictNext(script: Script, topN: Int = 3): List<Suggestion> {
        val engine = interpreter ?: return emptyList()
        val sprite = ProjectManager.getInstance().getCurrentSprite() ?: return emptyList()

        val contextTokens = buildProjectContext(listOf(script))

        val existingTypes = mutableSetOf<String>()
        for (b in script.brickList) {
            existingTypes.add(b.javaClass.simpleName)
        }

        val inputBuffer = ByteBuffer.allocateDirect(4 * window).apply {
            order(ByteOrder.nativeOrder())
            for (i in contextTokens.indices) {
                putInt(contextTokens[i])
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
        val indexed = probs.indices
            .filter { id2word[it] != null }
            .map { it to probs[it] }
            .sortedByDescending { it.second }
            .filter { (_, p) -> p > 0.01f }
            .filter { (id) ->
                val brickType = id2word[id] ?: ""
                brickType !in existingTypes && brickType !in AiProjectAssistant.rejectedSuggestions
                        && !brickType.startsWith("[")
            }
            .take(topN)

        return indexed.map { (id, prob) ->
            val brickType = id2word[id] ?: "Unknown"
            val confPct = (prob * 100).toInt().coerceIn(1, 99)
            Suggestion(brickType, "Neural: best match ($confPct%)", confPct)
        }
    }

    private fun buildProjectContext(allScripts: List<Script>): List<Int> {
        val tokens = mutableListOf(START_ID)
        for (s in allScripts) {
            for (b in s.brickList) {
                tokens.add(encode(b.javaClass.simpleName))
            }
            tokens.add(SEP_ID)
        }
        // Truncate/pad to window size
        return if (tokens.size < window) {
            val padded = MutableList(window) { PAD_ID }
            val start = window - tokens.size
            for (i in tokens.indices) {
                padded[start + i] = tokens[i]
            }
            padded
        } else {
            tokens.takeLast(window)
        }
    }

    private fun encode(brickType: String): Int = word2id[brickType] ?: UNK_ID

    fun getSuggestionsForAllScripts(): List<Pair<Script, List<Suggestion>>> {
        if (!loaded) return emptyList()
        val sprite = ProjectManager.getInstance().getCurrentSprite() ?: return emptyList()
        return sprite.scriptList.mapNotNull { script ->
            val suggs = predictNext(script, 2)
            if (suggs.isNotEmpty()) script to suggs else null
        }
    }
}
