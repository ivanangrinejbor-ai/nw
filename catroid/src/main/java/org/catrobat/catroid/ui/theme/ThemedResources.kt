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
import android.content.res.Resources

class ThemedResources(base: Resources) :
    Resources(base.assets, base.displayMetrics, base.configuration) {

    private val overrides: Map<Int, Int>
        get() = ThemeManager.overrideMap

    @Deprecated("Deprecated in Java")
    override fun getColor(id: Int): Int =
        overrides[id] ?: @Suppress("DEPRECATION") super.getColor(id)

    override fun getColor(id: Int, theme: Theme?): Int =
        overrides[id] ?: super.getColor(id, theme)

    @Deprecated("Deprecated in Java")
    override fun getColorStateList(id: Int): ColorStateList {
        val override = overrides[id]
        return if (override != null) {
            ColorStateList.valueOf(override)
        } else {
            @Suppress("DEPRECATION") super.getColorStateList(id)
        }
    }

    override fun getColorStateList(id: Int, theme: Theme?): ColorStateList {
        val override = overrides[id]
        return if (override != null) ColorStateList.valueOf(override) else super.getColorStateList(id, theme)
    }
}
