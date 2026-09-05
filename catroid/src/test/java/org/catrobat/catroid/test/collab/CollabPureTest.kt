package org.catrobat.catroid.test.collab

import org.catrobat.catroid.collab.BorderSegments
import org.catrobat.catroid.collab.CollabCodes
import org.catrobat.catroid.collab.HsvColor
import org.catrobat.catroid.collab.PresenceColors
import org.catrobat.catroid.collab.PresenceFreshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.abs

@RunWith(JUnit4::class)
class CollabPureTest {

    private fun circularGap(a: Float, b: Float): Float {
        var d = abs(a - b) % 360f
        if (d > 180f) d = 360f - d
        return d
    }

    @Test
    fun huesSpreadApart() {
        val taken = ArrayList<Float>()
        repeat(5) {
            taken.add(PresenceColors.hueFor(taken))
        }
        var minGap = 360f
        for (i in taken.indices) {
            for (j in i + 1 until taken.size) {
                minGap = minOf(minGap, circularGap(taken[i], taken[j]))
            }
        }
        assertTrue("minGap=$minGap taken=$taken", minGap > 20f)
    }

    @Test
    fun hueInRange() {
        repeat(50) {
            val hue = PresenceColors.hueFor(emptyList())
            assertTrue(hue in 0f..360f)
        }
    }

    @Test
    fun hsvRedIsRed() {
        val color = HsvColor.hsvToRgb(0f, 0.78f, 0.95f)
        assertEquals(0xFF, (color ushr 24) and 0xFF)
        assertTrue(((color shr 16) and 0xFF) > 200)
        assertTrue((color and 0xFF) < 100)
    }

    @Test
    fun hsvWrapsAround() {
        assertEquals(HsvColor.hsvToRgb(0f, 0.5f, 0.5f), HsvColor.hsvToRgb(360f, 0.5f, 0.5f))
        assertEquals(HsvColor.hsvToRgb(10f, 0.5f, 0.5f), HsvColor.hsvToRgb(-350f, 0.5f, 0.5f))
    }

    @Test
    fun colorIntIsOpaqueDeterministic() {
        val first = PresenceColors.colorInt(123f)
        assertEquals(first, PresenceColors.colorInt(123f))
        assertEquals(0xFF, (first ushr 24) and 0xFF)
    }

    @Test
    fun initials() {
        assertEquals("ПЕ", PresenceColors.initials("Петя"))
        assertEquals("AB", PresenceColors.initials("anya belova"))
        assertEquals("X", PresenceColors.initials("x"))
        assertEquals("?", PresenceColors.initials("   "))
    }

    @Test
    fun freshnessBoundaries() {
        assertTrue(PresenceFreshness.isFresh(1000L, 1500L, 1000L))
        assertTrue(PresenceFreshness.isFresh(1000L, 2000L, 1000L))
        assertFalse(PresenceFreshness.isFresh(1000L, 2001L, 1000L))
        assertFalse(PresenceFreshness.isFresh(0L, 1000L, 1000L))
        assertFalse(PresenceFreshness.isFresh(3000L, 1000L, 1000L))
    }

    @Test
    fun segmentsCoverPerimeter() {
        val segments = BorderSegments.compute(100f, 4)
        assertEquals(4, segments.size)
        assertEquals(0f, segments[0].start)
        assertEquals(25f, segments[1].start)
        assertEquals(25f, segments[1].length)
        val last = segments.last()
        assertEquals(100f, last.start + last.length)
    }

    @Test
    fun segmentsSingleIsWhole() {
        val segments = BorderSegments.compute(100f, 1)
        assertEquals(1, segments.size)
        assertEquals(0f, segments[0].start)
        assertEquals(100f, segments[0].length)
    }

    @Test
    fun segmentsDegenerate() {
        assertTrue(BorderSegments.compute(0f, 3).isEmpty())
        assertTrue(BorderSegments.compute(100f, 0).isEmpty())
    }

    @Test
    fun segmentPhaseStartsColorAtItsOffset() {
        assertEquals(75f, BorderSegments.phaseFor(100f, 25f))
        assertEquals(0f, BorderSegments.phaseFor(100f, 0f))
    }

    @Test
    fun codeFormats() {
        assertTrue(CollabCodes.isValidSessionId("ABC234"))
        assertFalse(CollabCodes.isValidSessionId("ABC12"))
        assertFalse(CollabCodes.isValidSessionId("abc234"))
        assertFalse(CollabCodes.isValidSessionId("ABC1O3"))
        assertFalse(CollabCodes.isValidSessionId("ABC103"))
        assertTrue(CollabCodes.isValidInviteCode("456789"))
        assertFalse(CollabCodes.isValidInviteCode("45678A"))
    }

    @Test
    fun generatedCodesValid() {
        repeat(100) {
            assertTrue(CollabCodes.isValidSessionId(CollabCodes.randomSessionId()))
            assertTrue(CollabCodes.isValidInviteCode(CollabCodes.randomInviteCode()))
        }
    }
}
