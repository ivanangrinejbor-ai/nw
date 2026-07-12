/*
 * NeoCatroid / NeoPaint — font model for the text tool.
 *  Copyright (C) 2026 The Catrobat Team
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.paintroid.tools

import android.content.Context
import android.graphics.Typeface
import java.io.File

/**
 * A selectable font in the text tool's font list.
 * BuiltIn entries map to the static [FontType] enum; Imported entries are
 * fonts the user copied from another project and stored locally on the device.
 */
sealed class FontEntry {
    abstract fun displayName(context: Context): String

    data class BuiltIn(val fontType: FontType) : FontEntry() {
        override fun displayName(context: Context): String = context.getString(fontType.nameResource)
    }

    data class Imported(val font: ImportedFont) : FontEntry() {
        override fun displayName(context: Context): String = font.name
    }
}

data class ImportedFont(val name: String, val fileName: String)

/**
 * Persists fonts the user imported from other projects so they survive app
 * restarts. Font files are copied into the app-private "fonts" directory and
 * the name->file mapping is kept in SharedPreferences.
 */
object ImportedFontRegistry {
    private const val PREFS_NAME = "imported_fonts"
    private const val KEY_FONTS = "fonts"

    private fun fontDir(context: Context): File =
        context.getDir("fonts", Context.MODE_PRIVATE).also { if (!it.exists()) it.mkdirs() }

    fun getAll(context: Context): List<ImportedFont> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_FONTS, emptySet()) ?: emptySet()
        return raw.mapNotNull { entry ->
            val parts = entry.split("::")
            if (parts.size == 2) ImportedFont(parts[0], parts[1]) else null
        }.sortedBy { it.name.lowercase() }
    }

    fun getTypeface(context: Context, name: String): Typeface? {
        val font = getAll(context).firstOrNull { it.name == name } ?: return null
        val file = File(fontDir(context), font.fileName)
        if (!file.exists()) return null
        return createTypefaceWithFallback(file)
    }

    /**
     * Build a Typeface from a font file that falls back to the default sans-serif
     * for any glyph the file does not actually provide (e.g. a Cyrillic-only font
     * used for Latin text). Without this, missing glyphs render blank or as stray
     * system letters instead of a proper fallback.
     */
    private fun createTypefaceWithFallback(file: File): Typeface {
        val fallback = Typeface.SANS_SERIF
        return try {
            Typeface.Builder(file).setFallback("sans-serif").build() ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }

    fun importFromProject(context: Context, projectDir: File): List<ImportedFont> {
        val filesDir = File(projectDir, "files")
        if (!filesDir.isDirectory) return emptyList()
        val dir = fontDir(context)
        val existing = getAll(context).toMutableList()
        val imported = mutableListOf<ImportedFont>()
        filesDir.listFiles { _, name ->
            name.endsWith(".ttf", ignoreCase = true) || name.endsWith(".otf", ignoreCase = true)
        }?.forEach { src ->
            val baseName = src.nameWithoutExtension
            val destName = uniqueName(existing.map { it.fileName } + imported.map { it.fileName }, src.name)
            val dest = File(dir, destName)
            src.copyTo(dest, overwrite = true)
            val entry = ImportedFont(baseName, destName)
            imported.add(entry)
            existing.add(entry)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(KEY_FONTS, existing.map { "${it.name}::${it.fileName}" }.toSet())
            .apply()
        return imported
    }

    private fun uniqueName(existing: List<String>, desired: String): String {
        if (existing.none { it.equals(desired, ignoreCase = true) }) return desired
        var i = 1
        var candidate: String
        do {
            candidate = "$desired($i)"
            i++
        } while (existing.any { it.equals(candidate, ignoreCase = true) })
        return candidate
    }
}
