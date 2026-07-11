package org.catrobat.catroid.paintroid.tools.drawable

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

private const val PIXEL_COUNT = 12

class PixelDrawable : ShapeDrawable {
    override fun draw(canvas: Canvas, shapeRect: RectF, drawPaint: Paint) {
        val isStroke = drawPaint.style == Paint.Style.STROKE
        val oldAntiAlias = drawPaint.isAntiAlias
        drawPaint.isAntiAlias = false

        val cellWidth = shapeRect.width() / PIXEL_COUNT
        val cellHeight = shapeRect.height() / PIXEL_COUNT
        val left = shapeRect.left
        val top = shapeRect.top

        val cellRect = RectF()
        for (row in 0 until PIXEL_COUNT) {
            for (col in 0 until PIXEL_COUNT) {
                cellRect.set(
                    left + col * cellWidth,
                    top + row * cellHeight,
                    left + (col + 1) * cellWidth,
                    top + (row + 1) * cellHeight
                )
                if (isStroke) {
                    canvas.drawRect(cellRect, drawPaint)
                } else {
                    canvas.drawRect(cellRect, drawPaint)
                }
            }
        }
        drawPaint.isAntiAlias = oldAntiAlias
    }
}
