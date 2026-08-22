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
package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class WaitFramesAction : Action() {

    var scope: Scope? = null
    var frames: Formula? = null

    private var remainingFrames: Int = -1

    override fun act(delta: Float): Boolean {
        if (remainingFrames < 0) {
            val s = scope
            remainingFrames = try { frames?.interpretInteger(s) ?: 1 } catch (e: Exception) { 1 }
            if (remainingFrames <= 0) return true
        }
        remainingFrames--
        return remainingFrames <= 0
    }

    override fun restart() {
        super.restart()
        remainingFrames = -1
    }
}
