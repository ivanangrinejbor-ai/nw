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

class FillPolygonAction : TemporalAction() {
    var scope: Scope? = null
    var points: Formula? = null

    override fun update(delta: Float) {
        val s = scope ?: return
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val penActor = stageListener.penActor
        val shapeRenderer = stageListener.shapeRenderer
        val buffer = penActor.buffer
        val camera = penActor.canvasCamera

        val pointsStr = points?.interpretString(s) ?: return
        val tokens = pointsStr.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val verts = mutableListOf<Float>()
        for (token in tokens) {
            val coords = token.split(",")
            if (coords.size == 2) {
                verts.add(coords[0].toFloatOrNull() ?: 0f)
                verts.add(coords[1].toFloatOrNull() ?: 0f)
            }
        }
        if (verts.size < 6) return

        val triangulator = com.badlogic.gdx.math.EarClippingTriangulator()
        val floatArray = verts.toFloatArray()
        val indices = triangulator.computeTriangles(floatArray)

        val penColor = s.sprite.penConfiguration.getPenColor()
        camera.update()
        buffer.begin()
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(penColor.r, penColor.g, penColor.b, penColor.a)
        var i = 0
        while (i < indices.size) {
            val i1 = indices.get(i).toInt() * 2
            val i2 = indices.get(i + 1).toInt() * 2
            val i3 = indices.get(i + 2).toInt() * 2
            shapeRenderer.triangle(
                floatArray[i1], floatArray[i1 + 1],
                floatArray[i2], floatArray[i2 + 1],
                floatArray[i3], floatArray[i3 + 1]
            )
            i += 3
        }
        shapeRenderer.end()
        buffer.end()
    }
}
