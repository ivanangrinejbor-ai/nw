package org.catrobat.catroid.stage

import com.badlogic.gdx.ApplicationAdapter
import java.io.File
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import org.catrobat.catroid.audio.AudioServiceHolder
import org.catrobat.catroid.audio.MidiServiceHolder
import org.catrobat.catroid.notification.NotificationServiceHolder
import org.catrobat.catroid.text.TextServiceHolder

/**
 * Desktop (LWJGL3) application listener — replaces Android-specific
 * [org.catrobat.catroid.stage.StageListener] on desktop.
 *
 * Initialises desktop services (audio, MIDI, text, notifications),
 * sets up the camera and begins the render loop. The project is loaded
 * inside [create] (via [DesktopProjectManager.loadProject]) once the
 * libGDX application and GL context are ready, and all project sprites
 * are rendered here, including pen drawing commands
 * (lines, circles, rectangles, text, filled polygons, stamps).
 */
class DesktopStageListener(private val projectDir: File? = null) : ApplicationAdapter() {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var font: BitmapFont
    /** Cached Cyrillic-capable HUD texture (rendered once per project). */
    private var hudProjectTex: Texture? = null
    private lateinit var physicsWorld: DesktopPhysicsWorld
    private lateinit var scriptEngine: DesktopScriptEngine
    private lateinit var scriptRunner: DesktopScriptRunner
    private lateinit var input: DesktopInput
    private lateinit var cameraState: DesktopCameraState
    private var lastFrameNanos: Long = 0L

    /** Pen layer FBO cache — pen commands are rendered incrementally into this texture. */
    private var penFbo: FrameBuffer? = null
    private var penFboSprite: com.badlogic.gdx.graphics.g2d.Sprite? = null
    /** Tracks how many pen commands have already been rendered into the FBO. */
    private val penRenderedCounts = mutableMapOf<String, Int>()

    private val VIRTUAL_WIDTH: Float = 1280f
    private val VIRTUAL_HEIGHT: Float = 720f

    override fun create() {
        Gdx.app.log(TAG, "DesktopStageListener created")

        // Загружаем проект после старта приложения: здесь доступны Gdx.app и
        // GL-контекст, необходимые для loadProject (Texture look'ов + логи).
        // Раньше loadProject вызывался из DesktopStage.main() до создания
        // Lwjgl3Application — из-за этого Gdx.app был null и падал NPE.
        projectDir?.let { DesktopProjectManager.getInstance().loadProject(it) }

        // Camera: Y axis down (as in Catrobat: x right, y down from centre).
        // FitViewport сохраняет пропорции 16:9 с чёрными полями (letterbox), чтобы
        // спрайты не растягивались на окнах с другим соотношением сторон.
        camera = OrthographicCamera()
        viewport = FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)

        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        font = BitmapFont()

        // Initialize pen FBO for cached pen rendering
        try {
            penFbo = FrameBuffer(Pixmap.Format.RGBA8888, VIRTUAL_WIDTH.toInt(), VIRTUAL_HEIGHT.toInt(), false)
            penFboSprite = com.badlogic.gdx.graphics.g2d.Sprite(penFbo!!.colorBufferTexture)
            penFboSprite?.setFlip(false, true) // FBO textures are Y-flipped
        } catch (_: Exception) {
            // FBO not supported — fall back to direct rendering
            penFbo = null
        }

        input = DesktopInput()
        physicsWorld = DesktopPhysicsWorld(0f, -9.8f)
        cameraState = DesktopCameraState()
        val project = DesktopProjectManager.getInstance().getCurrentProject()
        scriptEngine = project?.let { DesktopScriptEngine(it, physicsWorld, input, cameraState) } ?: DesktopScriptEngine(DesktopProject("empty"), physicsWorld, input, cameraState)
        scriptRunner = project?.let { DesktopScriptRunner(it, input) } ?: DesktopScriptRunner(DesktopProject("empty"), input)
        lastFrameNanos = System.nanoTime()

        // Start the script engine
        scriptEngine.start()

        // Services already registered in DesktopStage.main()
        Gdx.app.log(TAG, "Services: audio=${AudioServiceHolder.audioService != null}, " +
                "midi=${MidiServiceHolder.midiService != null}, " +
                "text=${TextServiceHolder.textService != null}, " +
                "notifications=${NotificationServiceHolder.service != null}")

        // Load project (if not already loaded in main)
        DesktopProjectManager.getInstance().getCurrentProject()
            ?: run { Gdx.app.log(TAG, "No project loaded — running empty stage") }
    }

    override fun render() {
        // Update input state ONCE per frame (before script checks)
        input.update()

        // Clear the screen
        ScreenUtils.clear(0.95f, 0.95f, 0.95f, 1f)

        // Применяем состояние эмулированной 3D-камеры (pan / zoom / поворот).
        // Пропорции и glViewport уже заданы FitViewport в resize(); setToOrtho здесь
        // не вызываем, чтобы не сбрасывать letterbox и не растягивать изображение.
        // Каждый кадр сбрасываем ориентацию, иначе camera.rotate накапливается.
        camera.direction.set(0f, 0f, -1f)
        camera.up.set(0f, 1f, 0f)
        camera.position.set(VIRTUAL_WIDTH / 2f + cameraState.x, VIRTUAL_HEIGHT / 2f - cameraState.y, 0f)
        val projectForCamera = DesktopProjectManager.getInstance().getCurrentProject()
        cameraState.followTargetName?.let { targetName ->
            val target = projectForCamera?.sprites?.find { it.name == targetName }
            if (target != null) {
                camera.position.set(
                    VIRTUAL_WIDTH / 2f + target.x + cameraState.followOffsetX,
                    VIRTUAL_HEIGHT / 2f - (target.y + cameraState.followOffsetY),
                    0f
                )
            }
        }
        camera.zoom = cameraState.zoom
        if (cameraState.rotation != 0f) {
            camera.rotate(com.badlogic.gdx.math.Vector3(0f, 0f, 1f), cameraState.rotation)
        }
        camera.update()
        batch.projectionMatrix = camera.combined

        val project = DesktopProjectManager.getInstance().getCurrentProject()
        if (project != null) {
            scriptEngine.update(Gdx.graphics.deltaTime)
            scriptRunner.update(Gdx.graphics.deltaTime)
            physicsWorld.step(Gdx.graphics.deltaTime)

            // ── 1. Render sprites ──
            batch.begin()
            for (sprite in project.sprites) {
                if (!sprite.visible) continue
                if (sprite.sprite == null) sprite.buildSprite()
                // Sprites pinned/attached to the camera stay fixed on screen (compensate pan + zoom).
                cameraState.cameraPinned[sprite.name]?.let { (sx, sy) ->
                    sprite.x = cameraState.x + sx / cameraState.zoom
                    sprite.y = -cameraState.y + sy / cameraState.zoom
                }
                // Centre of screen = (0, 0) in Catrobat coords; Y inverted
                val screenX = VIRTUAL_WIDTH / 2f + sprite.x
                val screenY = VIRTUAL_HEIGHT / 2f - sprite.y
                sprite.sprite?.setPosition(
                    screenX - sprite.sprite!!.width / 2f,
                    screenY - sprite.sprite!!.height / 2f
                )
                sprite.sprite?.setColor(
                    sprite.objectColorRed.coerceIn(0f, 1f),
                    sprite.objectColorGreen.coerceIn(0f, 1f),
                    sprite.objectColorBlue.coerceIn(0f, 1f),
                    (1f - sprite.transparency / 100f).coerceIn(0f, 1f)
                )
                sprite.sprite?.draw(batch)
            }
            batch.end()

            // ── 2. Render pen shapes (incremental FBO caching) ──
            val fbo = penFbo
            if (fbo != null) {
                // Render only NEW pen commands into the FBO
                fbo.begin()
                // We need to set up an orthographic projection for the FBO
                val penCam = OrthographicCamera(VIRTUAL_WIDTH, VIRTUAL_HEIGHT)
                penCam.setToOrtho(true) // Y-down to match stage coords
                penCam.update()
                shapeRenderer.projectionMatrix = penCam.combined
                for (sprite in project.sprites) {
                    val cmds = sprite.penDrawCommands
                    if (cmds.isEmpty()) continue
                    val key = sprite.name
                    val startIdx = penRenderedCounts[key] ?: 0
                    if (startIdx >= cmds.size) continue
                    for (i in startIdx until cmds.size) {
                        drawPenCommand(cmds[i])
                    }
                    penRenderedCounts[key] = cmds.size
                }
                fbo.end()
                // Draw the cached pen texture in the main batch
                batch.begin()
                penFboSprite?.draw(batch)
                batch.end()
            } else {
                // Fallback: direct rendering (no FBO)
                shapeRenderer.projectionMatrix = camera.combined
                for (sprite in project.sprites) {
                    val cmds = sprite.penDrawCommands
                    if (cmds.isEmpty()) continue
                    for (cmd in cmds) {
                        drawPenCommand(cmd)
                    }
                }
            }

            // ── 3. Render stamps and text ──
            batch.begin()
            for (sprite in project.sprites) {
                for (cmd in sprite.penDrawCommands) {
                    when (cmd) {
                        is PenDrawCommand.StampSprite -> {
                            val tex = cmd.texture
                            if (tex != null) {
                                batch.draw(tex, cmd.x - cmd.width / 2f, cmd.y - cmd.height / 2f,
                                    cmd.width, cmd.height)
                            }
                        }
                        is PenDrawCommand.DrawText -> {
                            font.draw(batch, cmd.text, cmd.x, cmd.y)
                        }
                        else -> { /* handled in shape pass */ }
                    }
                }
            }

            // ── 4. Render text overlays (think/say bubbles, show text) ──
            val VIRTUAL_WIDTH_F = VIRTUAL_WIDTH
            val VIRTUAL_HEIGHT_F = VIRTUAL_HEIGHT
            for (overlay in scriptEngine.textOverlays.values) {
                if (!overlay.visible) continue
                val sx = VIRTUAL_WIDTH_F / 2f + overlay.x
                val sy = VIRTUAL_HEIGHT_F / 2f - overlay.y
                val prefix = if (overlay.isThink) "🤔 " else "💬 "
                font.draw(batch, "$prefix${overlay.text}", sx, sy)
            }

            // ── 5. Render HUD text ──
            // Project name may be Cyrillic — BitmapFont lacks those glyphs,
            // so render it once via CyrillicText (AWT + system TTF).
            if (hudProjectTex == null) {
                hudProjectTex = CyrillicText.render(
                    "Project: ${project.name} | Sprites: ${project.sprites.size}")
            }
            hudProjectTex?.let { batch.draw(it, 20f, (VIRTUAL_HEIGHT - 24f) - it.height) }
            font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 20f, 32f)
            batch.end()
        } else {
            batch.begin()
            font.draw(batch, "NeoCatroid Desktop Player (LWJGL3)", 32f, VIRTUAL_HEIGHT - 48f)
            font.draw(batch, "No project loaded. Pass project path as argument.", 32f, VIRTUAL_HEIGHT - 84f)
            font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 20f, 32f)
            batch.end()
        }

        val requestedFps = scriptEngine.getTargetFps()
        if (requestedFps > 0 && requestedFps != 60) {
            // Use libGDX foreground FPS limiter instead of Thread.sleep
            // (already set via config.setForegroundFPS in DesktopStage)
        }
        lastFrameNanos = System.nanoTime()
    }

    override fun resize(width: Int, height: Int) {
        // FitViewport пересчитывает glViewport с сохранением пропорций 16:9.
        viewport.update(width, height)
    }

    override fun dispose() {
        // Stop services
        AudioServiceHolder.audioService?.stopAllSounds()
        AudioServiceHolder.audioService?.clear()
        MidiServiceHolder.midiService?.stopAllSounds()
        batch.dispose()
        shapeRenderer.dispose()
        font.dispose()
        hudProjectTex?.dispose()
        hudProjectTex = null
        penFbo?.dispose()
        penFbo = null
        physicsWorld.dispose()
        DesktopProjectManager.getInstance().clear()
    }

    /** Draw a single pen command using the shapeRenderer (used by both FBO and fallback paths). */
    private fun drawPenCommand(cmd: PenDrawCommand) {
        when (cmd) {
            is PenDrawCommand.DrawLine -> {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                shapeRenderer.color = com.badlogic.gdx.graphics.Color(
                    cmd.colorRed / 255f, cmd.colorGreen / 255f, cmd.colorBlue / 255f, 1f)
                shapeRenderer.line(cmd.x1, cmd.y1, cmd.x2, cmd.y2)
                shapeRenderer.end()
            }
            is PenDrawCommand.DrawCircle -> {
                shapeRenderer.begin(if (cmd.fill) ShapeRenderer.ShapeType.Filled else ShapeRenderer.ShapeType.Line)
                shapeRenderer.color = com.badlogic.gdx.graphics.Color(
                    cmd.colorRed / 255f, cmd.colorGreen / 255f, cmd.colorBlue / 255f, 1f)
                shapeRenderer.circle(cmd.cx, cmd.cy, cmd.radius)
                shapeRenderer.end()
            }
            is PenDrawCommand.DrawRect -> {
                shapeRenderer.begin(if (cmd.fill) ShapeRenderer.ShapeType.Filled else ShapeRenderer.ShapeType.Line)
                shapeRenderer.color = com.badlogic.gdx.graphics.Color(
                    cmd.colorRed / 255f, cmd.colorGreen / 255f, cmd.colorBlue / 255f, 1f)
                shapeRenderer.rect(cmd.x - cmd.width / 2f, cmd.y - cmd.height / 2f, cmd.width, cmd.height)
                shapeRenderer.end()
            }
            is PenDrawCommand.FillPolygon -> {
                if (cmd.points.size >= 3) {
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                    shapeRenderer.color = com.badlogic.gdx.graphics.Color(
                        cmd.colorRed / 255f, cmd.colorGreen / 255f, cmd.colorBlue / 255f, 1f)
                    val verts = cmd.points.flatMap { listOf(it.first, it.second) }.toFloatArray()
                    shapeRenderer.polygon(verts)
                    shapeRenderer.end()
                }
            }
            is PenDrawCommand.DrawText,
            is PenDrawCommand.StampSprite -> { /* handled in batch pass */ }
        }
    }

    companion object {
        private const val TAG = "DesktopStageListener"
    }
}
