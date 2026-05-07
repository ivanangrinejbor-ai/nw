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

import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform

class RunLuaAction() : TemporalAction() {
    var scope: Scope? = null
    var code: Formula? = null
    var userVariable: UserVariable? = null

    private var isRunning = false
    private var isFinished = false

    override fun act(delta: Float): Boolean {
        if (!isRunning) {
            isRunning = true
            val codeStr = code?.interpretString(scope) ?: "return 0"

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val globals: Globals = JsePlatform.standardGlobals()
                    val result: LuaValue = globals.load(codeStr).call()

                    Gdx.app.postRunnable {
                        userVariable?.value = result.tojstring()
                        isFinished = true
                    }
                } catch (e: Exception) {
                    Log.e("RunLuaAction", "FATAL LUA ERROR DURING GENERATION: ", e)
                    Gdx.app.postRunnable {
                        isFinished = true
                    }
                }
            }
        }

        return isFinished
    }

    override fun update(percent: Float) {
    }
}
