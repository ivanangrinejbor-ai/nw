package org.catrobat.catroid.content.actions

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import java.io.File

class Create2dJumpButtonAction : Action() {
    var scope: Scope? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var activeFileName: Formula? = null
    var inactiveFileName: Formula? = null
    var jumpPower: Formula? = null

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

        val px = posX?.interpretInteger(scope) ?: 300
        val py = posY?.interpretInteger(scope) ?: 100
        val activeFileStr = activeFileName?.interpretString(scope) ?: ""
        val inactiveFileStr = inactiveFileName?.interpretString(scope) ?: ""
        val pwr = jumpPower?.interpretFloat(scope) ?: 25f

        stage.runOnUiThread {
            try {
                val density = stage.resources.displayMetrics.density
                val btnSize = Math.round(70 * density)

                val buttonView = ImageView(stage)
                val projectDir = scope?.project?.directory

                val activeFile = if (!activeFileStr.isBlank() && projectDir != null) File(projectDir, activeFileStr) else null
                val inactiveFile = if (!inactiveFileStr.isBlank() && projectDir != null) File(projectDir, inactiveFileStr) else null

                val activeBmp = if (activeFile != null && activeFile.exists()) BitmapFactory.decodeFile(activeFile.absolutePath) else null
                val inactiveBmp = if (inactiveFile != null && inactiveFile.exists()) BitmapFactory.decodeFile(inactiveFile.absolutePath) else null

                fun setInactiveState() {
                    if (inactiveBmp != null) {
                        buttonView.setImageBitmap(inactiveBmp)
                        buttonView.background = null
                    } else {
                        buttonView.setImageDrawable(null)
                        buttonView.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#6622C55E"))
                            setStroke(3, Color.parseColor("#AA22C55E"))
                        }
                    }
                }

                fun setActiveState() {
                    if (activeBmp != null) {
                        buttonView.setImageBitmap(activeBmp)
                        buttonView.background = null
                    } else {
                        buttonView.setImageDrawable(null)
                        buttonView.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#FF22C55E"))
                            setStroke(4, Color.WHITE)
                        }
                    }
                }

                setInactiveState()

                buttonView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            setActiveState()
                            sprite.look.yInUserInterfaceDimensionUnit += pwr
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            setInactiveState()
                            true
                        }
                        else -> false
                    }
                }

                val params = FrameLayout.LayoutParams(btnSize, btnSize).apply {
                    leftMargin = px
                    topMargin = py
                }
                stage.addViewToStage("jump_2d_" + sprite.name, buttonView, params)
            } finally {
                finished = true
            }
        }
    }

    override fun restart() {
        super.restart()
        started = false
        finished = false
    }
}
