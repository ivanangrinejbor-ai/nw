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
import org.catrobat.catroid.paintroid.tools.options.FillToolOptionsView
import org.catrobat.catroid.paintroid.tools.options.ToolOptionsViewController

class MagicWandTool(
    fillToolOptionsView: FillToolOptionsView,
    contextCallback: ContextCallback,
    toolOptionsViewController: ToolOptionsViewController,
    toolPaint: ToolPaint,
    workspace: Workspace,
    idlingResource: CountingIdlingResource,
    commandManager: CommandManager,
    override var drawTime: Long
) : BaseTool(
    contextCallback,
    toolOptionsViewController,
    toolPaint,
    workspace,
    idlingResource,
    commandManager
) {
    var colorTolerance = MAX_ABSOLUTE_TOLERANCE * DEFAULT_TOLERANCE_IN_PERCENT / 100f

    init {
        fillToolOptionsView.setCallback(object : FillToolOptionsView.Callback {
            override fun onColorToleranceChanged(colorTolerance: Int) {
                this@MagicWandTool.colorTolerance = MAX_ABSOLUTE_TOLERANCE * colorTolerance / 100f
            }
        })
    }

    override fun handleDown(coordinate: PointF?): Boolean = false
    override fun handleMove(coordinate: PointF?, shouldAnimate: Boolean): Boolean = false

    override fun handleUp(coordinate: PointF?): Boolean {
        coordinate ?: return false
        if (!workspace.contains(coordinate)) return false

        val clearPaint = Paint(toolPaint.paint).apply {
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        val command = commandFactory.createFillCommand(
            coordinate.x.toInt(), coordinate.y.toInt(), clearPaint, colorTolerance
        )
        commandManager.addCommand(command)
        return true
    }

    override fun toolPositionCoordinates(coordinate: PointF): PointF = coordinate
    override fun resetInternalState() = Unit
    override val toolType: ToolType = ToolType.MAGIC_WAND

    override fun handleUpAnimations(coordinate: PointF?) {
        super.handleUp(coordinate)
    }

    override fun handleDownAnimations(coordinate: PointF?) {
        super.handleDown(coordinate)
    }

    override fun draw(canvas: Canvas) = Unit
}
