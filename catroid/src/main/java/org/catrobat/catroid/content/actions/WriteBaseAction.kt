/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

import android.widget.Toast
import android.content.Context
import com.badlogic.gdx.scenes.scene2d.Action
import android.app.Activity
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.FireBaseManager

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.util.ArrayList

class WriteBaseAction() : Action() {
    private var contextt: Context? = null
    var scope: Scope? = null
    var base: Formula? = null
    var key: Formula? = null
    var value: Formula? = null
    var waitForResponse: Boolean = true
    private var started = false
    @Volatile private var finished = false

    override fun act(delta: Float): Boolean {
        if (!started) {
            started = true
            val baseStr = base?.interpretString(scope) ?: ""
            val keyStr = key?.interpretString(scope) ?: ""
            val valueStr = value?.interpretString(scope) ?: ""
            if (waitForResponse) {
                FireBaseManager.writeToDatabase(baseStr, keyStr, valueStr) { finished = true }
                return finished
            }
            FireBaseManager.writeToDatabase(baseStr, keyStr, valueStr)
            return true
        }
        return finished
    }

    override fun restart() {
        super.restart()
        started = false
        finished = false
    }

    override fun reset() {
        super.reset()
        started = false
        finished = false
        scope = null
        base = null
        key = null
        value = null
    }
}
