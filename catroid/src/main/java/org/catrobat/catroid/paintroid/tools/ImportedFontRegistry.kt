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
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

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

    fun remove(context: Context, name: String) {
        val font = getAll(context).firstOrNull { it.name == name } ?: return
        File(fontDir(context), font.fileName).delete()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = getAll(context).filterNot { it.name == name }
            .map { "${it.name}::${it.fileName}" }
            .toSet()
        prefs.edit().putStringSet(KEY_FONTS, updated).apply()
    }

    fun copyToProject(context: Context, font: ImportedFont, projectFilesDir: File): Boolean {
        val src = File(fontDir(context), font.fileName)
        if (!src.exists() || font.name.isBlank()) {
            return false
        }
        if (!projectFilesDir.exists() && !projectFilesDir.mkdirs()) {
            return false
        }
        val extension = font.fileName.substringAfterLast('.', "ttf")
        val existing = projectFilesDir.listFiles()?.map { it.name } ?: emptyList()
        val destName = uniqueName(existing, "${font.name}.$extension")
        return try {
            src.copyTo(File(projectFilesDir, destName), overwrite = false).exists()
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    fun importFromFile(context: Context, uri: Uri): ImportedFont? {
        val resolver = context.contentResolver
        var displayName = queryDisplayName(resolver, uri) ?: return null
        if (!displayName.endsWith(".ttf", ignoreCase = true) &&
            !displayName.endsWith(".otf", ignoreCase = true)
        ) {
            return null
        }
        val baseName = displayName.substringBeforeLast('.')
        val dir = fontDir(context)
        val existing = getAll(context).toMutableList()
        val destName = uniqueName(existing.map { it.fileName }, displayName)
        val dest = File(dir, destName)
        return try {
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            val entry = ImportedFont(baseName, destName)
            existing.add(entry)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putStringSet(KEY_FONTS, existing.map { "${it.name}::${it.fileName}" }.toSet())
                .apply()
            entry
        } catch (e: Exception) {
            dest.delete()
            null
        }
    }

    private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

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
