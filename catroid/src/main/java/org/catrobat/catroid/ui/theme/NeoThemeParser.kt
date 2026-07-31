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

object NeoThemeParser {
    private const val MAX_META_LENGTH = 40

    fun parse(text: String): ThemePalette {
        var name: String? = null
        var author: String? = null
        var toolbar = ThemePalette.DEFAULT_TOOLBAR
        var background = ThemePalette.DEFAULT_BACKGROUND
        var button = ThemePalette.DEFAULT_BUTTON
        var accent = ThemePalette.DEFAULT_ACCENT

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || (line.startsWith("#") && !line.contains("="))) {
                return@forEach
            }
            val sepIndex = line.indexOf('=')
            if (sepIndex <= 0) {
                return@forEach
            }
            val key = line.substring(0, sepIndex).trim().lowercase()
            val value = line.substring(sepIndex + 1).trim()
            when (key) {
                "name" -> name = value.take(MAX_META_LENGTH)
                "author" -> author = value.take(MAX_META_LENGTH)
                "toolbar" -> toolbar = parseHexColor(value)
                "background" -> background = parseHexColor(value)
                "button" -> button = parseHexColor(value)
                "accent" -> accent = parseHexColor(value)
                else -> Unit
            }
        }

        return ThemePalette(
            name = name?.takeIf { it.isNotEmpty() },
            author = author?.takeIf { it.isNotEmpty() },
            toolbar = toolbar,
            background = background,
            button = button,
            accent = accent
        )
    }

    fun serialize(palette: ThemePalette): String = buildString {
        palette.name?.let { append("name=").append(it).append('\n') }
        palette.author?.let { append("author=").append(it).append('\n') }
        append("toolbar=").append(toHex(palette.toolbar)).append('\n')
        append("background=").append(toHex(palette.background)).append('\n')
        append("button=").append(toHex(palette.button)).append('\n')
        append("accent=").append(toHex(palette.accent)).append('\n')
    }

    fun parseHexColor(raw: String): Int {
        var s = raw.trim()
        if (s.startsWith("#")) {
            s = s.substring(1)
        }
        if (s.length != 6 && s.length != 8) {
            throw NeoThemeException("Invalid color value: $raw")
        }
        s.forEach {
            if (Character.digit(it, 16) < 0) {
                throw NeoThemeException("Invalid color value: $raw")
            }
        }
        val value = s.toLong(16)
        return if (s.length == 6) {
            (0xFF000000L or value).toInt()
        } else {
            value.toInt()
        }
    }

    private fun toHex(color: Int): String = String.format("#%08X", color)
}
