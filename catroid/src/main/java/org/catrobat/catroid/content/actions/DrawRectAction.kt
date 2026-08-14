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

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class DrawRectAction : TemporalAction() {
    var scope: Scope? = null
    var x: Formula? = null
    var y: Formula? = null
    var width: Formula? = null
    var height: Formula? = null

    override fun update(delta: Float) {
        val s = scope ?: return
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val penActor = stageListener.penActor
        val shapeRenderer = stageListener.shapeRenderer
        val buffer = penActor.buffer
        val camera = penActor.canvasCamera

        val xVal = x?.interpretFloat(s) ?: 0f
        val yVal = y?.interpretFloat(s) ?: 0f
        val wVal = width?.interpretFloat(s) ?: 0f
        val hVal = height?.interpretFloat(s) ?: 0f

        val penColor = s.sprite.penConfiguration.getPenColor()
        camera.update()
        buffer.begin()
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.setColor(penColor.r, penColor.g, penColor.b, penColor.a)
        shapeRenderer.rect(xVal, yVal, wVal, hVal)
        shapeRenderer.end()
        buffer.end()
    }
}
