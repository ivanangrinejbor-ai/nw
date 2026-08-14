package org.catrobat.catroid.desktop

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import org.catrobat.catroid.desktop.hardware.DesktopHardwareBridge
import org.catrobat.catroid.desktop.ui.DesktopToastManager
import java.io.File

class DesktopGameListener(
    private val projectDir: File
) : ApplicationListener, InputProcessor {

    private lateinit var batch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: Viewport
    private lateinit var font: BitmapFont

    private val virtualWidth = 1280f
    private val virtualHeight = 720f

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDragging = false

    override fun create() {
        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        camera = OrthographicCamera()
        viewport = FitViewport(virtualWidth, virtualHeight, camera)
        viewport.apply()

        camera.position.set(virtualWidth / 2f, virtualHeight / 2f, 0f)
        camera.update()

        font = BitmapFont().apply {
            data.setScale(1.3f)
        }

        Gdx.input.inputProcessor = this
        Gdx.app.log("DesktopGame", "NeoCatroid desktop player started for: ${projectDir.name}")

        DesktopToastManager.showToast("Игра запущена: ${projectDir.name}", 2.5f)
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime

        Gdx.gl.glClearColor(0.10f, 0.10f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined
        shapeRenderer.projectionMatrix = camera.combined

        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "NeoCatroid Player (LWJGL 3 Engine)", 40f, virtualHeight - 40f)
        font.draw(batch, "Проект: ${projectDir.name}", 40f, virtualHeight - 75f)
        font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 40f, virtualHeight - 110f)

        font.color = Color.GRAY
        font.draw(batch, "F11: Полный экран | ESC: Выход | F5: Эмуляция встряхивания", 40f, 45f)
        batch.end()

        DesktopToastManager.render(batch, font, shapeRenderer, virtualWidth, virtualHeight, dt)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun pause() {}
    override fun resume() {}

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        font.dispose()
    }

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.ESCAPE -> {
                Gdx.app.exit()
                return true
            }
            Input.Keys.F11 -> {
                if (Gdx.graphics.isFullscreen) {
                    Gdx.graphics.setWindowedMode(1280, 720)
                } else {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
                }
                return true
            }
            Input.Keys.F5 -> {
                DesktopHardwareBridge.triggerShake()
                DesktopToastManager.showToast("Сенсор: Встряхивание (Shake)", 1.5f)
                return true
            }
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        if (keycode == Input.Keys.F5) {
            DesktopHardwareBridge.resetShake()
            return true
        }
        return false
    }

    override fun keyTyped(character: Char): Boolean = false

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        touchStartX = screenX.toFloat()
        touchStartY = screenY.toFloat()
        isDragging = true
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (isDragging) {
            val deltaX = screenX - touchStartX
            val deltaY = screenY - touchStartY
            val threshold = 80f

            if (Math.abs(deltaX) > threshold || Math.abs(deltaY) > threshold) {
                val direction = when {
                    Math.abs(deltaX) > Math.abs(deltaY) -> if (deltaX > 0) "Вправо" else "Влево"
                    else -> if (deltaY > 0) "Вниз" else "Вверх"
                }
                DesktopToastManager.showToast("Жест: Свайп $direction", 1.0f)
            }
        }
        isDragging = false
        return true
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        isDragging = false
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
    override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false
    override fun scrolled(amountX: Float, amountY: Float): Boolean = false
}
