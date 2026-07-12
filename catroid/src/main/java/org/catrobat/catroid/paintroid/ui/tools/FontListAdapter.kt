/*
 * NeoCatroid / NeoPaint — font list adapter for the text tool.
 *  Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
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
package org.catrobat.catroid.paintroid.ui.tools

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import org.catrobat.catroid.R
import org.catrobat.catroid.paintroid.tools.FontEntry
import org.catrobat.catroid.paintroid.tools.FontType
import org.catrobat.catroid.paintroid.tools.ImportedFontRegistry

class FontListAdapter(
    private val context: Context,
    var fontEntries: List<FontEntry>,
    private val onFontClicked: (FontEntry) -> Unit
) : RecyclerView.Adapter<FontListAdapter.ViewHolder>() {

    var selectedIndex = 0
        private set

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_font, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = fontEntries[position]
        val typeface = when (entry) {
            is FontEntry.Imported ->
                ImportedFontRegistry.getTypeface(context, entry.font.name) ?: Typeface.SANS_SERIF
            is FontEntry.BuiltIn -> when (entry.fontType) {
                FontType.SANS_SERIF -> Typeface.SANS_SERIF
                FontType.SERIF -> Typeface.SERIF
                FontType.MONOSPACE -> Typeface.MONOSPACE
                FontType.STC -> ResourcesCompat.getFont(context, R.font.stc_regular)
                FontType.DUBAI -> ResourcesCompat.getFont(context, R.font.dubai)
                FontType.PROJECT_FONT -> Typeface.SANS_SERIF
            }
        }
        holder.textView.typeface = typeface
        holder.textView.text = entry.displayName(context)
        holder.textView.isSelected = position == selectedIndex
        holder.itemView.setOnClickListener {
            val previous = selectedIndex
            selectedIndex = position
            notifyItemChanged(previous)
            notifyItemChanged(selectedIndex)
            onFontClicked(entry)
        }
    }

    override fun getItemCount(): Int = fontEntries.size

    fun setSelectedFontIndex(index: Int) {
        selectedIndex = index
        notifyItemChanged(index)
    }

    fun updateEntries(entries: List<FontEntry>) {
        fontEntries = entries
        notifyDataSetChanged()
    }
}
