package org.catrobat.catroid.test.localization

import org.catrobat.catroid.ai.localization.LocalizationReport
import org.catrobat.catroid.ai.localization.SpriteLocalizationResult
import org.catrobat.catroid.ai.localization.TextRegion
import org.catrobat.catroid.ai.localization.TextRenderer
import org.junit.Assert.*
import org.junit.Test
import android.graphics.Rect
import android.graphics.Color

class LocalizationDataTest {

    @Test
    fun `LocalizationReport successRate calculation`() {
        val report = LocalizationReport(
            targetLanguage = "ru",
            totalSprites = 10,
            processedSprites = 8,
            failedSprites = 2,
            results = emptyList(),
            startTime = 1000L,
            endTime = 3000L
        )
        assertEquals(0.8f, report.successRate)
        assertEquals(2000L, report.durationMs)
        assertTrue(report.hasFailures())
    }

    @Test
    fun `LocalizationReport with no failures`() {
        val report = LocalizationReport(
            targetLanguage = "en",
            totalSprites = 5,
            processedSprites = 5,
            failedSprites = 0,
            results = emptyList(),
            startTime = 100L,
            endTime = 500L
        )
        assertEquals(1.0f, report.successRate)
        assertFalse(report.hasFailures())
        assertEquals("", report.failureSummary())
    }

    @Test
    fun `SpriteLocalizationResult error reporting`() {
        val successResult = SpriteLocalizationResult(
            spriteName = "Hero",
            lookName = "hero.png",
            regions = emptyList(),
            success = true,
            outputPath = "/path/to/hero_ru.png"
        )
        assertTrue(successResult.success)
        assertNull(successResult.errorMessage)

        val failureResult = SpriteLocalizationResult(
            spriteName = "Villain",
            lookName = "villain.png",
            regions = emptyList(),
            success = false,
            errorMessage = "OCR failed: no text found"
        )
        assertFalse(failureResult.success)
        assertEquals("OCR failed: no text found", failureResult.errorMessage)
    }

    @Test
    fun `TextRegion contains all necessary fields`() {
        val region = TextRegion(
            originalText = "Hello",
            translatedText = "Привет",
            boundingBox = Rect(10, 20, 100, 50),
            textColor = Color.BLACK,
            backgroundColor = Color.WHITE,
            estimatedFontSize = 24f,
            confidence = 0.95f
        )
        assertEquals("Hello", region.originalText)
        assertEquals("Привет", region.translatedText)
        assertEquals(10, region.boundingBox.left)
        assertEquals(20, region.boundingBox.top)
        assertEquals(100, region.boundingBox.right)
        assertEquals(50, region.boundingBox.bottom)
        assertEquals(Color.BLACK, region.textColor)
        assertEquals(Color.WHITE, region.backgroundColor)
        assertEquals(24f, region.estimatedFontSize)
        assertEquals(0.95f, region.confidence)
    }

    @Test
    fun `failureSummary aggregates failures`() {
        val results = listOf(
            SpriteLocalizationResult("A", "a.png", emptyList(), true, null, "/path/a.png"),
            SpriteLocalizationResult("B", "b.png", emptyList(), false, "Error B", null),
            SpriteLocalizationResult("C", "c.png", emptyList(), false, "Error C", null)
        )
        val report = LocalizationReport("ru", 3, 1, 2, results, 0L, 0L)
        val summary = report.failureSummary()
        assertTrue(summary.contains("B"))
        assertTrue(summary.contains("Error B"))
        assertTrue(summary.contains("C"))
        assertTrue(summary.contains("Error C"))
        assertTrue(!summary.contains("A"))
    }

    @Test
    fun `toSummary produces formatted report`() {
        val report = LocalizationReport(
            targetLanguage = "ru",
            totalSprites = 5,
            processedSprites = 4,
            failedSprites = 1,
            results = listOf(
                SpriteLocalizationResult("Hero", "h.png", emptyList(), true, null, "/p/h.png"),
                SpriteLocalizationResult("Fail", "f.png", emptyList(), false, "OCR error", null)
            ),
            startTime = 1000L, endTime = 41000L,
            avgOcrConfidence = 0.97f, avgTextExpansion = 0.18f,
            geminiRequestCount = 4, spritesWithText = 5
        )
        val summary = report.toSummary()
        assertTrue(summary.contains("ru"))
        assertTrue(summary.contains("40s"))
        assertTrue(summary.contains("97%"))
        assertTrue(summary.contains("+18%"))
        assertTrue(summary.contains("Gemini API requests: 4"))
        assertTrue(summary.contains("OCR error"))
    }

    @Test
    fun `report with zero failures shows no failure section`() {
        val report = LocalizationReport("en", 3, 3, 0, emptyList(), 0L, 100L)
        val summary = report.toSummary()
        assertFalse(summary.contains("Failures:"))
    }
}
