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
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NeoThemeParserTest {

    @Test
    fun testValidFileParsesAllFields() {
        val text = """
            name=Ocean
            author=Ivan
            toolbar=#0B2545
            background=#13315C
            button=#1B4A7A
            accent=#8DA9C4
        """.trimIndent()

        val palette = NeoThemeParser.parse(text)

        assertEquals("Ocean", palette.name)
        assertEquals("Ivan", palette.author)
        assertEquals(0xFF0B2545.toInt(), palette.toolbar)
        assertEquals(0xFF13315C.toInt(), palette.background)
        assertEquals(0xFF1B4A7A.toInt(), palette.button)
        assertEquals(0xFF8DA9C4.toInt(), palette.accent)
    }

    @Test
    fun testMissingKeysFallBackToDefaults() {
        val palette = NeoThemeParser.parse("name=Partial\ntoolbar=#101010")

        assertEquals("Partial", palette.name)
        assertNull(palette.author)
        assertEquals(0xFF101010.toInt(), palette.toolbar)
        assertEquals(ThemePalette.DEFAULT_BACKGROUND, palette.background)
        assertEquals(ThemePalette.DEFAULT_BUTTON, palette.button)
        assertEquals(ThemePalette.DEFAULT_ACCENT, palette.accent)
    }

    @Test
    fun testUnknownKeysAndCommentsIgnored() {
        val text = """
            # This is a comment
            name=Test
            unknownKey=whatever
            
            accent=#FF0000
        """.trimIndent()

        val palette = NeoThemeParser.parse(text)

        assertEquals("Test", palette.name)
        assertEquals(0xFFFF0000.toInt(), palette.accent)
    }

    @Test
    fun testArgbEightDigitColor() {
        val palette = NeoThemeParser.parse("accent=#80FF0000")
        assertEquals(0x80FF0000.toInt(), palette.accent)
    }

    @Test
    fun testColorWithoutHashParses() {
        assertEquals(0xFF112233.toInt(), NeoThemeParser.parseHexColor("112233"))
    }

    @Test(expected = NeoThemeException::class)
    fun testInvalidHexLengthThrows() {
        NeoThemeParser.parseHexColor("#12345")
    }

    @Test(expected = NeoThemeException::class)
    fun testNonHexCharactersThrow() {
        NeoThemeParser.parseHexColor("#GGGGGG")
    }

    @Test
    fun testSerializeRoundTrip() {
        val original = ThemePalette(
            name = "RoundTrip",
            author = "Tester",
            toolbar = 0xFF010203.toInt(),
            background = 0xFF040506.toInt(),
            button = 0xFF070809.toInt(),
            accent = 0xFF0A0B0C.toInt()
        )

        val reparsed = NeoThemeParser.parse(NeoThemeParser.serialize(original))

        assertEquals(original, reparsed)
    }
}
