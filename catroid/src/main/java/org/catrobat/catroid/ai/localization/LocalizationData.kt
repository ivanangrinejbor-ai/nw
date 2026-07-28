package org.catrobat.catroid.ai.localization

import android.graphics.Rect

data class TextRegion(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect,
    val textColor: Int,
    val backgroundColor: Int,
    val estimatedFontSize: Float,
    val confidence: Float = 1.0f,
    val outlineColor: Int = 0,
    val outlineWidth: Float = 0f,
    val rotationAngle: Float = 0f,
    val lineRef: Any? = null
)

data class SpriteLocalizationResult(
    val spriteName: String,
    val lookName: String,
    val regions: List<TextRegion>,
    val success: Boolean,
    val errorMessage: String? = null,
    val outputPath: String? = null
)

data class LocalizationReport(
    val targetLanguage: String,
    val totalSprites: Int,
    val processedSprites: Int,
    val failedSprites: Int,
    val results: List<SpriteLocalizationResult>,
    val startTime: Long,
    val endTime: Long,
    val avgOcrConfidence: Float = 0f,
    val avgTextExpansion: Float = 0f,
    val geminiRequestCount: Int = 0,
    val spritesWithText: Int = 0
) {
    val successRate: Float get() = if (totalSprites > 0) processedSprites.toFloat() / totalSprites else 0f
    val durationMs: Long get() = endTime - startTime
    fun hasFailures(): Boolean = failedSprites > 0
    fun failureSummary(): String = results
        .filter { !it.success }
        .joinToString("\n") { "${it.spriteName}/${it.lookName}: ${it.errorMessage}" }

    fun toSummary(): String = buildString {
        appendLine("Localization Report")
        appendLine("=" .repeat(40))
        appendLine("Target language: $targetLanguage")
        appendLine("Duration: ${durationMs / 1000}s")
        appendLine()
        appendLine("Sprites found: $totalSprites")
        appendLine("Sprites with text: $spritesWithText")
        appendLine("Successfully processed: $processedSprites")
        if (failedSprites > 0) appendLine("Failed: $failedSprites")
        if (totalSprites > spritesWithText) appendLine("No text: ${totalSprites - spritesWithText}")
        appendLine("Success rate: ${"%.0f".format(successRate * 100)}%")
        appendLine()
        if (avgOcrConfidence > 0) appendLine("Avg OCR confidence: ${"%.1f".format(avgOcrConfidence * 100)}%")
        if (avgTextExpansion != 0f) appendLine("Avg text length change: ${if (avgTextExpansion > 0) "+" else ""}${"%.0f".format(avgTextExpansion * 100)}%")
        appendLine("Gemini API requests: $geminiRequestCount")
        if (hasFailures()) {
            appendLine()
            appendLine("Failures:")
            appendLine("-".repeat(20))
            appendLine(failureSummary())
        }
    }
}
