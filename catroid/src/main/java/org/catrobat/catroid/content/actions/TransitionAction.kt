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
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Look
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException

class TransitionAction : TemporalAction() {

    enum class Type { FADE_IN, FADE_OUT, ZOOM_IN, ZOOM_OUT, SLIDE_IN, SLIDE_OUT }
    enum class Edge { LEFT, RIGHT, TOP, BOTTOM }

    var scope: Scope? = null
    var speedFormula: Formula? = null
    var type: Type = Type.FADE_IN
    var edge: Edge = Edge.LEFT

    private var startScalar = 0f
    private var endScalar = 0f
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private var durationCalculated = 0f
    private var initialized = false

    override fun setActor(actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
        super.setActor(actor)
    }

    override fun restart() {
        super.restart()
        initialized = false
        durationCalculated = 0f
    }

    private fun initialize() {
        val look = scope?.sprite?.look ?: return
        val speed = try {
            speedFormula?.interpretFloat(scope) ?: 1f
        } catch (e: InterpretationException) {
            1f
        }
        durationCalculated = 1f / speed.coerceAtLeast(MIN_SPEED)
        duration = durationCalculated

        when (type) {
            Type.FADE_IN -> {
                startScalar = 100f
                endScalar = 0f
                look.setTransparencyInUserInterfaceDimensionUnit(startScalar)
            }
            Type.FADE_OUT -> {
                startScalar = look.transparencyInUserInterfaceDimensionUnit
                endScalar = 100f
            }
            Type.ZOOM_IN -> {
                startScalar = 0f
                endScalar = 100f
                look.setSizeInUserInterfaceDimensionUnit(startScalar)
            }
            Type.ZOOM_OUT -> {
                startScalar = look.sizeInUserInterfaceDimensionUnit
                endScalar = 0f
            }
            Type.SLIDE_IN -> {
                endX = look.xInUserInterfaceDimensionUnit
                endY = look.yInUserInterfaceDimensionUnit
                val off = offScreenPosition(look, endX, endY)
                startX = off.first
                startY = off.second
                look.setPositionInUserInterfaceDimensionUnit(startX, startY)
            }
            Type.SLIDE_OUT -> {
                startX = look.xInUserInterfaceDimensionUnit
                startY = look.yInUserInterfaceDimensionUnit
                val off = offScreenPosition(look, startX, startY)
                endX = off.first
                endY = off.second
            }
        }
        initialized = true
    }

    override fun act(delta: Float): Boolean {
        val look = scope?.sprite?.look ?: return true
        if (!initialized) {
            initialize()
        }
        if (durationCalculated <= 0f) return true

        time += delta
        val percent = (time / durationCalculated).coerceIn(0f, 1f)

        when (type) {
            Type.FADE_IN, Type.FADE_OUT ->
                look.setTransparencyInUserInterfaceDimensionUnit(interpolate(startScalar, endScalar, percent))
            Type.ZOOM_IN, Type.ZOOM_OUT ->
                look.setSizeInUserInterfaceDimensionUnit(interpolate(startScalar, endScalar, percent))
            Type.SLIDE_IN, Type.SLIDE_OUT ->
                look.setPositionInUserInterfaceDimensionUnit(
                    interpolate(startX, endX, percent),
                    interpolate(startY, endY, percent)
                )
        }

        return percent >= 1f
    }

    override fun update(percent: Float) {
    }

    override fun reset() {
        super.reset()
        initialized = false
        durationCalculated = 0f
    }

    private fun interpolate(from: Float, to: Float, percent: Float): Float = from + (to - from) * percent

    private fun offScreenPosition(look: Look, homeX: Float, homeY: Float): Pair<Float, Float> {
        val project = ProjectManager.getInstance().currentProject
        val screenWidth = (project?.xmlHeader?.virtualScreenWidth ?: 480).toFloat()
        val screenHeight = (project?.xmlHeader?.virtualScreenHeight ?: 800).toFloat()
        val halfWidth = screenWidth / 2f
        val halfHeight = screenHeight / 2f
        val objectHalfWidth = look.widthInUserInterfaceDimensionUnit / 2f
        val objectHalfHeight = look.heightInUserInterfaceDimensionUnit / 2f
        return when (edge) {
            Edge.LEFT -> Pair(-halfWidth - objectHalfWidth, homeY)
            Edge.RIGHT -> Pair(halfWidth + objectHalfWidth, homeY)
            Edge.TOP -> Pair(homeX, halfHeight + objectHalfHeight)
            Edge.BOTTOM -> Pair(homeX, -halfHeight - objectHalfHeight)
        }
    }

    companion object {
        private const val MIN_SPEED = 0.005f
    }
}
