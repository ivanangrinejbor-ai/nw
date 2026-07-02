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
import org.catrobat.catroid.content.FollowState
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class MoveToObjectAction : Action() {
    var scope: Scope? = null
    var targetObject: String = ""
    var avoidObjects: Formula? = null
    var speed: Formula? = null
    var moveMode: Int = 0
    var sizeCheckMode: Int = 0
    var blockedPathAction: Int = 0

    private var pathSet = false

    override fun act(delta: Float): Boolean {
        val pf = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return true
        val spriteName = scope?.sprite?.name ?: return true

        if (!pathSet) {
            pathSet = true
            val spd = speed?.interpretFloat(scope) ?: 100f
            val avoidStr = avoidObjects?.interpretString(scope) ?: ""
            for (name in avoidStr.split(",")) {
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    pf.addObstacle(trimmed)
                }
            }
            if (pf.navGrid == null) return true
            pf.setFollowerTarget(spriteName, targetObject)
            pf.setFollowerStopOnTouch(spriteName, moveMode == 1)
            pf.setFollowerSizeCheckMode(spriteName, sizeCheckMode)
            pf.setFollowerBlockedPathAction(spriteName, blockedPathAction)
            val result = pf.findPathToObject(spriteName, targetObject, sizeCheckMode, blockedPathAction)
            if (result.found || (blockedPathAction == 1 && result.points.isNotEmpty())) {
                if (moveMode == 1) {
                    pf.setPathForFollower(spriteName, result.points)
                } else {
                    pf.setPathForFollowerWithTarget(spriteName, result.points, targetObject)
                }
                pf.startFollowing(spriteName, spd)
            } else {
                return true
            }
        }

        if (pf.isEndReached(spriteName)) {
            pathSet = false
            return true
        }
        val follower = pf.getFollower(spriteName)
        if (follower == null || follower.state == FollowState.IDLE) {
            pathSet = false
            return true
        }
        return false
    }

    override fun restart() {
        pathSet = false
        super.restart()
    }
}
