package org.catrobat.catroid.collab

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable

class PresenceBorderDrawable(colors: List<Int>, density: Float) : Drawable() {
    private val ordered = colors.distinct().take(6)
    private val strokeWidth = 2.5f * density
    private val corner = 10f * density
    private val path = Path()
    private val paints = ArrayList<Paint>()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        paints.clear()
        if (ordered.isEmpty() || bounds.width() <= 0 || bounds.height() <= 0) return
        val inset = strokeWidth / 2f + 1f
        path.reset()
        path.addRoundRect(
            RectF(
                bounds.left + inset,
                bounds.top + inset,
                bounds.right - inset,
                bounds.bottom - inset
            ),
            corner, corner, Path.Direction.CW
        )
        val perim = PathMeasure(path, false).length
        if (perim <= 0f) return
        val segments = BorderSegments.compute(perim, ordered.size)
        for (i in ordered.indices) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            paint.color = ordered[i]
            paint.strokeWidth = strokeWidth
            if (ordered.size > 1) {
                val segment = segments[i]
                paint.pathEffect = DashPathEffect(
                    floatArrayOf(segment.length, perim - segment.length),
                    BorderSegments.phaseFor(perim, segment.start)
                )
            }
            paints.add(paint)
        }
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        for (paint in paints) {
            canvas.drawPath(path, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        for (paint in paints) paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        for (paint in paints) paint.colorFilter = colorFilter
    }

    @Deprecated("deprecated")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
