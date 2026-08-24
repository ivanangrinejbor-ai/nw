/*
 * Paintroid: An image manipulation application for Android.
 *  Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.paintroid.tools.implementation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import androidx.test.espresso.idling.CountingIdlingResource
import org.catrobat.catroid.paintroid.MainActivity
import org.catrobat.catroid.paintroid.colorpicker.OnColorPickedListener
import org.catrobat.catroid.paintroid.command.CommandManager
import org.catrobat.catroid.paintroid.tools.ContextCallback
import org.catrobat.catroid.paintroid.tools.ToolPaint
import org.catrobat.catroid.paintroid.tools.ToolType
import org.catrobat.catroid.paintroid.tools.Workspace
import org.catrobat.catroid.paintroid.tools.options.ToolOptionsViewController

class PipetteTool(
    contextCallback: ContextCallback,
    toolOptionsViewController: ToolOptionsViewController,
    toolPaint: ToolPaint,
    workspace: Workspace,
    idlingResource: CountingIdlingResource,
    commandManager: CommandManager,
    private val listener: OnColorPickedListener,
    private val mainActivity: MainActivity
) : BaseTool(contextCallback, toolOptionsViewController, toolPaint, workspace, idlingResource, commandManager) {

    override val toolType: ToolType
        get() = ToolType.PIPETTE

    override var drawTime: Long = 0
    override fun handleUpAnimations(coordinate: PointF?) {
      super.handleUp(coordinate)
    }

    override fun handleDownAnimations(coordinate: PointF?) {
        super.handleDown(coordinate)
    }

    override fun draw(canvas: Canvas) = Unit

    private var cachedComposite: Bitmap? = null

    override fun handleDown(coordinate: PointF?): Boolean {
        cachedComposite?.recycle()
        cachedComposite = workspace.bitmapOfAllLayers
        return setColor(coordinate)
    }

    override fun handleMove(coordinate: PointF?, shouldAnimate: Boolean): Boolean = setColor(coordinate)

    override fun handleUp(coordinate: PointF?): Boolean {
        val result = setColor(coordinate, true)
        cachedComposite?.recycle()
        cachedComposite = null
        return result
    }

    override fun toolPositionCoordinates(coordinate: PointF): PointF = coordinate

    override fun resetInternalState() = Unit

    private fun setColor(coordinate: PointF?, saveCommand: Boolean = false): Boolean {
        if (coordinate == null || !workspace.contains(coordinate)) {
            return false
        }
        val bitmap = cachedComposite ?: workspace.bitmapOfAllLayers ?: return false
        if (bitmap.isRecycled) return false
        val x = coordinate.x.toInt().coerceIn(0, bitmap.width - 1)
        val y = coordinate.y.toInt().coerceIn(0, bitmap.height - 1)
        val color = bitmap.getPixel(x, y)
        listener.colorChanged(color)
        changePaintColor(color)
        if (saveCommand) {
            val command = commandFactory.createColorChangedCommand(this, mainActivity, color)
            mainActivity.model.colorHistory.addColor(color)
            mainActivity.commandManager.addCommand(command)
        }
        return true
    }
}
