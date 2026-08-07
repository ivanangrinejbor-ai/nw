package org.catrobat.catroid.paintroid.tools.implementation

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import androidx.test.espresso.idling.CountingIdlingResource
import org.catrobat.catroid.paintroid.command.CommandManager
import org.catrobat.catroid.paintroid.tools.ContextCallback
import org.catrobat.catroid.paintroid.tools.ToolPaint
import org.catrobat.catroid.paintroid.tools.ToolType
import org.catrobat.catroid.paintroid.tools.Workspace
import org.catrobat.catroid.paintroid.tools.options.BrushToolOptionsView
import org.catrobat.catroid.paintroid.tools.options.ToolOptionsViewController

class PatternTool(
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
    override val toolType: ToolType
        get() = ToolType.PATTERN

    init {
        val patternBitmap = createBrickPatternBitmap()
        val shader = BitmapShader(patternBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        toolPaint.paint.shader = shader
    }

    private fun createBrickPatternBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(180, 80, 50))
        val linePaint = Paint().apply {
            color = Color.rgb(230, 220, 200)
            strokeWidth = 1f
        }
        canvas.drawLine(0f, 7f, 16f, 7f, linePaint)
        canvas.drawLine(0f, 15f, 16f, 15f, linePaint)
        canvas.drawLine(8f, 0f, 8f, 7f, linePaint)
        canvas.drawLine(0f, 8f, 0f, 15f, linePaint)
        return bmp
    }
}
