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

        // Минимальная логика: двигаем первый спрайт к тачу/мыши
        if (project.sprites.isNotEmpty()) {
            val sprite = project.sprites[0]
            if (touchState.isDown) {
                sprite.x += (touchState.x - sprite.x) * 4 * deltaSeconds
                sprite.y += (touchState.y - sprite.y) * 4 * deltaSeconds
            }
            if (keyState.left) sprite.x -= 120 * deltaSeconds
            if (keyState.right) sprite.x += 120 * deltaSeconds
            if (keyState.up) sprite.y += 120 * deltaSeconds
            if (keyState.down) sprite.y -= 120 * deltaSeconds
            if (keyState.space) sprite.direction = (sprite.direction + 90 * deltaSeconds) % 360f
        }
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