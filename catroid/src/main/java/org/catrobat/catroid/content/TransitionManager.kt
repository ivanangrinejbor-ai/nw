package org.catrobat.catroid.content

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.stage.StageActivity

enum class TransitionType {
    NONE,
    INSTANT,
    FADE_TO_BLACK,
    FADE_FROM_BLACK,
    CROSSFADE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN,
    FADE_OUT,
    FADE_IN
}

enum class TransitionState {
    IDLE,
    FADING_OUT,
    FADING_IN
}

class TransitionManager {
    var state = TransitionState.IDLE
    var targetSceneName = ""
    var exitType = TransitionType.NONE
    var enterType = TransitionType.NONE
    private var exitDuration = 0.5f
    private var enterDuration = 0.5f
    private var phaseDuration = 0.5f
    private var progress = 0f
    private var pendingSwitch: Runnable? = null
    private var overlay: Image? = null
    private var overlayStage: Stage? = null

    /**
     * Plays the leaving (exit) transition of the old scene, performs the actual scene
     * switch via [switchAction] at the midpoint, then plays the entering (start) transition
     * of the new scene. If both transitions are NONE the switch happens immediately.
     */
    fun startSceneTransition(
        exit: TransitionType,
        exitDur: Float,
        enter: TransitionType,
        enterDur: Float,
        sceneName: String,
        switchAction: Runnable
    ) {
        targetSceneName = sceneName
        pendingSwitch = switchAction
        exitType = exit
        enterType = enter
        exitDuration = exitDur.coerceAtLeast(0.1f)
        enterDuration = enterDur.coerceAtLeast(0.1f)

        when {
            exit != TransitionType.NONE -> {
                initOverlay()
                overlay?.color?.a = 0f
                phaseDuration = exitDuration
                progress = 0f
                state = TransitionState.FADING_OUT
            }
            enter != TransitionType.NONE -> {
                runPendingSwitch()
                initOverlay()
                overlay?.color?.a = 1f
                phaseDuration = enterDuration
                progress = 0f
                state = TransitionState.FADING_IN
            }
            else -> {
                runPendingSwitch()
                state = TransitionState.IDLE
            }
        }
    }

    // --- Backward-compatible API used by the transition blocks (Crossfade/Fade/Slide) ---
    fun startTransition(transitionType: TransitionType, sceneName: String) {
        startTransition(transitionType, sceneName, 0.5f)
    }

    fun startTransition(transitionType: TransitionType, sceneName: String, dur: Float) {
        targetSceneName = sceneName
        pendingSwitch = Runnable { performSceneSwitch() }
        when (transitionType) {
            TransitionType.NONE -> {
                state = TransitionState.IDLE
            }
            TransitionType.INSTANT -> {
                runPendingSwitch()
                state = TransitionState.IDLE
            }
            TransitionType.FADE_TO_BLACK, TransitionType.FADE_OUT -> {
                initOverlay()
                overlay?.color?.a = 0f
                exitType = TransitionType.FADE_OUT
                enterType = TransitionType.NONE
                phaseDuration = dur.coerceAtLeast(0.1f)
                progress = 0f
                state = TransitionState.FADING_OUT
            }
            TransitionType.FADE_FROM_BLACK, TransitionType.FADE_IN -> {
                runPendingSwitch()
                initOverlay()
                overlay?.color?.a = 1f
                exitType = TransitionType.NONE
                enterType = TransitionType.FADE_IN
                phaseDuration = dur.coerceAtLeast(0.1f)
                progress = 0f
                state = TransitionState.FADING_IN
            }
            TransitionType.CROSSFADE -> {
                initOverlay()
                overlay?.color?.a = 0f
                exitType = TransitionType.FADE_OUT
                enterType = TransitionType.FADE_IN
                phaseDuration = dur.coerceAtLeast(0.1f)
                progress = 0f
                state = TransitionState.FADING_OUT
            }
            TransitionType.SLIDE_LEFT, TransitionType.SLIDE_RIGHT,
            TransitionType.SLIDE_UP, TransitionType.SLIDE_DOWN -> {
                runPendingSwitch()
                initOverlay()
                overlay?.color?.a = 1f
                exitType = TransitionType.NONE
                enterType = TransitionType.FADE_IN
                phaseDuration = dur.coerceAtLeast(0.1f)
                progress = 0f
                state = TransitionState.FADING_IN
            }
        }
    }

    private fun initOverlay() {
        if (overlay == null) {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(0f, 0f, 0f, 1f)
            pixmap.fill()
            val tex = Texture(pixmap)
            pixmap.dispose()
            val drawable: Drawable = TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(tex))
            overlay = Image(drawable)
            overlay?.setSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
            overlayStage = Stage()
            overlayStage?.addActor(overlay)
        }
        overlay?.color?.a = 0f
        overlayStage?.viewport?.update(Gdx.graphics.width, Gdx.graphics.height, true)
    }

    fun update(delta: Float) {
        when (state) {
            TransitionState.FADING_OUT -> {
                progress += delta / phaseDuration
                if (progress >= 1f) {
                    overlay?.color?.a = 1f
                    runPendingSwitch()
                    if (enterType != TransitionType.NONE) {
                        overlay?.color?.a = 1f
                        phaseDuration = enterDuration
                        progress = 0f
                        state = TransitionState.FADING_IN
                    } else {
                        overlay?.color?.a = 0f
                        state = TransitionState.IDLE
                    }
                } else {
                    overlay?.color?.a = Interpolation.pow2.apply(progress)
                }
            }
            TransitionState.FADING_IN -> {
                progress += delta / phaseDuration
                if (progress >= 1f) {
                    overlay?.color?.a = 0f
                    state = TransitionState.IDLE
                } else {
                    overlay?.color?.a = Interpolation.pow2Out.apply(1f - progress)
                }
            }
            else -> {}
        }
    }

    fun renderOverlay(batch: SpriteBatch) {
        if (state != TransitionState.IDLE && overlayStage != null) {
            batch.end()
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            overlayStage?.viewport?.apply()
            overlayStage?.act()
            overlayStage?.draw()
            batch.begin()
        }
    }

    private fun performSceneSwitch() {
        val listener = StageActivity.getActiveStageListener() ?: return
        val newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(targetSceneName)
        if (newScene != null) {
            listener.doSceneSwitch(newScene)
        }
    }

    private fun runPendingSwitch() {
        pendingSwitch?.run()
        pendingSwitch = null
    }

    fun cancelTransition() {
        state = TransitionState.IDLE
        overlay?.color?.a = 0f
    }

    fun resize(width: Int, height: Int) {
        overlayStage?.viewport?.update(width, height, true)
        overlay?.setSize(width.toFloat(), height.toFloat())
    }

    fun clearScene() {
        cancelTransition()
        targetSceneName = ""
    }

    fun dispose() {
        overlay = null
        overlayStage?.dispose()
        overlayStage = null
    }
}
