package org.catrobat.catroid.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

class DesktopInput {
    /** Состояние мыши на ПРЕДЫДУЩЕМ кадре (для justPressed/justReleased). */
    var wasMouseDown = false
    /** Внутренний флаг для двухкадрового трекинга. */
    private var previousMouseDown = false
    /** Позиция мыши на предыдущем кадре (для delta). */
    private var prevMouseX = 0f
    private var prevMouseY = 0f
    /** Флаг касания для сенсоров. */
    var isTouched: Boolean = false
        private set
    /** Позиция пальца/мыши для сенсоров FINGER_X/Y. */
    var fingerX: Float = 0f
        private set
    var fingerY: Float = 0f
        private set

    /** Simulate a tap at the given position (used by TapAtBrick). */
    fun simulateTap(x: Float, y: Float) {
        fingerX = x
        fingerY = y
        isTouched = true
    }
    /** Дельта мыши за кадр. */
    val mouseDeltaX: Float
        get() = mouseX - prevMouseX
    val mouseDeltaY: Float
        get() = mouseY - prevMouseY

    val mouseX: Float
        get() = Gdx.input.x.toFloat()
    val mouseY: Float
        get() = Gdx.input.y.toFloat()
    val mouseWorldX: Float
        get() = mouseX
    val mouseWorldY: Float
        get() = Gdx.graphics.height - mouseY
    val isMouseDown: Boolean
        get() = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
    val isMouseJustPressed: Boolean
        get() = isMouseDown && !wasMouseDown
    val isMouseJustReleased: Boolean
        get() = !isMouseDown && wasMouseDown

    init {
        previousMouseDown = isMouseDown
        prevMouseX = mouseX
        prevMouseY = mouseY
    }

    var mouseScroll: Float = 0f
        private set
    /** Вызывается ОДИН раз за кадр, ДО проверки состояний. */
    fun update() {
        wasMouseDown = previousMouseDown
        previousMouseDown = isMouseDown
        isTouched = isMouseDown
        fingerX = mouseX
        fingerY = Gdx.graphics.height - mouseY
        prevMouseX = mouseX
        prevMouseY = mouseY
        mouseScroll = Gdx.input.deltaY.toFloat()
    }

    val isLeftPressed: Boolean
        get() = Gdx.input.isKeyPressed(Input.Keys.LEFT)
    val isRightPressed: Boolean
        get() = Gdx.input.isKeyPressed(Input.Keys.RIGHT)
    val isUpPressed: Boolean
        get() = Gdx.input.isKeyPressed(Input.Keys.UP)
    val isDownPressed: Boolean
        get() = Gdx.input.isKeyPressed(Input.Keys.DOWN)
    val isSpacePressed: Boolean
        get() = Gdx.input.isKeyPressed(Input.Keys.SPACE)

    val isKeyJustPressed: (Int) -> Boolean = { Gdx.input.isKeyJustPressed(it) }
}