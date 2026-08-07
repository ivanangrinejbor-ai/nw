package org.catrobat.catroid.paintroid.tools.implementation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import androidx.test.espresso.idling.CountingIdlingResource
import org.catrobat.catroid.paintroid.command.CommandManager
import org.catrobat.catroid.paintroid.tools.ContextCallback
import org.catrobat.catroid.paintroid.tools.ToolPaint
import org.catrobat.catroid.paintroid.tools.ToolType
import org.catrobat.catroid.paintroid.tools.Workspace
import org.catrobat.catroid.paintroid.tools.options.BrushToolOptionsView
import org.catrobat.catroid.paintroid.tools.options.ToolOptionsViewController

class LassoTool(
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
    private val lassoPath = Path()
    private val lassoPoints = mutableListOf<PointF>()
    private var isClosed = false

    private val contourPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    override val toolType: ToolType
        get() = ToolType.LASSO

    override fun handleDown(coordinate: PointF?): Boolean {
        val c = coordinate ?: return super.handleDown(null)
        lassoPath.reset()
        lassoPoints.clear()
        isClosed = false
        lassoPath.moveTo(c.x, c.y)
        lassoPoints.add(c)
        return super.handleDown(c)
    }

    override fun handleMove(coordinate: PointF?, shouldAnimate: Boolean): Boolean {
        val c = coordinate ?: return super.handleMove(null, shouldAnimate)
        if (!isClosed) {
            lassoPath.lineTo(c.x, c.y)
            lassoPoints.add(c)
        }
        return super.handleMove(c, shouldAnimate)
    }

    override fun handleUp(coordinate: PointF?): Boolean {
        if (lassoPoints.isNotEmpty()) {
            lassoPath.close()
            isClosed = true
        }
        return super.handleUp(coordinate)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (lassoPoints.isNotEmpty()) {
            canvas.drawPath(lassoPath, contourPaint)
        }
    }
}
