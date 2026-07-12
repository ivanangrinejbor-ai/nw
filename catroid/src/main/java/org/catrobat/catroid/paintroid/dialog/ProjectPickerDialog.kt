/*
 * NeoCatroid / NeoPaint — dialog to pick a project and import its fonts.
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
package org.catrobat.catroid.paintroid.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import org.catrobat.catroid.R
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.paintroid.tools.ImportedFontRegistry
import org.catrobat.catroid.utils.FileMetaDataExtractor
import java.io.File

class ProjectPickerDialog : DialogFragment() {
    private var onImported: (() -> Unit)? = null

    companion object {
        fun newInstance(onImported: () -> Unit): ProjectPickerDialog =
            ProjectPickerDialog().apply { this.onImported = onImported }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val scroll = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        layout.addView(
            TextView(context).apply {
                text = context.getString(R.string.import_fonts_from_project)
                textSize = 18f
            }
        )

        val projectNames = FileMetaDataExtractor.getProjectNames(FlavoredConstants.DEFAULT_ROOT_DIRECTORY)
        if (projectNames.isEmpty()) {
            layout.addView(
                TextView(context).apply { text = context.getString(R.string.no_projects_available) }
            )
        }
        val checkBoxes = projectNames.map { name ->
            CheckBox(context).apply { text = name }.also { layout.addView(it) }
        }

        val importButton = Button(context).apply { text = context.getString(R.string.import_selected) }
        importButton.setOnClickListener {
            var anyImported = false
            checkBoxes.filter { it.isChecked }.forEach { checkBox ->
                val projectDir = File(FlavoredConstants.DEFAULT_ROOT_DIRECTORY, checkBox.text.toString())
                if (ImportedFontRegistry.importFromProject(context, projectDir).isNotEmpty()) {
                    anyImported = true
                }
            }
            if (anyImported) onImported?.invoke()
            dismiss()
        }
        layout.addView(importButton)
        scroll.addView(layout)
        return scroll
    }
}
