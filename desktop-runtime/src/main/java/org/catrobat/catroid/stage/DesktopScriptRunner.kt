package org.catrobat.catroid.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array

/**
 * Десктопный раннер скриптов.
 *
 * На Android используется LunoScript + ActionFactory + ScriptSequenceAction.
 * На десктопе пока что это минимальная заглушка, которая:
 *  - эмулирует тач/сенсоры через мышь/клавиатуру
 *  - выполняет простые встроенные команды движения/поворота
 *  -可作为 точка подключения полного интерпретатора LunoScript в будущем.
 */
class DesktopScriptRunner(private val project: DesktopProject,
                          private val input: DesktopInput) {

    private val touchState = TouchState()
    private val keyState = KeyState()

    fun update(deltaSeconds: Float) {
        val mouseScreen = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        val stageX = mouseScreen.x - VIRTUAL_WIDTH / 2f
        val stageY = VIRTUAL_HEIGHT / 2f - mouseScreen.y
        touchState.updateMouse(stageX, stageY, Gdx.input.isButtonPressed(Input.Buttons.LEFT))

        // Эмулируем сенсор наклона по стрелкам
        keyState.left = Gdx.input.isKeyPressed(Input.Keys.LEFT)
        keyState.right = Gdx.input.isKeyPressed(Input.Keys.RIGHT)
        keyState.up = Gdx.input.isKeyPressed(Input.Keys.UP)
        keyState.down = Gdx.input.isKeyPressed(Input.Keys.DOWN)
        keyState.space = Gdx.input.isKeyPressed(Input.Keys.SPACE)

        // NOTE: sprite movement is driven exclusively by DesktopScriptEngine (direct
        // position writes from motion/glide/etc. bricks). This runner previously
        // also mutated sprite[0].x/y every frame, which conflicted with the engine
        // and caused drift. That redundant write has been removed — the engine is
        // now the single source of truth for sprite positions.
    }

    fun getTouchState(): TouchState = touchState
    fun getKeyState(): KeyState = keyState

    companion object {
        const val VIRTUAL_WIDTH = 1280f
        const val VIRTUAL_HEIGHT = 720f
    }
}

class TouchState {
    var x: Float = 0f
    var y: Float = 0f
    var isDown: Boolean = false
    private var prevDown = false
    var justPressed: Boolean = false
        private set
    var justReleased: Boolean = false
        private set

    fun updateMouse(x: Float, y: Float, down: Boolean) {
        this.x = x
        this.y = y
        justPressed = down && !prevDown
        justReleased = !down && prevDown
        isDown = down
        prevDown = down
    }
}

class KeyState {
    var left = false
    var right = false
    var up = false
    var down = false
    var space = false
}