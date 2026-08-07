package org.catrobat.catroid.paintroid.tools.implementation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import androidx.test.espresso.idling.CountingIdlingResource
import org.catrobat.catroid.paintroid.command.CommandManager
import org.catrobat.catroid.paintroid.tools.ContextCallback
import org.catrobat.catroid.paintroid.tools.ToolPaint
import org.catrobat.catroid.paintroid.tools.ToolType
import org.catrobat.catroid.paintroid.tools.Workspace
import org.catrobat.catroid.paintroid.tools.options.BrushToolOptionsView
import org.catrobat.catroid.paintroid.tools.options.ToolOptionsViewController

class SymmetryTool(
    brushToolOptionsView: BrushToolOptionsView,
    contextCallback: ContextCallback,
    toolOptionsViewController: ToolOptionsViewController,
    toolPaint: ToolPaint,
    workspace: Workspace,
    idlingResource: CountingIdlingResource,
    commandManager: CommandManager,
    drawTime: Long
) : BrushTool(
    brushToolOptionsView,
    contextCallback,
    toolOptionsViewController,
    toolPaint,
    workspace,
    idlingResource,
    commandManager,
    drawTime
) {
    var mirrorHorizontal = true
    var mirrorVertical = true

    private val axisPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
    }

    override val toolType: ToolType
        get() = ToolType.SYMMETRY

    override fun handleDown(coordinate: PointF?): Boolean {
        val c = coordinate ?: return super.handleDown(null)
        val result = super.handleDown(c)
        val mirrors = getMirrorPoints(c)
        for (m in mirrors) {
            super.handleDown(m)
        }
        return result
    }

    override fun handleMove(coordinate: PointF?, shouldAnimate: Boolean): Boolean {
        val c = coordinate ?: return super.handleMove(null, shouldAnimate)
        val result = super.handleMove(c, shouldAnimate)
        val mirrors = getMirrorPoints(c)
        for (m in mirrors) {
            super.handleMove(m, shouldAnimate)
        }
        return result
    }

    override fun handleUp(coordinate: PointF?): Boolean {
        val c = coordinate ?: return super.handleUp(null)
        val result = super.handleUp(c)
        val mirrors = getMirrorPoints(c)
        for (m in mirrors) {
            super.handleUp(m)
        }
        return result
    }

    private fun getMirrorPoints(p: PointF): List<PointF> {
        val centerX = workspace.width / 2f
        val centerY = workspace.height / 2f
        val list = mutableListOf<PointF>()
        if (mirrorHorizontal) {
            list.add(PointF(2 * centerX - p.x, p.y))
        }
        if (mirrorVertical) {
            list.add(PointF(p.x, 2 * centerY - p.y))
        }
        if (mirrorHorizontal && mirrorVertical) {
            list.add(PointF(2 * centerX - p.x, 2 * centerY - p.y))
        }
        return list
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val centerX = workspace.width / 2f
        val centerY = workspace.height / 2f
        if (mirrorHorizontal) {
            canvas.drawLine(centerX, 0f, centerX, workspace.height.toFloat(), axisPaint)
        }
        if (mirrorVertical) {
            canvas.drawLine(0f, centerY, workspace.width.toFloat(), centerY, axisPaint)
        }
    }
}
