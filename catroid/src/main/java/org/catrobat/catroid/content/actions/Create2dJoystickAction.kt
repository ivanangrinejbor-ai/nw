package org.catrobat.catroid.content.actions

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import java.io.File

class Create2dJoystickAction : Action() {
    var scope: Scope? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var backgroundFileName: Formula? = null
    var thumbFileName: Formula? = null
    var speed: Formula? = null

    private var started = false
    @Volatile private var finished = false

    override fun act(delta: Float): Boolean {
        if (!started) {
            started = true
            runAsyncCreate()
        }
        return finished
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun runAsyncCreate() {
        val stage = StageActivity.activeStageActivity?.get()
        val sprite = scope?.sprite
        if (stage == null || sprite == null) {
            finished = true
            return
        }

        val px = posX?.interpretInteger(scope) ?: 100
        val py = posY?.interpretInteger(scope) ?: 100
        val bgFileStr = backgroundFileName?.interpretString(scope) ?: ""
        val thumbFileStr = thumbFileName?.interpretString(scope) ?: ""
        val spd = speed?.interpretFloat(scope) ?: 5f

        stage.runOnUiThread {
            try {
                val density = stage.resources.displayMetrics.density
                val baseSize = Math.round(120 * density)
                val thumbSize = Math.round(50 * density)

                val container = FrameLayout(stage)
                val bgView = ImageView(stage)
                val thumbView = ImageView(stage)

                val projectDir = scope?.project?.directory

                // 1. Background image or default fallback
                val bgFile = if (!bgFileStr.isBlank() && projectDir != null) File(projectDir, bgFileStr) else null
                if (bgFile != null && bgFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(bgFile.absolutePath)
                    if (bmp != null) bgView.setImageBitmap(bmp) else setDefaultBg(bgView)
                } else {
                    setDefaultBg(bgView)
                }

                // 2. Thumb image or default fallback
                val thumbFile = if (!thumbFileStr.isBlank() && projectDir != null) File(projectDir, thumbFileStr) else null
                if (thumbFile != null && thumbFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                    if (bmp != null) thumbView.setImageBitmap(bmp) else setDefaultThumb(thumbView)
                } else {
                    setDefaultThumb(thumbView)
                }

                container.addView(bgView, FrameLayout.LayoutParams(baseSize, baseSize))

                val thumbLp = FrameLayout.LayoutParams(thumbSize, thumbSize).apply {
                    leftMargin = (baseSize - thumbSize) / 2
                    topMargin = (baseSize - thumbSize) / 2
                }
                container.addView(thumbView, thumbLp)

                val centerPos = baseSize / 2f
                val maxRadius = baseSize / 2f - thumbSize / 4f

                container.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            val dx = event.x - centerPos
                            val dy = event.y - centerPos
                            val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

                            val angle = Math.atan2(dy.toDouble(), dx.toDouble())
                            val clampedDist = Math.min(dist, maxRadius)

                            val clampX = (Math.cos(angle) * clampedDist).toFloat()
                            val clampY = (Math.sin(angle) * clampedDist).toFloat()

                            thumbView.translationX = clampX
                            thumbView.translationY = clampY

                            if (dist > 0) {
                                val normX = clampX / maxRadius
                                val normY = -clampY / maxRadius // Y flipped in 2D stage
                                sprite.x += normX * spd
                                sprite.y += normY * spd
                            }
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            thumbView.translationX = 0f
                            thumbView.translationY = 0f
                            true
                        }
                        else -> false
                    }
                }

                val params = FrameLayout.LayoutParams(baseSize, baseSize).apply {
                    leftMargin = px
                    topMargin = py
                }
                stage.addViewToStage("joystick_2d_" + sprite.name, container, params)
            } finally {
                finished = true
            }
        }
    }

    private fun setDefaultBg(view: ImageView) {
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#44FFFFFF"))
            setStroke(4, Color.parseColor("#88FFFFFF"))
        }
    }

    private fun setDefaultThumb(view: ImageView) {
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#CC38BDF8"))
        }
    }

    override fun restart() {
        super.restart()
        started = false
        finished = false
    }
}
