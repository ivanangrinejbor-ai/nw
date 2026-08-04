package org.catrobat.catroid.ai.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderTest {

    @Test
    fun opencode_registered_withFreeModelsOnly() {
        val provider = AiProvider.fromId("opencode")
        assertEquals(AiProvider.OPENCODE, provider)
        assertEquals("OpenCode", provider.displayName)
        assertEquals("https://opencode.ai/zen/v1/", provider.baseUrl)
        assertTrue(provider.defaultModels.isNotEmpty())
        for (model in provider.defaultModels) {
            val isFree = model.endsWith("-free") || model == "big-pickle"
            assertTrue("$model is not a free model", isFree)
        }
    }

    @Test
    fun fromId_isCaseInsensitive() {
        assertEquals(AiProvider.OPENCODE, AiProvider.fromId("OpenCode"))
        assertEquals(AiProvider.OPENCODE, AiProvider.fromId("OPENCODE"))
    }

    @Test
    fun unknownId_fallsBackToGemini() {
        assertEquals(AiProvider.GEMINI, AiProvider.fromId("no-such-provider"))
        assertEquals(AiProvider.GEMINI, AiProvider.fromId(null))
    }

    @Test
    fun everyProvider_hasDefaults() {
        for (provider in AiProvider.values()) {
            assertTrue("${provider.id} has no default models", provider.defaultModels.isNotEmpty())
            assertTrue("${provider.id} has blank baseUrl", provider.baseUrl.isNotBlank())
        }
    }
}
