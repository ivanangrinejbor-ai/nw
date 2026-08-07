package org.catrobat.catroid.paintroid.tools.implementation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.test.espresso.idling.CountingIdlingResource
import org.catrobat.catroid.paintroid.command.CommandManager
import org.catrobat.catroid.paintroid.tools.ContextCallback
import org.catrobat.catroid.paintroid.tools.ToolPaint
import org.catrobat.catroid.paintroid.tools.ToolType
import org.catrobat.catroid.paintroid.tools.Workspace
import org.catrobat.catroid.paintroid.tools.options.ToolOptionsViewController

class AutoRemoveBgTool(
    contextCallback: ContextCallback,
    toolOptionsViewController: ToolOptionsViewController,
    toolPaint: ToolPaint,
    workspace: Workspace,
    idlingResource: CountingIdlingResource,
    commandManager: CommandManager
) : BaseTool(
    contextCallback,
    toolOptionsViewController,
    toolPaint,
    workspace,
    idlingResource,
    commandManager
) {
    override var drawTime: Long = 0

    override fun handleDown(coordinate: PointF?): Boolean = false
    override fun handleMove(coordinate: PointF?, shouldAnimate: Boolean): Boolean = false

    override fun handleUp(coordinate: PointF?): Boolean {
        removeBackgroundAtCorners()
        return true
    }

    private fun removeBackgroundAtCorners() {
        val width = workspace.width
        val height = workspace.height
        if (width <= 0 || height <= 0) return

        val clearPaint = Paint(toolPaint.paint).apply {
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        val corners = listOf(
            PointF(0f, 0f),
            PointF(width - 1f, 0f),
            PointF(0f, height - 1f),
            PointF(width - 1f, height - 1f)
        )

        val tolerance = MAX_ABSOLUTE_TOLERANCE * 15 / 100f
        for (corner in corners) {
            val command = commandFactory.createFillCommand(
                corner.x.toInt(), corner.y.toInt(), clearPaint, tolerance
            )
            commandManager.addCommand(command)
        }
    }

    override fun toolPositionCoordinates(coordinate: PointF): PointF = coordinate
    override fun resetInternalState() = Unit
    override val toolType: ToolType = ToolType.AUTO_REMOVE_BG

    override fun handleUpAnimations(coordinate: PointF?) {
        super.handleUp(coordinate)
    }

    override fun handleDownAnimations(coordinate: PointF?) {
        super.handleDown(coordinate)
    }

    override fun draw(canvas: Canvas) = Unit
}
