package org.catrobat.catroid.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

class DesktopInput {
    var wasMouseDown = false
    private var previousMouseDown = false
    private var prevMouseWorldX = 0f
    private var prevMouseWorldY = 0f
    var isTouched: Boolean = false
        private set
    var fingerX: Float = 0f
        private set
    var fingerY: Float = 0f
        private set

    fun simulateTap(x: Float, y: Float) {
        fingerX = x
        fingerY = y
        isTouched = true
    }

    val mouseDeltaX: Float
        get() = mouseWorldX - prevMouseWorldX
    val mouseDeltaY: Float
        get() = mouseWorldY - prevMouseWorldY

    val mouseX: Float
        get() = Gdx.input.x.toFloat()
    val mouseY: Float
        get() = Gdx.input.y.toFloat()

    val mouseWorldX: Float
        get() = mouseX - Gdx.graphics.width / 2f
    val mouseWorldY: Float
        get() = Gdx.graphics.height / 2f - mouseY

    val isMouseDown: Boolean
        get() = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
    val isMouseJustPressed: Boolean
        get() = isMouseDown && !wasMouseDown
    val isMouseJustReleased: Boolean
        get() = !isMouseDown && wasMouseDown

    init {
        previousMouseDown = isMouseDown
        prevMouseWorldX = mouseWorldX
        prevMouseWorldY = mouseWorldY
    }

    var mouseScroll: Float = 0f
        private set

    fun update() {
        wasMouseDown = previousMouseDown
        previousMouseDown = isMouseDown
        isTouched = isMouseDown
        fingerX = mouseWorldX
        fingerY = mouseWorldY
        prevMouseWorldX = mouseWorldX
        prevMouseWorldY = mouseWorldY
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
