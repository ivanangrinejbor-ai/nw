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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePaletteTest {

    @Test
    fun testDefaultMatchesColorConstants() {
        assertEquals(ThemePalette.DEFAULT_TOOLBAR, ThemePalette.DEFAULT.toolbar)
        assertEquals(ThemePalette.DEFAULT_BACKGROUND, ThemePalette.DEFAULT.background)
        assertEquals(ThemePalette.DEFAULT_BUTTON, ThemePalette.DEFAULT.button)
        assertEquals(ThemePalette.DEFAULT_ACCENT, ThemePalette.DEFAULT.accent)
    }

    @Test
    fun testDefaultPaletteIsDefault() {
        assertTrue(ThemePalette.DEFAULT.isDefault)
    }

    @Test
    fun testCustomColorsAreNotDefault() {
        val custom = ThemePalette.DEFAULT.copy(accent = 0xFF123456.toInt())
        assertFalse(custom.isDefault)
    }

    @Test
    fun testNamedPaletteWithDefaultColorsIsNotDefault() {
        val named = ThemePalette.DEFAULT.copy(name = "Named")
        assertFalse(named.isDefault)
    }

    @Test
    fun testOverrideMapContainsPaletteColors() {
        val palette = ThemePalette(
            name = "X",
            author = null,
            toolbar = 0xFF111111.toInt(),
            background = 0xFF222222.toInt(),
            button = 0xFF333333.toInt(),
            accent = 0xFF444444.toInt()
        )

        val values = palette.toResourceOverrideMap().values

        assertTrue(values.contains(0xFF111111.toInt()))
        assertTrue(values.contains(0xFF222222.toInt()))
        assertTrue(values.contains(0xFF333333.toInt()))
        assertTrue(values.contains(0xFF444444.toInt()))
    }
}
