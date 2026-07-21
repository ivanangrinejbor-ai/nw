package org.catrobat.catroid.ai.chat

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView

/**
 * A TextView that paints a moving highlight band across its text, creating a shimmer that
 * sweeps left -> right -> left continuously. Used for the "Thinking" indicator.
 */
class ShimmerTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var shimmerAnimator: ValueAnimator? = null
    private var translate = 0f
    private var gradient: LinearGradient? = null

    private val baseColor = Color.argb(150, 255, 255, 255)
    private val highlightColor = Color.WHITE
    private val bandWidth = 160f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildGradient()
    }

    private fun buildGradient() {
        gradient = LinearGradient(
            0f, 0f, bandWidth, 0f,
            intArrayOf(baseColor, highlightColor, baseColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (gradient != null) {
            val width = width.toFloat().coerceAtLeast(1f)
            val dx = -bandWidth + translate * (width + bandWidth)
            android.graphics.Matrix().apply {
                setTranslate(dx, 0f)
                gradient?.setLocalMatrix(this)
            }
        }
        super.onDraw(canvas)
    }

    fun startShimmer() {
        if (shimmerAnimator?.isRunning == true) return
        if (width > 0) buildGradient()
        shimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                translate = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopShimmer() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
        paint.shader = null
        setTextColor(highlightColor)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stopShimmer()
        super.onDetachedFromWindow()
    }
}
