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
import android.os.Build
import android.preference.PreferenceManager
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import java.io.File

object ThemeManager {
    private const val TAG = "ThemeManager"

    const val PREF_KEY_SELECTED = "pref_neo_theme_selected"
    const val PREF_MATERIAL_YOU = "setting_material_you"
    const val DEFAULT_ID = "__default__"
    private const val THEMES_DIR = "neothemes"
    private const val EXTENSION = ".neotema"

    @Volatile
    var currentPalette: ThemePalette = ThemePalette.DEFAULT
        private set

    val overrideMap: Map<Int, Int>
        get() = if (currentPalette.isDefault) emptyMap() else currentPalette.toResourceOverrideMap()

    val isDefaultSelected: Boolean
        get() = currentPalette.isDefault

    data class ThemeEntry(
        val id: String,
        val palette: ThemePalette,
        val isDefault: Boolean
    )

    fun init(context: Context) {
        currentPalette = try {
            if (isMaterialYouEnabled(context)) {
                dynamicPalette(context) ?: loadSelectedPalette(context)
            } else {
                loadSelectedPalette(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load selected theme, falling back to default", e)
            ThemePalette.DEFAULT
        }
    }

    private fun loadSelectedPalette(context: Context): ThemePalette {
        val selected = prefs(context).getString(PREF_KEY_SELECTED, DEFAULT_ID) ?: DEFAULT_ID
        return if (selected == DEFAULT_ID) {
            ThemePalette.DEFAULT
        } else {
            val file = File(themesDir(context), selected)
            if (file.exists()) NeoThemeParser.parse(file.readText()) else ThemePalette.DEFAULT
        }
    }

    fun isMaterialYouEnabled(context: Context): Boolean =
        prefs(context).getBoolean(PREF_MATERIAL_YOU, false) && DynamicColors.isDynamicColorAvailable()

    fun setMaterialYou(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(PREF_MATERIAL_YOU, enabled).apply()
        currentPalette = if (enabled && DynamicColors.isDynamicColorAvailable()) {
            dynamicPalette(context) ?: loadSelectedPalette(context)
        } else {
            loadSelectedPalette(context)
        }
    }

    private fun dynamicPalette(context: Context): ThemePalette? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null
        }
        return try {
            ThemePalette(
                name = "Material You",
                author = null,
                toolbar = ContextCompat.getColor(context, android.R.color.system_neutral1_800),
                background = ContextCompat.getColor(context, android.R.color.system_neutral1_900),
                button = ContextCompat.getColor(context, android.R.color.system_accent2_700),
                accent = ContextCompat.getColor(context, android.R.color.system_accent1_200)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read system dynamic colours", e)
            null
        }
    }

    fun defaultTemplateText(): String = buildString {
        append("# NeoCatroid theme template (.neotema)\n")
        append("# Edit the colours below, then import this file in Settings > Themes.\n")
        append("# Colours are #RRGGBB or #AARRGGBB. Keys: toolbar, background, button, accent.\n")
        append("name=My theme\n")
        append("author=\n")
        append("toolbar=").append(templateHex(ThemePalette.DEFAULT_TOOLBAR)).append('\n')
        append("background=").append(templateHex(ThemePalette.DEFAULT_BACKGROUND)).append('\n')
        append("button=").append(templateHex(ThemePalette.DEFAULT_BUTTON)).append('\n')
        append("accent=").append(templateHex(ThemePalette.DEFAULT_ACCENT)).append('\n')
    }

    private fun templateHex(color: Int): String = String.format("#%08X", color)

    fun themesDir(context: Context): File {
        val dir = File(context.filesDir, THEMES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

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
