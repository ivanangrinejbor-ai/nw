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
import org.catrobat.catroid.desktop.audio.DesktopSoundManager
import org.catrobat.catroid.desktop.hardware.DesktopHardwareBridge
import org.catrobat.catroid.desktop.project.DesktopCodeParser
import org.catrobat.catroid.desktop.project.DesktopProject
import org.catrobat.catroid.desktop.project.DesktopProjectLoader
import org.catrobat.catroid.desktop.project.DesktopSound
import org.catrobat.catroid.desktop.stage.DesktopEngine
import org.catrobat.catroid.desktop.stage.DesktopSpriteRuntime
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

    private var project: DesktopProject? = null
    private var engine: DesktopEngine? = null
    private var soundManager: DesktopSoundManager? = null
    private var virtualWidth = 1280f
    private var virtualHeight = 720f
    private var loadError: String? = null

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDragging = false

    override fun create() {
        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        camera = OrthographicCamera()

        Gdx.input.inputProcessor = this
        Gdx.app.log("DesktopGame", "NeoCatroid desktop player started for: ${projectDir.name}")

        loadProject()

        viewport = FitViewport(virtualWidth, virtualHeight, camera)
        viewport.apply()
        camera.position.set(virtualWidth / 2f, virtualHeight / 2f, 0f)
        camera.update()

        font = BitmapFont().apply {
            data.setScale(1.3f)
        }
    }

    private fun loadProject() {
        try {
            val loader = DesktopProjectLoader(projectDir)
            val codeXml = loader.findCodeXml(projectDir)
                ?: throw IllegalStateException("code.xml не найден в ${projectDir.absolutePath}")
            val parsed = DesktopCodeParser().parse(codeXml)

            virtualWidth = parsed.screenWidth.toFloat()
            virtualHeight = parsed.screenHeight.toFloat()

            soundManager = DesktopSoundManager(loader.getSoundsDir(projectDir))
            val eng = DesktopEngine(parsed, projectDir)
            eng.setSoundBridge(object : DesktopEngine.DesktopSoundManagerBridge {
                override fun playSound(sound: DesktopSound?) {
                    if (sound != null) soundManager?.playSound(sound.fileName)
                }

                override fun stopAll() {
                    soundManager?.stopAllSounds()
                }

                override fun setVolume(v: Float) {
                    soundManager?.setGlobalVolume(v / 100f)
                }

                override fun changeVolume(delta: Float) {
                    soundManager?.setGlobalVolume(((soundManager?.let { volumeOf() } ?: 100f) + delta) / 100f)
                }

                override fun durationOf(sound: DesktopSound): Float = 1.5f
            })
            engine = eng
            eng.start()

            project = parsed
            Gdx.app.log("DesktopGame", "Проект загружен: ${parsed.name}, " +
                "${parsed.screenWidth}x${parsed.screenHeight}, спрайтов: ${parsed.startScene().sprites.size}")
            DesktopToastManager.showToast("Проект: ${parsed.name}", 2.0f)
        } catch (e: Exception) {
            loadError = e.message ?: e.toString()
            Gdx.app.error("DesktopGame", "Ошибка загрузки проекта", e)
            DesktopToastManager.showToast("Ошибка загрузки: ${loadError}", 4.0f)
        }
    }

    private fun volumeOf(): Float = 100f

    override fun render() {
        val dt = Gdx.graphics.deltaTime

        Gdx.gl.glClearColor(0.10f, 0.10f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        val engCam = engine
        if (engCam != null) {
            camera.position.set(
                virtualWidth / 2f + engCam.cameraOffsetX,
                virtualHeight / 2f + engCam.cameraOffsetY,
                0f
            )
        }
        camera.update()
        batch.projectionMatrix = camera.combined
        shapeRenderer.projectionMatrix = camera.combined

        val eng = engine
        if (eng != null) {
            eng.tick(dt)
            renderScene(eng)
            eng.fast2d.render(batch, eng.textures, virtualWidth, virtualHeight)
        } else {
            batch.begin()
            font.color = Color.WHITE
            font.draw(batch, "NeoCatroid Player (LWJGL 3 Engine)", 40f, virtualHeight - 40f)
            font.draw(batch, "Проект: ${projectDir.name}", 40f, virtualHeight - 75f)
            font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 40f, virtualHeight - 110f)
            if (loadError != null) {
                font.color = Color.RED
                font.draw(batch, "Ошибка загрузки: $loadError", 40f, virtualHeight / 2f)
            }
            font.color = Color.GRAY
            font.draw(batch, "F11: Полный экран | ESC: Выход | F5: Эмуляция встряхивания", 40f, 45f)
            batch.end()
        }

        DesktopToastManager.render(batch, font, shapeRenderer, virtualWidth, virtualHeight, dt)
    }

    private fun renderScene(eng: DesktopEngine) {
        batch.begin()
        for (sprite in eng.sprites) {
            if (!sprite.visible) continue
            renderSprite(eng, sprite)
        }
        renderTextBubbles(eng)
        renderTextWidgets(eng)
        batch.end()
    }

    private fun renderSprite(eng: DesktopEngine, sprite: DesktopSpriteRuntime) {
        val look = sprite.currentLook() ?: return
        val texture = eng.textures[look.fileName] ?: return

        val w = eng.widthOf(sprite)
        val h = eng.heightOf(sprite)
        if (w < 1f || h < 1f) return

        val screenX = virtualWidth / 2f + sprite.x
        val screenY = virtualHeight / 2f + sprite.y

        val alpha = (1f - sprite.transparency / 100f).coerceIn(0f, 1f)
        val brightness = 1f + sprite.brightness / 100f

        batch.setColor(brightness, brightness, brightness, alpha)
        batch.draw(
            texture,
            screenX - w / 2f, screenY - h / 2f,
            w / 2f, h / 2f,
            w, h,
            1f, 1f,
            -sprite.rotation,
            0, 0, texture.width, texture.height,
            sprite.xScale < 0f, sprite.yScale < 0f
        )
        batch.setColor(Color.WHITE)
    }

    private fun renderTextBubbles(eng: DesktopEngine) {
        for (t in eng.texts) {
            val screenX = virtualWidth / 2f + t.x
            val screenY = virtualHeight / 2f + t.y + 40f
            font.draw(batch, t.text, screenX, screenY)
        }
    }

    private fun renderTextWidgets(eng: DesktopEngine) {
        for (w in eng.textWidgets) {
            val value = eng.variables[w.variableName] ?: eng.spriteVariables[w.spriteName]?.get(w.variableName) ?: ""
            val text = when (value) {
                is Double -> if (value == kotlin.math.floor(value)) value.toLong().toString() else value.toString()
                else -> value.toString()
            }
            if (text.isEmpty()) continue
            val screenX = virtualWidth / 2f + w.x
            val screenY = virtualHeight / 2f + w.y
            font.color = parseColor(w.colorHex)
            font.data.setScale((w.size / 100f).coerceAtLeast(0.2f))
            font.draw(batch, text, screenX, screenY)
            font.data.setScale(1.3f)
            font.color = Color.WHITE
        }
    }

    private fun parseColor(hex: String): Color {
        return try {
            val cleaned = hex.removePrefix("#")
            when (cleaned.length) {
                6 -> Color.valueOf("$cleaned" + "ff")
                8 -> Color.valueOf(cleaned)
                else -> Color.WHITE
            }
        } catch (e: Exception) {
            Color.WHITE
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        engine?.onWindowResized(width, height)
    }

    override fun pause() {
        engine?.onAppMinimized()
    }

    override fun resume() {
        engine?.onAppRestored()
    }

    override fun dispose() {
        engine?.dispose()
        soundManager?.dispose()
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
        engine?.let { eng ->
            val wx = virtualWidth / 2f + (screenX - Gdx.graphics.width / 2f) * (virtualWidth / Gdx.graphics.width)
            val wy = virtualHeight / 2f - (screenY - Gdx.graphics.height / 2f) * (virtualHeight / Gdx.graphics.height)
            eng.mouseX = wx
            eng.mouseY = wy
            eng.isTouched = true
            eng.touchDownSprite = eng.spriteAt(wx, wy)
            eng.onTouchDown(wx, wy)
            eng.onMouseButton(button)
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        engine?.let { eng ->
            eng.isTouched = false
            val wx = virtualWidth / 2f + (screenX - Gdx.graphics.width / 2f) * (virtualWidth / Gdx.graphics.width)
            val wy = virtualHeight / 2f - (screenY - Gdx.graphics.height / 2f) * (virtualHeight / Gdx.graphics.height)
            eng.mouseX = wx
            eng.mouseY = wy
        }
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
                engine?.onSwipe(deltaX.toFloat(), deltaY.toFloat())
            }
        }
        isDragging = false
        val eng = engine
        eng?.onSpriteReleased(eng.touchDownSprite)
        eng?.touchDownSprite = null
        return true
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        isDragging = false
        engine?.isTouched = false
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        engine?.let { eng ->
            val wx = virtualWidth / 2f + (screenX - Gdx.graphics.width / 2f) * (virtualWidth / Gdx.graphics.width)
            val wy = virtualHeight / 2f - (screenY - Gdx.graphics.height / 2f) * (virtualHeight / Gdx.graphics.height)
            eng.mouseX = wx
            eng.mouseY = wy
            eng.isTouched = true
            eng.onFingerMoved()
        }
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        engine?.let { eng ->
            val wx = virtualWidth / 2f + (screenX - Gdx.graphics.width / 2f) * (virtualWidth / Gdx.graphics.width)
            val wy = virtualHeight / 2f - (screenY - Gdx.graphics.height / 2f) * (virtualHeight / Gdx.graphics.height)
            eng.mouseX = wx
            eng.mouseY = wy
        }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        engine?.onScrolled(amountY)
        return false
    }
}