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

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.catrobat.catroid.R
import org.catrobat.catroid.ui.theme.ThemeManager.ThemeEntry

class ThemesAdapter(
    private var entries: List<ThemeEntry>,
    private var selectedId: String,
    private val onClick: (ThemeEntry) -> Unit,
    private val onLongClick: (ThemeEntry) -> Unit
) : RecyclerView.Adapter<ThemesAdapter.CubeViewHolder>() {

    fun update(newEntries: List<ThemeEntry>, newSelectedId: String) {
        entries = newEntries
        selectedId = newSelectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CubeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_theme_cube, parent, false)
        return CubeViewHolder(view)
    }

    override fun onBindViewHolder(holder: CubeViewHolder, position: Int) {
        holder.bind(entries[position], entries[position].id == selectedId)
    }

    override fun getItemCount(): Int = entries.size

    inner class CubeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val toolbar: View = itemView.findViewById(R.id.theme_preview_toolbar)
        private val background: View = itemView.findViewById(R.id.theme_preview_background)
        private val button: View = itemView.findViewById(R.id.theme_preview_button)
        private val accent: View = itemView.findViewById(R.id.theme_preview_accent)
        private val name: TextView = itemView.findViewById(R.id.theme_name)
        private val author: TextView = itemView.findViewById(R.id.theme_author)
        private val appliedBorder: View = itemView.findViewById(R.id.theme_applied_border)
        private val appliedBadge: View = itemView.findViewById(R.id.theme_applied_badge)
        private val card: MaterialCardView = itemView.findViewById(R.id.theme_cube_card)

        fun bind(entry: ThemeEntry, isSelected: Boolean) {
            val palette = entry.palette
            toolbar.setBackgroundColor(palette.toolbar)
            background.setBackgroundColor(palette.background)
            button.backgroundTintList = ColorStateList.valueOf(palette.button)
            accent.backgroundTintList = ColorStateList.valueOf(palette.accent)

            val context = itemView.context
            if (entry.isDefault) {
                name.text = context.getString(R.string.theme_default_name)
                author.visibility = View.GONE
            } else {
                name.text = palette.name ?: entry.id.substringBeforeLast('.')
                if (palette.author.isNullOrEmpty()) {
                    author.visibility = View.GONE
                } else {
                    author.visibility = View.VISIBLE
                    author.text = context.getString(R.string.theme_by_author, palette.author)
                }
            }

            appliedBorder.visibility = if (isSelected) View.VISIBLE else View.GONE
            appliedBadge.visibility = if (isSelected) View.VISIBLE else View.GONE

            card.setOnClickListener { onClick(entry) }
            card.setOnLongClickListener {
                onLongClick(entry)
                true
            }
        }
    }
}
