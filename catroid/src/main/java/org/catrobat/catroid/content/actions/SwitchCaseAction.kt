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
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException

class SwitchCaseAction : Action() {
    var scope: Scope? = null
    var expressionFormula: Formula? = null
    var caseActions: MutableList<Action> = mutableListOf()
    var defaultAction: Action? = null
    private var matchedAction: Action? = null
    private var isInitialized = false
    private var matched = false
    private var expressionValue: Double = 0.0
    private var expressionString: String = ""

    private fun evaluateSwitch() {
        if (scope == null) {
            matched = true
            matchedAction = defaultAction
            return
        }
        expressionValue = try {
            expressionFormula?.interpretDouble(scope) ?: 0.0
        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Switch expression interpretation failed", e)
            0.0
        }
        expressionString = try {
            expressionFormula?.interpretString(scope) ?: expressionValue.toString()
        } catch (e: InterpretationException) {
            expressionValue.toString()
        }

        for (caseAction in caseActions) {
            if (caseAction is CaseAction) {
                if (caseAction.matches(expressionValue, expressionString)) {
                    matchedAction = caseAction
                    matched = true
                    return
                }
            }
        }
        matchedAction = defaultAction
        matched = true
    }

    fun addCaseAction(caseAction: CaseAction) {
        caseActions.add(caseAction)
    }

    override fun act(delta: Float): Boolean {
        if (!isInitialized) {
            evaluateSwitch()
            isInitialized = true
        }
        if (!matched) return true
        return matchedAction?.act(delta) ?: true
    }

    override fun restart() {
        matchedAction?.restart()
        isInitialized = false
        matched = false
        matchedAction = null
        super.restart()
    }

    override fun setActor(actor: Actor) {
        super.setActor(actor)
        for (ca in caseActions) {
            ca.actor = actor
        }
        defaultAction?.actor = actor
    }

    class CaseAction : Action() {
        var scope: Scope? = null
        var caseFormula: Formula? = null
        var bodyAction: Action? = null
        private var caseValue: Double = 0.0
        private var caseString: String = ""
        private var evaluated = false

        fun matches(switchValue: Double, switchString: String): Boolean {
            if (scope == null) return false
            if (!evaluated) {
                caseValue = try {
                    caseFormula?.interpretDouble(scope) ?: 0.0
                } catch (e: InterpretationException) {
                    Log.d(javaClass.simpleName, "Case value interpretation failed", e)
                    0.0
                }
                caseString = try {
                    caseFormula?.interpretString(scope) ?: caseValue.toString()
                } catch (e: InterpretationException) {
                    caseValue.toString()
                }
                evaluated = true
            }
            if (caseString.toDoubleOrNull() != null && switchString.toDoubleOrNull() != null) {
                return caseValue == switchValue
            }
            return caseString.trim() == switchString.trim()
        }

        override fun act(delta: Float): Boolean {
            return bodyAction?.act(delta) ?: true
        }

        override fun restart() {
            bodyAction?.restart()
            evaluated = false
            super.restart()
        }

        override fun setActor(actor: Actor) {
            super.setActor(actor)
            bodyAction?.actor = actor
        }
    }
}
