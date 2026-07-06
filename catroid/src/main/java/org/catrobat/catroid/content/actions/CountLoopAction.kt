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

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.Actor
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.formulaeditor.UserVariable

class CountLoopAction : LoopAction() {
    private var executedCount = 0
    private var isCurrentLoopInitialized = false
    private var isCountLoopInitialized = false
    private var countValue = 0
    override var currentTime = 0f
    var scope: Scope? = null
    var count: Formula? = null
    var loopVariable: UserVariable? = null

    public override fun delegate(delta: Float): Boolean {
        if (!isCountLoopInitialized) {
            isCountLoopInitialized = true
            interpretCount()
        }
        if (!isCurrentLoopInitialized) {
            currentTime = 0f
            isCurrentLoopInitialized = true
        }
        currentTime += delta
        if (countValue < 0) {
            countValue = 0
        }
        if (executedCount >= countValue) {
            return true
        }
        if (action != null && action.act(delta) && !isLoopDelayNeeded()) {
            executedCount++
            loopVariable?.value = executedCount
            if (executedCount >= countValue) {
                return true
            }
            isCurrentLoopInitialized = false
            action?.restart()
        }
        return false
    }

    override fun restart() {
        isCurrentLoopInitialized = false
        isCountLoopInitialized = false
        executedCount = 0
        super.restart()
    }

    private fun interpretCount() {
        countValue = try {
            count?.interpretInteger(scope) ?: 0
        } catch (interpretationException: InterpretationException) {
            Log.d(
                javaClass.simpleName,
                "Formula interpretation for this specific Brick failed.",
                interpretationException
            )
            0
        }
    }
}
