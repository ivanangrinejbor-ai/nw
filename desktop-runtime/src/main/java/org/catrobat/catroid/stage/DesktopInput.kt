package org.catrobat.catroid.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

class DesktopInput {
    /** Mouse state on the previous frame (for justPressed/justReleased). */
    var wasMouseDown = false
    /** Internal flag for two-frame tracking. */
    private var previousMouseDown = false
    /** Mouse position on the previous frame, in stage coordinates. */
    private var prevMouseWorldX = 0f
    private var prevMouseWorldY = 0f
    /** Touch state for sensing bricks. */
    var isTouched: Boolean = false
        private set
    /** Finger/mouse position in stage coordinates. */
    var fingerX: Float = 0f
        private set
    var fingerY: Float = 0f
        private set

    /** Simulate a tap at the given stage position (used by TapAtBrick). */
    fun simulateTap(x: Float, y: Float) {
        fingerX = x
        fingerY = y
        isTouched = true
    }

    /** Mouse delta per frame in stage coordinates. */
    val mouseDeltaX: Float
        get() = mouseWorldX - prevMouseWorldX
    val mouseDeltaY: Float
        get() = mouseWorldY - prevMouseWorldY

    val mouseX: Float
        get() = Gdx.input.x.toFloat()
    val mouseY: Float
        get() = Gdx.input.y.toFloat()

    /** Mouse position in stage coordinates, centered at (0, 0). */
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

    /** Called once per frame before sensing checks. */
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
