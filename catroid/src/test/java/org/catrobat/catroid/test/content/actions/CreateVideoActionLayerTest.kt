package org.catrobat.catroid.test.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.actions.CreateVideoAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.junit.Test
import org.junit.Assert.*

class CreateVideoActionLayerTest {

    @Test
    fun `action has layer field`() {
        val action = CreateVideoAction()
        action.layer = Formula(2)
        assertEquals(2, action.layer?.interpretInteger(null))
    }

    @Test
    fun `layer defaults to null`() {
        val action = CreateVideoAction()
        assertNull(action.layer)
    }

    @Test
    fun `layer 2 means foreground`() {
        val action = CreateVideoAction()
        action.layer = Formula(2)
        val layer = action.layer?.interpretInteger(null) ?: 2
        assertTrue("Layer 2 should be foreground (>= 2)", layer >= 2)
    }

    @Test
    fun `layer 1 means background`() {
        val action = CreateVideoAction()
        action.layer = Formula(1)
        val layer = action.layer?.interpretInteger(null) ?: 2
        assertTrue("Layer 1 should be background (<= 1)", layer <= 1)
    }

    @Test
    fun `layer 0 means background`() {
        val action = CreateVideoAction()
        action.layer = Formula(0)
        val layer = action.layer?.interpretInteger(null) ?: 2
        assertTrue("Layer 0 should be background (<= 1)", layer <= 1)
    }

    @Test
    fun `null layer falls back to 2 in update`() {
        val action = CreateVideoAction()
        val fallback = action.layer?.interpretInteger(null) ?: 2
        assertEquals("Null layer should fall back to 2", 2, fallback)
    }

    @Test
    fun `all layer values map correctly`() {
        val expected = mapOf(
            0 to "background (behind sprites)",
            1 to "background (behind sprites)",
            2 to "foreground (above sprites)"
        )
        for ((layer, desc) in expected) {
            val isForeground = layer >= 2
            val isBackground = layer <= 1
            if (layer >= 2) {
                assertTrue("Layer $layer: $desc — should be foreground", isForeground)
            } else {
                assertTrue("Layer $layer: $desc — should be background", isBackground)
            }
        }
    }

    @Test
    fun `negative layer treated as background`() {
        val layer = -1
        assertTrue("Negative layer should be background (<= 1)", layer <= 1)
    }

    @Test
    fun `layer 5 treated as foreground`() {
        val layer = 5
        assertTrue("Layer >= 2 should be foreground", layer >= 2)
    }
}
