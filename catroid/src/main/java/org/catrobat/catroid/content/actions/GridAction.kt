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

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.PathfindingManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class GridAction : TemporalAction() {
    var scope: Scope? = null
    var gridX: Formula? = null
    var gridY: Formula? = null
    var gridW: Formula? = null
    var gridH: Formula? = null

    override fun update(percent: Float) {
        val x = gridX?.interpretFloat(scope) ?: 0f
        val y = gridY?.interpretFloat(scope) ?: 0f
        val w = (gridW?.interpretFloat(scope) ?: 32f).toInt().coerceIn(1, 2000)
        val h = (gridH?.interpretFloat(scope) ?: 32f).toInt().coerceIn(1, 2000)
        StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager?.createGrid(w, h, 20f, true, x, y)
    }
}
