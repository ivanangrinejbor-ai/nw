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

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.OpenableColumns
import android.util.Log
import java.io.File

/**
 * Central store for the active [ThemePalette] and for the imported `.neotema` files.
 *
 * The selected theme id is persisted in the default SharedPreferences; imported themes are
 * copied into the app-private `neothemes/` directory. The active palette is exposed as an
 * [overrideMap] consumed by [ThemedResources].
 */
object ThemeManager {
    private const val TAG = "ThemeManager"

    const val PREF_KEY_SELECTED = "pref_neo_theme_selected"
    const val DEFAULT_ID = "__default__"
    private const val THEMES_DIR = "neothemes"
    private const val EXTENSION = ".neotema"

    @Volatile
    var currentPalette: ThemePalette = ThemePalette.DEFAULT
        private set

    /** Empty when the default theme is selected (full delegation to compiled colours). */
    val overrideMap: Map<Int, Int>
        get() = if (currentPalette.isDefault) emptyMap() else currentPalette.toResourceOverrideMap()

    val isDefaultSelected: Boolean
        get() = currentPalette.isDefault

    data class ThemeEntry(
        val id: String,
        val palette: ThemePalette,
        val isDefault: Boolean
    )

    /** Loads the persisted selection into memory. Call once from Application.onCreate. */
    fun init(context: Context) {
        currentPalette = try {
            val selected = prefs(context).getString(PREF_KEY_SELECTED, DEFAULT_ID) ?: DEFAULT_ID
            if (selected == DEFAULT_ID) {
                ThemePalette.DEFAULT
            } else {
                val file = File(themesDir(context), selected)
                if (file.exists()) NeoThemeParser.parse(file.readText()) else ThemePalette.DEFAULT
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load selected theme, falling back to default", e)
            ThemePalette.DEFAULT
        }
    }

    fun themesDir(context: Context): File {
        val dir = File(context.filesDir, THEMES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /** Reads, validates and copies a `.neotema` file into app storage. */
    fun importFromUri(context: Context, uri: Uri): ThemeEntry {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: throw NeoThemeException("Cannot open file")

        val palette = NeoThemeParser.parse(text)
        val baseName = sanitizeFileName(palette.name ?: queryDisplayName(context, uri) ?: "theme")
        val file = uniqueFile(context, baseName)
        file.writeText(NeoThemeParser.serialize(palette))
        return ThemeEntry(file.name, palette, isDefault = false)
    }

    /** Default entry first, then every parseable `.neotema` in storage (bad files skipped). */
    fun listThemes(context: Context): List<ThemeEntry> {
        val result = ArrayList<ThemeEntry>()
        result.add(ThemeEntry(DEFAULT_ID, ThemePalette.DEFAULT, isDefault = true))
        themesDir(context)
            .listFiles { f -> f.isFile && f.name.endsWith(EXTENSION) }
            ?.sortedBy { it.name.lowercase() }
            ?.forEach { f ->
                try {
                    result.add(ThemeEntry(f.name, NeoThemeParser.parse(f.readText()), isDefault = false))
                } catch (e: Exception) {
                    Log.e(TAG, "Skipping unreadable theme ${f.name}", e)
                }
            }
        return result
    }

    fun selectedId(context: Context): String =
        prefs(context).getString(PREF_KEY_SELECTED, DEFAULT_ID) ?: DEFAULT_ID

    fun applyTheme(context: Context, entry: ThemeEntry) {
        prefs(context).edit().putString(PREF_KEY_SELECTED, entry.id).apply()
        currentPalette = if (entry.isDefault) ThemePalette.DEFAULT else entry.palette
    }

    fun resetToDefault(context: Context) {
        prefs(context).edit().putString(PREF_KEY_SELECTED, DEFAULT_ID).apply()
        currentPalette = ThemePalette.DEFAULT
    }

    fun deleteTheme(context: Context, entry: ThemeEntry) {
        if (entry.isDefault) {
            return
        }
        val file = File(themesDir(context), entry.id)
        if (file.exists()) {
            file.delete()
        }
        if (selectedId(context) == entry.id) {
            resetToDefault(context)
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[^a-zA-Z0-9-_ ]"), "_").trim().replace(' ', '_')
        return if (cleaned.isEmpty()) "theme" else cleaned.take(40)
    }

    private fun uniqueFile(context: Context, baseName: String): File {
        val dir = themesDir(context)
        var candidate = File(dir, "$baseName$EXTENSION")
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "${baseName}_$i$EXTENSION")
            i++
        }
        return candidate
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx)?.substringBeforeLast('.') else null
        }
    } catch (e: Exception) {
        null
    }
}
