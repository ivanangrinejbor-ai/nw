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

import android.os.Handler
import android.os.Looper
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable

class TimerAction : TemporalAction() {
    companion object {
        private val activeTimers = mutableMapOf<String, TimerData>()

        fun stopTimer(name: String) {
            activeTimers[name]?.let { timer ->
                timer.handler.removeCallbacksAndMessages(null)
                timer.variable?.value = 0.0
                activeTimers.remove(name)
            }
        }

        fun stopAllTimers() {
            activeTimers.values.forEach { timer ->
                timer.handler.removeCallbacksAndMessages(null)
            }
            activeTimers.clear()
        }
    }

    var scope: Scope? = null
    var timerName: Formula? = null
    var duration: Formula? = null
    var variable: Formula? = null

    private data class TimerData(
        val handler: Handler,
        val variable: UserVariable?,
        val startTime: Long,
        val durationMs: Long
    )

    override fun update(percent: Float) {
        val name = timerName?.interpretString(scope) ?: return
        val durationSec = duration?.interpretFloat(scope) ?: return
        val variableName = variable?.interpretString(scope) ?: return

        val project = ProjectManager.getInstance().currentProject ?: return
        val userVariable = project.getUserVariable(variableName) ?: return

        // Cancel existing timer with same name
        activeTimers[name]?.handler?.removeCallbacksAndMessages(null)
        activeTimers.remove(name)

        val durationMs = (durationSec * 1000).toLong()
        val handler = Handler(Looper.getMainLooper())
        val startTime = System.currentTimeMillis()

        val timerData = TimerData(handler, userVariable, startTime, durationMs)
        activeTimers[name] = timerData

        // Update variable with remaining time
        val updateRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = (durationMs - elapsed) / 1000.0

                if (remaining > 0) {
                    userVariable.value = remaining
                    handler.postDelayed(this, 100) // Update every 100ms
                } else {
                    userVariable.value = 0.0
                    activeTimers.remove(name)
                }
            }
        }

        handler.post(updateRunnable)
    }
}
