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

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.FirestoreManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable

class QueryFirestoreAction() : Action() {
    var scope: Scope? = null
    var base: Formula? = null
    var collection: Formula? = null
    var field: Formula? = null
    var operator: String = "="
    var value: Formula? = null
    var limit: Formula? = null
    var variable: UserVariable? = null
    var waitForResponse: Boolean = true
    private var started = false
    @Volatile private var finished = false

    override fun act(delta: Float): Boolean {
        if (!started) {
            started = true
            val baseStr = base?.interpretString(scope) ?: ""
            val collectionStr = collection?.interpretString(scope) ?: ""
            val fieldStr = field?.interpretString(scope) ?: ""
            val valueStr = value?.interpretString(scope) ?: ""
            val limitInt = limit?.interpretInteger(scope) ?: 0
            if (waitForResponse) {
                FirestoreManager.queryDocuments(baseStr, collectionStr, fieldStr, operator, valueStr, limitInt) { result ->
                    variable?.value = result ?: "ERROR"
                    finished = true
                }
                return finished
            }
            FirestoreManager.queryDocuments(baseStr, collectionStr, fieldStr, operator, valueStr, limitInt) { result ->
                variable?.value = result ?: "ERROR"
            }
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
        collection = null
        field = null
        value = null
        limit = null
        variable = null
    }
}