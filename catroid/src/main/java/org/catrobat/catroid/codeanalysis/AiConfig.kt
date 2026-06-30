package org.catrobat.catroid.codeanalysis

import android.app.ActivityManager
import android.content.Context
import android.preference.PreferenceManager
import org.catrobat.catroid.CatroidApplication

object AiConfig {

    private const val PREF_KEY_MAX_TOKENS = "ai_max_tokens"
    private const val DEFAULT_TOKENS = 2000
    private const val MIN_TOKENS = 500
    private const val MAX_TOKENS_MOBILE = 40_000
    private const val MAX_TOKENS_PC = 50_000
    private const val WARN_THRESHOLD = 25_000

    var context: Context? = null
        private set

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    val maxTokens: Int
        get() {
            val ctx = context ?: return DEFAULT_TOKENS
            val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
            val raw = try { prefs.getString(PREF_KEY_MAX_TOKENS, "$DEFAULT_TOKENS")?.toIntOrNull() ?: DEFAULT_TOKENS } catch (_: Exception) { DEFAULT_TOKENS }
            return raw.coerceIn(MIN_TOKENS, maxLimit)
        }

    val maxLimit: Int
        get() = if (isPcMode()) MAX_TOKENS_PC else MAX_TOKENS_MOBILE

    val needsWarning: Boolean
        get() = maxTokens > WARN_THRESHOLD

    private fun availableRamMB(ctx: Context): Long {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024L * 1024L)
    }

    private fun estimatedMemoryMB(tokens: Int): Long {
        val hiddenDim = 128L
        val bytesPerElement = 2L
        return tokens * hiddenDim * bytesPerElement / (1024L * 1024L)
    }

    fun hasEnoughRamForInference(): Boolean {
        if (maxTokens <= WARN_THRESHOLD) return true
        val ctx = context ?: return true
        val avail = availableRamMB(ctx)
        val needed = estimatedMemoryMB(maxTokens) * 2L
        return avail > needed
    }

    fun warningMessage(): String {
        if (!needsWarning) return ""
        val ctx = context ?: return ""
        val avail = availableRamMB(ctx)
        val est = estimatedMemoryMB(maxTokens) * 2L
        return "High token limit ($maxTokens). ~${est}MB RAM needed, ${avail}MB available. May cause lag."
    }

    fun setMaxTokens(value: Int) {
        val ctx = context ?: return
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
            .putString(PREF_KEY_MAX_TOKENS, value.coerceIn(MIN_TOKENS, maxLimit).toString())
            .apply()
    }

    private fun isPcMode(): Boolean {
        val ctx = context ?: return false
        val cfg = ctx.resources.configuration
        return cfg.screenWidthDp >= 600 && cfg.screenHeightDp >= 600
    }

    fun estimateTokens(text: String): Int = text.split("\\s+".toRegex()).size.coerceAtLeast(1)
}
