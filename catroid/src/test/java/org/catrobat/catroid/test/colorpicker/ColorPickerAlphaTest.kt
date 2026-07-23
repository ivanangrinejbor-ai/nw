/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.test.colorpicker

import android.graphics.Bitmap
import android.graphics.Color
import org.catrobat.catroid.formulaeditor.VisualizeColorString
import org.catrobat.catroid.formulaeditor.common.Conversions
import org.catrobat.catroid.formulaeditor.common.Conversions.isValidHexColor
import org.catrobat.catroid.utils.ShowTextUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ColorPickerAlphaTest {

    // ========================
    // Conversions.isValidHexColor
    // ========================

    @Test
    fun test01_validHexColor6Digits() {
        assertTrue("#FF0000".isValidHexColor())
    }

    @Test
    fun test02_validHexColor8DigitsWithAlpha() {
        assertTrue("#99FF0000".isValidHexColor())
    }

    @Test
    fun test03_validHexColor8DigitsFullAlpha() {
        assertTrue("#FFFF0000".isValidHexColor())
    }

    @Test
    fun test04_validHexColor8DigitsTransparent() {
        assertTrue("#00FF0000".isValidHexColor())
    }

    @Test
    fun test05_validHexColor8DigitsSemiTransparent() {
        assertTrue("#8000FF00".isValidHexColor())
    }

    @Test
    fun test06_invalidHexColorTooShort() {
        assertFalse("#FF00".isValidHexColor())
    }

    @Test
    fun test07_invalidHexColorTooLong() {
        assertFalse("#AABBCCDDEE".isValidHexColor())
    }

    @Test
    fun test08_invalidHexColorNoHash() {
        assertFalse("FF0000".isValidHexColor())
    }

    @Test
    fun test09_invalidHexColorLettersBeyondF() {
        assertFalse("#GG0000".isValidHexColor())
    }

    @Test
    fun test10_invalidHexColorNull() {
        val nullStr: String? = null
        assertFalse(nullStr.isValidHexColor())
    }

    // ========================
    // Conversions.tryParseColor
    // ========================

    @Test
    fun test11_tryParseColor6DigitsRed() {
        assertEquals(Color.RED, Conversions.tryParseColor("#FF0000"))
    }

    @Test
    fun test12_tryParseColor8DigitsSemiTransparentRed() {
        val color = Conversions.tryParseColor("#80FF0000")
        assertEquals(0x80, Color.alpha(color))
        assertEquals(255, Color.red(color))
        assertEquals(0, Color.green(color))
        assertEquals(0, Color.blue(color))
    }

    @Test
    fun test13_tryParseColor8DigitsFullyTransparent() {
        val color = Conversions.tryParseColor("#0000FF00")
        assertEquals(0, Color.alpha(color))
        assertEquals(0, Color.red(color))
        assertEquals(255, Color.green(color))
        assertEquals(0, Color.blue(color))
    }

    @Test
    fun test14_tryParseColor8DigitsFullAlphaGreen() {
        val color = Conversions.tryParseColor("#FF00FF00")
        assertEquals(255, Color.alpha(color))
        assertEquals(0, Color.red(color))
        assertEquals(255, Color.green(color))
        assertEquals(0, Color.blue(color))
    }

    @Test
    fun test15_tryParseColorInvalidReturnsDefaultBlack() {
        assertEquals(Color.BLACK, Conversions.tryParseColor("invalid"))
    }

    @Test
    fun test16_tryParseColorInvalidReturnsCustomDefault() {
        assertEquals(Color.WHITE, Conversions.tryParseColor("invalid", Color.WHITE))
    }

    @Test
    fun test17_tryParseColorNullReturnsDefault() {
        assertEquals(Color.BLACK, Conversions.tryParseColor(null))
    }

    @Test
    fun test18_tryParseColor8DigitsComplexColor() {
        val color = Conversions.tryParseColor("#993FFA02")
        assertEquals(0x99, Color.alpha(color))
        assertEquals(0x3F, Color.red(color))
        assertEquals(0xFA, Color.green(color))
        assertEquals(0x02, Color.blue(color))
    }

    // ========================
    // ShowTextUtils.isValidColorString
    // ========================

    @Test
    fun test19_isValidColorString6Digits() {
        assertTrue(ShowTextUtils.isValidColorString("#FF0000"))
    }

    @Test
    fun test20_isValidColorString8Digits() {
        assertTrue(ShowTextUtils.isValidColorString("#99FF0000"))
    }

    @Test
    fun test21_isValidColorStringTooShort() {
        assertFalse(ShowTextUtils.isValidColorString("#FF"))
    }

    @Test
    fun test22_isValidColorStringNull() {
        assertFalse(ShowTextUtils.isValidColorString(null))
    }

    @Test
    fun test23_isValidColorStringNoHash() {
        assertFalse(ShowTextUtils.isValidColorString("FF0000"))
    }

    // ========================
    // ShowTextUtils.convertColorToString
    // ========================

    @Test
    fun test24_convertColorToStringOpaqueRed() {
        val result = ShowTextUtils.convertColorToString(Color.RED)
        assertEquals("#FFFF0000", result)
    }

    @Test
    fun test25_convertColorToStringSemiTransparentGreen() {
        val color = Color.argb(128, 0, 255, 0)
        val result = ShowTextUtils.convertColorToString(color)
        assertEquals("#8000FF00", result)
    }

    @Test
    fun test26_convertColorToStringFullyTransparentBlue() {
        val color = Color.argb(0, 0, 0, 255)
        val result = ShowTextUtils.convertColorToString(color)
        assertEquals("#000000FF", result)
    }

    @Test
    fun test27_convertColorToStringComplexColor() {
        val color = Color.argb(0x99, 0x3F, 0xFA, 0x02)
        val result = ShowTextUtils.convertColorToString(color)
        assertEquals("#993FFA02", result)
    }

    @Test
    fun test28_convertColorToStringBlackOpaque() {
        val result = ShowTextUtils.convertColorToString(Color.BLACK)
        assertEquals("#FF000000", result)
    }

    @Test
    fun test29_convertColorToStringWhiteOpaque() {
        val result = ShowTextUtils.convertColorToString(Color.WHITE)
        assertEquals("#FFFFFFFF", result)
    }

    // ========================
    // ShowTextUtils.calculateColorRGBs (backward compat)
    // ========================

    @Test
    fun test30_calculateColorRGBs6Digits() {
        val rgb = ShowTextUtils.calculateColorRGBs("#FF8040")
        assertEquals(255, rgb[0])
        assertEquals(128, rgb[1])
        assertEquals(64, rgb[2])
    }

    @Test
    fun test31_calculateColorRGBs8Digits() {
        val rgb = ShowTextUtils.calculateColorRGBs("#99FF8040")
        assertEquals(255, rgb[0])
        assertEquals(128, rgb[1])
        assertEquals(64, rgb[2])
    }

    // ========================
    // VisualizeColorString (Robolectric)
    // ========================

    @Test
    fun test32_visualizeColorStringOpaqueColor() {
        val context = RuntimeEnvironment.getApplication()
        val viz = VisualizeColorString(context, "#FF0000", 48f)
        assertNotNull(viz.drawable)
        assertNotNull(viz.imageSpan)
        assertEquals(0xFFFF0000.toInt(), viz.colorValue)
    }

    @Test
    fun test33_visualizeColorStringWithAlpha() {
        val context = RuntimeEnvironment.getApplication()
        val viz = VisualizeColorString(context, "#80FF0000", 48f)
        assertNotNull(viz.drawable)
        assertEquals(0x80FF0000.toInt(), viz.colorValue)
    }

    @Test
    fun test34_visualizeColorStringFullyTransparent() {
        val context = RuntimeEnvironment.getApplication()
        val viz = VisualizeColorString(context, "#00000000", 48f)
        assertNotNull(viz.drawable)
        assertEquals(0x00000000, viz.colorValue)
    }

    @Test
    fun test35_visualizeColorString6DigitGetsFullAlpha() {
        val context = RuntimeEnvironment.getApplication()
        val viz = VisualizeColorString(context, "#FF0000", 48f)
        assertEquals(0xFFFF0000.toInt(), viz.colorValue)
    }

    @Test
    fun test36_visualizeColorStringBitmapIsARGB() {
        val context = RuntimeEnvironment.getApplication()
        val viz = VisualizeColorString(context, "#80FF0000", 48f)
        val bitmap = viz.drawable.bitmap
        assertNotNull(bitmap)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun test37_visualizeColorStringInvalidColorReturnsZero() {
        val context = RuntimeEnvironment.getApplication()
        val viz = VisualizeColorString(context, "invalid", 48f)
        assertEquals(0, viz.colorValue)
    }

    @Test
    fun test38_visualizeColorStringComplexColorWithAlpha() {
        val context = RuntimeEnvironment.getApplication()
        val viz = VisualizeColorString(context, "#993FFA02", 36f)
        assertEquals(0x993FFA02.toInt(), viz.colorValue)
        assertNotNull(viz.drawable)
    }

    // ========================
    // Round-trip: convertColorToString -> tryParseColor
    // ========================

    @Test
    fun test39_roundTripOpaqueColor() {
        val original = Color.argb(255, 100, 200, 50)
        val str = ShowTextUtils.convertColorToString(original)
        val parsed = Conversions.tryParseColor(str)
        assertEquals(original, parsed)
    }

    @Test
    fun test40_roundTripSemiTransparentColor() {
        val original = Color.argb(128, 100, 200, 50)
        val str = ShowTextUtils.convertColorToString(original)
        val parsed = Conversions.tryParseColor(str)
        assertEquals(original, parsed)
    }

    @Test
    fun test41_roundTripFullyTransparentColor() {
        val original = Color.argb(0, 255, 128, 64)
        val str = ShowTextUtils.convertColorToString(original)
        val parsed = Conversions.tryParseColor(str)
        assertEquals(original, parsed)
    }

    @Test
    fun test42_roundTripTheSpecificUserColor() {
        val original = Color.argb(0x99, 0x3F, 0xFA, 0x02)
        val str = ShowTextUtils.convertColorToString(original)
        assertEquals("#993FFA02", str)
        val parsed = Conversions.tryParseColor(str)
        assertEquals(original, parsed)
    }

    // ========================
    // Backward compatibility: 6-digit input still works
    // ========================

    @Test
    fun test43_backwardCompat6DigitColorStillValid() {
        assertTrue("#FF0000".isValidHexColor())
        val color = Conversions.tryParseColor("#FF0000")
        assertEquals(255, Color.alpha(color))
        assertEquals(255, Color.red(color))
    }

    @Test
    fun test44_backwardCompat6DigitGreenStillValid() {
        assertTrue("#00FF00".isValidHexColor())
        val color = Conversions.tryParseColor("#00FF00")
        assertEquals(255, Color.alpha(color))
        assertEquals(255, Color.green(color))
    }

    // ========================
    // Edge cases
    // ========================

    @Test
    fun test45_edgeCaseAllZeros8Digit() {
        val color = Conversions.tryParseColor("#00000000")
        assertEquals(0, Color.alpha(color))
        assertEquals(0, Color.red(color))
        assertEquals(0, Color.green(color))
        assertEquals(0, Color.blue(color))
    }

    @Test
    fun test46_edgeCaseAllFF8Digit() {
        val color = Conversions.tryParseColor("#FFFFFFFF")
        assertEquals(255, Color.alpha(color))
        assertEquals(255, Color.red(color))
        assertEquals(255, Color.green(color))
        assertEquals(255, Color.blue(color))
    }

    @Test
    fun test47_convertThenParseAllAlphaValues() {
        for (alpha in listOf(0, 1, 64, 128, 192, 255)) {
            val original = Color.argb(alpha, 42, 128, 200)
            val str = ShowTextUtils.convertColorToString(original)
            val parsed = Conversions.tryParseColor(str)
            assertEquals("Alpha mismatch for alpha=$alpha", original, parsed)
        }
    }

    @Test
    fun test48_visualizeOpaqueDoesNotNeedCheckerboard() {
        val context = RuntimeEnvironment.getApplication()
        val viz = VisualizeColorString(context, "#FFFF0000", 48f)
        assertNotNull(viz.drawable)
        assertEquals(Bitmap.Config.ARGB_8888, viz.drawable.bitmap.config)
    }
}
