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

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.ShowBubbleActor
import org.catrobat.catroid.stage.StageActivity

class BlinkSpriteAction : TemporalAction() {

    var scope: Scope? = null
    var timesFormula: Formula? = null
    var intervalFormula: Formula? = null

    private var intervalSec: Float = 0.1f
    private var totalTimes: Int = 3
    private var originalVisible: Boolean = true
    private var initialized: Boolean = false

    override fun update(percent: Float) {
        val s = scope ?: return
        val sprite = s.sprite ?: return
        val look = sprite.look ?: return
        if (!initialized) {
            originalVisible = look.isLookVisible
            totalTimes = try { timesFormula?.interpretInteger(s) ?: 3 } catch (e: Exception) { 3 }
            if (totalTimes < 1) totalTimes = 1
            intervalSec = try { intervalFormula?.interpretFloat(s) ?: 0.1f } catch (e: Exception) { 0.1f }
            if (intervalSec <= 0.001f) intervalSec = 0.001f
            setDuration(totalTimes * intervalSec * 2f)
            initialized = true
        }
        val elapsed = time
        val cycle = (elapsed / intervalSec).toInt()
        val visible = (cycle % 2) == 0
        look.setLookVisible(visible)
        val listener = StageActivity.getActiveStageListener()
        if (listener != null) {
            val actor: ShowBubbleActor? = listener.getBubbleActorForSprite(sprite)
            actor?.isVisible = visible
        }
    }

    override fun end() {
        val sprite = scope?.sprite ?: return
        val look = sprite.look ?: return
        look.setLookVisible(originalVisible)
        val listener = StageActivity.getActiveStageListener()
        if (listener != null) {
            val actor: ShowBubbleActor? = listener.getBubbleActorForSprite(sprite)
            actor?.isVisible = originalVisible
        }
        initialized = false
    }

    override fun restart() {
        initialized = false
        super.restart()
    }
}
