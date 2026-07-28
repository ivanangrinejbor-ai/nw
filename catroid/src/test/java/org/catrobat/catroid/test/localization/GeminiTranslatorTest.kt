package org.catrobat.catroid.test.localization

import org.catrobat.catroid.ai.localization.GeminiTranslator
import org.junit.Assert.*
import org.junit.Test

class GeminiTranslatorTest {

    @Test
    fun `parseTranslationResponse parses JSON array correctly`() {
        val result = invokePrivateParse("""["Привет", "Мир", "Тест"]""", 3)
        assertEquals(3, result.size)
        assertEquals("Привет", result[0])
        assertEquals("Мир", result[1])
        assertEquals("Тест", result[2])
    }

    @Test
    fun `parseTranslationResponse handles code-fenced JSON`() {
        val result = invokePrivateParse("""```json\n["Hello", "World"]\n```""", 2)
        assertEquals(2, result.size)
        assertEquals("Hello", result[0])
        assertEquals("World", result[1])
    }

    @Test
    fun `parseTranslationResponse falls back to line parsing`() {
        val result = invokePrivateParse("One\nTwo\nThree", 3)
        assertEquals(3, result.size)
        assertEquals("One", result[0])
        assertEquals("Two", result[1])
        assertEquals("Three", result[2])
    }

    @Test
    fun `parseTranslationResponse handles empty input`() {
        val result = invokePrivateParse("", 0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `translateBatch with empty texts returns success`() {
        val result = GeminiTranslator.translateBatch(
            mockContext(), emptyList(), "ru"
        )
        assertTrue(result.success)
        assertTrue(result.translatedTexts.isEmpty())
    }

    @Test
    fun `translateBatch with no API key returns failure`() {
        val result = GeminiTranslator.translateBatch(
            mockContext(), listOf("Hello"), "ru"
        )
        assertFalse(result.success)
        assertTrue(result.errorMessage?.contains("key") == true)
    }

    private fun mockContext(): android.content.Context {
        return org.mockito.Mockito.mock(android.content.Context::class.java)
    }

    private fun invokePrivateParse(text: String, expectedCount: Int): List<String> {
        val method = GeminiTranslator.javaClass.getDeclaredMethod(
            "parseTranslationResponse", String::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(GeminiTranslator, text, expectedCount) as List<String>
    }
}
