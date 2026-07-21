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
package org.catrobat.catroid.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.catrobat.catroid.R
import org.catrobat.catroid.ui.theme.NeoThemeException
import org.catrobat.catroid.ui.theme.ThemeManager
import org.catrobat.catroid.ui.theme.ThemeManager.ThemeEntry
import org.catrobat.catroid.ui.theme.ThemesAdapter

class ThemesActivity : BaseActivity() {

    private lateinit var adapter: ThemesAdapter
    private lateinit var overlay: View
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_themes)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings_themes_title)

        overlay = findViewById(R.id.theme_apply_overlay)

        adapter = ThemesAdapter(
            entries = ThemeManager.listThemes(this),
            selectedId = ThemeManager.selectedId(this),
            onClick = { onCubeClicked(it) },
            onLongClick = { onCubeLongClicked(it) }
        )

        val recycler = findViewById<RecyclerView>(R.id.theme_recycler)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = adapter

        findViewById<Button>(R.id.theme_import_button).setOnClickListener { launchImportPicker() }
    }

    private fun refresh() {
        adapter.update(ThemeManager.listThemes(this), ThemeManager.selectedId(this))
    }

    private fun onCubeClicked(entry: ThemeEntry) {
        if (entry.id == ThemeManager.selectedId(this)) {
            return
        }
        overlay.visibility = View.VISIBLE
        ThemeManager.applyTheme(this, entry)
        handler.postDelayed({ recreate() }, THEME_APPLY_DELAY_MS)
    }

    private fun onCubeLongClicked(entry: ThemeEntry) {
        if (entry.isDefault) {
            return
        }
        AlertDialog.Builder(this)
            .setTitle(entry.palette.name ?: entry.id)
            .setMessage(R.string.theme_delete_confirm)
            .setPositiveButton(R.string.theme_delete) { _, _ ->
                ThemeManager.deleteTheme(this, entry)
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun launchImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_IMPORT_THEME)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMPORT_THEME && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                ThemeManager.importFromUri(this, uri)
                refresh()
                Toast.makeText(this, R.string.theme_import_success, Toast.LENGTH_SHORT).show()
            } catch (e: NeoThemeException) {
                showImportError(e.message)
            } catch (e: Exception) {
                showImportError(e.message)
            }
        }
    }

    private fun showImportError(detail: String?) {
        Toast.makeText(
            this,
            getString(R.string.theme_import_error, detail ?: ""),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_IMPORT_THEME = 4711
        private const val THEME_APPLY_DELAY_MS = 450L
    }
}
