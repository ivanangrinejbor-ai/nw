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

import org.catrobat.catroid.R

/**
 * A curated palette parsed from a `.neotema` file. Colours are stored as ARGB ints.
 * The [name]/[author] are optional metadata; the default theme has both null.
 */
data class ThemePalette(
    val name: String?,
    val author: String?,
    val toolbar: Int,
    val background: Int,
    val button: Int,
    val accent: Int
) {
    /** True when this palette matches the built-in default (no custom overrides). */
    val isDefault: Boolean
        get() = name == null && author == null &&
            toolbar == DEFAULT_TOOLBAR &&
            background == DEFAULT_BACKGROUND &&
            button == DEFAULT_BUTTON &&
            accent == DEFAULT_ACCENT

    /**
     * Maps the curated palette keys onto the concrete `@color` resource ids that are
     * overridden at runtime by [ThemedResources].
     */
    fun toResourceOverrideMap(): Map<Int, Int> = linkedMapOf(
        R.color.toolbar_background to toolbar,
        R.color.app_background to background,
        R.color.app_background_dark to background,
        R.color.button_background to button,
        R.color.button_bottom_bar to button,
        R.color.accent to accent
    )

    companion object {
        // Defaults mirror res/values/colors.xml.
        const val DEFAULT_TOOLBAR = 0xFF1C1C1E.toInt()
        const val DEFAULT_BACKGROUND = 0xFF2C2C2E.toInt()
        const val DEFAULT_BUTTON = 0xFF48484A.toInt()
        const val DEFAULT_ACCENT = 0xFFB0BEC5.toInt()

        @JvmField
        val DEFAULT = ThemePalette(
            name = null,
            author = null,
            toolbar = DEFAULT_TOOLBAR,
            background = DEFAULT_BACKGROUND,
            button = DEFAULT_BUTTON,
            accent = DEFAULT_ACCENT
        )
    }
}
