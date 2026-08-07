package org.catrobat.catroid.paintroid.tools.implementation

import android.graphics.PointF
import androidx.test.espresso.idling.CountingIdlingResource
import org.catrobat.catroid.paintroid.command.CommandManager
import org.catrobat.catroid.paintroid.tools.ContextCallback
import org.catrobat.catroid.paintroid.tools.ToolPaint
import org.catrobat.catroid.paintroid.tools.ToolType
import org.catrobat.catroid.paintroid.tools.Workspace
import org.catrobat.catroid.paintroid.tools.options.BrushToolOptionsView
import org.catrobat.catroid.paintroid.tools.options.ToolOptionsViewController
import kotlin.math.floor

class PixelArtTool(
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
    private val pixelPoints = mutableListOf<PointF>()

    override val toolType: ToolType
        get() = ToolType.PIXELART

    override fun handleDown(coordinate: PointF?): Boolean {
        val c = coordinate ?: return super.handleDown(null)
        pixelPoints.clear()
        val snapped = snapToPixel(c)
        pixelPoints.add(snapped)
        return super.handleDown(snapped)
    }

    override fun handleMove(coordinate: PointF?, shouldAnimate: Boolean): Boolean {
        val c = coordinate ?: return super.handleMove(null, shouldAnimate)
        val snapped = snapToPixel(c)
        if (pixelPoints.isEmpty() || pixelPoints.last() != snapped) {
            pixelPoints.add(snapped)
            filterPixelPerfectCorners()
        }
        return super.handleMove(snapped, shouldAnimate)
    }

    override fun handleUp(coordinate: PointF?): Boolean {
        if (coordinate == null) {
            pixelPoints.clear()
            return super.handleUp(null)
        }
        val snapped = snapToPixel(coordinate)
        if (pixelPoints.isEmpty() || pixelPoints.last() != snapped) {
            pixelPoints.add(snapped)
            filterPixelPerfectCorners()
        }
        val result = super.handleUp(snapped)
        pixelPoints.clear()
        return result
    }

    private fun snapToPixel(point: PointF): PointF {
        return PointF(floor(point.x.toDouble()).toFloat(), floor(point.y.toDouble()).toFloat())
    }

    private fun filterPixelPerfectCorners() {
        if (pixelPoints.size < 3) return
        val p1 = pixelPoints[pixelPoints.size - 3]
        val p2 = pixelPoints[pixelPoints.size - 2]
        val p3 = pixelPoints[pixelPoints.size - 1]

        if ((p1.x == p2.x || p1.y == p2.y) && (p2.x == p3.x || p2.y == p3.y) && p1.x != p3.x && p1.y != p3.y) {
            pixelPoints.removeAt(pixelPoints.size - 2)
        }
    }
}
