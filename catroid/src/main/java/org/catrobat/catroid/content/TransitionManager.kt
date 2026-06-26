package org.catrobat.catroid.content

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import org.catrobat.catroid.stage.StageActivity

enum class TransitionType {
    INSTANT,
    FADE_TO_BLACK,
    FADE_FROM_BLACK,
    CROSSFADE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN
}

enum class TransitionState {
    IDLE,
    FADING_OUT,
    SWITCHING_SCENE,
    FADING_IN
}

class TransitionManager {
    var state = TransitionState.IDLE
    var type = TransitionType.INSTANT
    var targetSceneName = ""
    var duration = 0.5f
    var progress = 0f
    private var overlay: Image? = null
    private var overlayStage: Stage? = null

    fun startTransition(transitionType: TransitionType, sceneName: String) {
        type = transitionType
        targetSceneName = sceneName
        progress = 0f

        when (transitionType) {
            TransitionType.INSTANT -> {
                performSceneSwitch()
                state = TransitionState.IDLE
            }
            TransitionType.FADE_TO_BLACK -> {
                initOverlay()
                state = TransitionState.FADING_OUT
            }
            TransitionType.FADE_FROM_BLACK -> {
                performSceneSwitch()
                initOverlay()
                overlay?.color?.a = 1f
                state = TransitionState.FADING_IN
            }
            TransitionType.CROSSFADE -> {
                initOverlay()
                state = TransitionState.FADING_OUT
            }
            TransitionType.SLIDE_LEFT, TransitionType.SLIDE_RIGHT,
            TransitionType.SLIDE_UP, TransitionType.SLIDE_DOWN -> {
                performSceneSwitch()
                initOverlay()
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
                progress += delta / duration
                if (progress >= 1f) {
                    progress = 1f
                    overlay?.color?.a = 1f
                    performSceneSwitch()
                    if (type == TransitionType.CROSSFADE) {
                        state = TransitionState.FADING_IN
                        progress = 0f
                    } else {
                        overlay?.color?.a = 0f
                        state = TransitionState.IDLE
                    }
                } else {
                    val alpha = Interpolation.pow2.apply(progress)
                    overlay?.color?.a = alpha
                }
            }
            TransitionState.FADING_IN -> {
                progress += delta / duration
                if (progress >= 1f) {
                    progress = 1f
                    overlay?.color?.a = 0f
                    state = TransitionState.IDLE
                } else {
                    val alpha = Interpolation.pow2Out.apply(1f - progress)
                    overlay?.color?.a = alpha
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
            overlayStage?.view?.apply()
            overlayStage?.act()
            overlayStage?.draw()
            batch.begin()
        }
    }

    private fun performSceneSwitch() {
        val listener = StageActivity.getActiveStageListener() ?: return
        listener.transitionToScene(targetSceneName)
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
