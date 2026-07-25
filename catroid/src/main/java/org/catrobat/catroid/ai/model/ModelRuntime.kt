package org.catrobat.catroid.ai.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File


object ModelRuntime {

    private const val TAG = "ModelRuntime"

    // Local on-device GGUF inference via llama.cpp (JNI). When the native library
    // is present it is used for models selected under the "local" backend; the
    // cloud (Gemini) backend remains available independently.
    private const val LOCAL_MODELS_ENABLED = true

    private var nativeLoaded = false
    private var nativeContext: Long = 0
    private var loadedPath: String? = null
    private val generateMutex = Mutex()

    init {
        if (!LOCAL_MODELS_ENABLED) {
            Log.i(TAG, "Local model support is disabled; skipping native library load")
            nativeLoaded = false
        } else {
            try {
                System.loadLibrary("ai_agent_jni")
                nativeLoaded = true
                Log.i(TAG, "Native library 'ai_agent_jni' loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native library not available: ${e.message}")
                nativeLoaded = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
                nativeLoaded = false
            }
        }
    }

    fun loadModel(modelFile: File, nCtx: Int = 512): Boolean {
        if (!LOCAL_MODELS_ENABLED) {
            Log.w(TAG, "Local models are disabled; refusing to load ${modelFile.name}")
            return false
        }
        if (!nativeLoaded) {
            Log.e(TAG, "Native library not loaded")
            return false
        }
        if (!modelFile.exists()) {
            Log.e(TAG, "Model file not found: ${modelFile.absolutePath}")
            return false
        }
        try {
            if (nativeContext != 0L) {
                unloadModel()
            }
            nativeContext = nativeLoadModel(modelFile.absolutePath, nCtx)
            if (nativeContext == 0L) {
                Log.e(TAG, "Failed to load model (native returned 0)")
                return false
            }
            loadedPath = modelFile.absolutePath
            Log.i(TAG, "Model loaded: ${modelFile.name}, ctx=$nativeContext")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading model", e)
            nativeContext = 0
            return false
        }
    }

    fun isModelLoaded(): Boolean = nativeContext != 0L

    fun getNativeContext(): Long = nativeContext

    fun unloadModel() {
        if (nativeContext != 0L) {
            try {
                nativeUnloadModel(nativeContext)
            } catch (e: Exception) {
                Log.e(TAG, "Error unloading model", e)
            }
            nativeContext = 0
            loadedPath = null
            Log.i(TAG, "Model unloaded")
        }
    }

    suspend fun generate(
        input: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 256
    ): String {
        return generateMutex.withLock {
            if (nativeLoaded && nativeContext != 0L) {
                try {
                    // Run blocking native inference on IO dispatcher so Default
                    // threads remain responsive and the UI doesn't freeze.
                    val raw = withContext(Dispatchers.IO) {
                        nativeGenerate(nativeContext, input, temperature, maxTokens)
                    }
                    val trimmed = raw.trim()
                    when {
                        trimmed.isEmpty() -> "Error: model generated empty response. The model may be incompatible or corrupted."
                        else -> trimmed
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Native generation failed", e)
                    "Error: generation failed - ${e.message}"
                }
            } else {
                "AI Agent not ready - no model loaded. Please download and load a GGUF model in Settings."
            }
        }
    }

    fun getLoadedPath(): String? = loadedPath

    private external fun nativeLoadModel(path: String, nCtx: Int): Long
    private external fun nativeUnloadModel(ctx: Long)
    private external fun nativeGenerate(ctx: Long, prompt: String, temperature: Float, maxTokens: Int): String
}
