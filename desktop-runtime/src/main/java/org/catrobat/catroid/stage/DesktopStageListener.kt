package org.catrobat.catroid.stage

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
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
 * through [DesktopStage] before this listener is created, and all
 * project sprites are rendered here, including pen drawing commands
 * (lines, circles, rectangles, text, filled polygons, stamps).
 */
class DesktopStageListener : ApplicationAdapter() {

    private lateinit var camera: OrthographicCamera
    private lateinit var batch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var physicsWorld: DesktopPhysicsWorld
    private lateinit var scriptEngine: DesktopScriptEngine
    private lateinit var scriptRunner: DesktopScriptRunner
    private lateinit var input: DesktopInput
    private lateinit var cameraState: DesktopCameraState

    private val VIRTUAL_WIDTH: Float = 1280f
    private val VIRTUAL_HEIGHT: Float = 720f

    override fun create() {
        Gdx.app.log(TAG, "DesktopStageListener created")

        // Camera: Y axis down (as in Catrobat: x right, y down from centre)
        camera = OrthographicCamera().apply {
            setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT)
            position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0f)
            update()
        }

        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        font = BitmapFont()

        input = DesktopInput()
        physicsWorld = DesktopPhysicsWorld(0f, -9.8f)
        cameraState = DesktopCameraState()
        val project = DesktopProjectManager.getInstance().getCurrentProject()
        scriptEngine = project?.let { DesktopScriptEngine(it, physicsWorld, input, cameraState) } ?: DesktopScriptEngine(DesktopProject("empty"), physicsWorld, input, cameraState)
        scriptRunner = project?.let { DesktopScriptRunner(it, input) } ?: DesktopScriptRunner(DesktopProject("empty"), input)

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

        // Apply the emulated 3D-camera state (pan / zoom / rotate) to the orthographic view.
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT)
        camera.position.set(VIRTUAL_WIDTH / 2f + cameraState.x, VIRTUAL_HEIGHT / 2f - cameraState.y, 0f)
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
                sprite.sprite?.draw(batch)
            }
            batch.end()

            // ── 2. Render pen shapes (lines, circles, rectangles, polygons) ──
            shapeRenderer.projectionMatrix = camera.combined
            for (sprite in project.sprites) {
                val cmds = sprite.penDrawCommands
                if (cmds.isEmpty()) continue
                for (cmd in cmds) {
                    when (cmd) {
                        is PenDrawCommand.DrawLine -> {
                            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                            shapeRenderer.color = com.badlogic.gdx.graphics.Color(
                                cmd.colorRed / 255f, cmd.colorGreen / 255f, cmd.colorBlue / 255f,
                                1f
                            )
                            shapeRenderer.line(cmd.x1, cmd.y1, cmd.x2, cmd.y2)
                            shapeRenderer.end()
                        }
                        is PenDrawCommand.DrawCircle -> {
                            if (cmd.fill) {
                                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                            } else {
                                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                            }
                            shapeRenderer.color = com.badlogic.gdx.graphics.Color(
                                cmd.colorRed / 255f, cmd.colorGreen / 255f, cmd.colorBlue / 255f,
                                1f
                            )
                            shapeRenderer.circle(cmd.cx, cmd.cy, cmd.radius)
                            shapeRenderer.end()
                        }
                        is PenDrawCommand.DrawRect -> {
                            if (cmd.fill) {
                                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                            } else {
                                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                            }
                            shapeRenderer.color = com.badlogic.gdx.graphics.Color(
                                cmd.colorRed / 255f, cmd.colorGreen / 255f, cmd.colorBlue / 255f,
                                1f
                            )
                            shapeRenderer.rect(cmd.x - cmd.width / 2f, cmd.y - cmd.height / 2f,
                                cmd.width, cmd.height)
                            shapeRenderer.end()
                        }
                        is PenDrawCommand.FillPolygon -> {
                            if (cmd.points.size >= 3) {
                                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                                shapeRenderer.color = com.badlogic.gdx.graphics.Color(
                                    cmd.colorRed / 255f, cmd.colorGreen / 255f, cmd.colorBlue / 255f,
                                    1f
                                )
                                val verts = cmd.points.flatMap { listOf(it.first, it.second) }.toFloatArray()
                                shapeRenderer.polygon(verts)
                                shapeRenderer.end()
                            }
                        }
                        is PenDrawCommand.DrawText,
                        is PenDrawCommand.StampSprite -> {
                            // handled in batch pass below
                        }
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
            font.draw(batch, "Project: ${project.name} | Sprites: ${project.sprites.size}",
                20f, VIRTUAL_HEIGHT - 24f)
            font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 20f, 32f)
            batch.end()
        } else {
            batch.begin()
            font.draw(batch, "NeoCatroid Desktop Player (LWJGL3)", 32f, VIRTUAL_HEIGHT - 48f)
            font.draw(batch, "No project loaded. Pass project path as argument.", 32f, VIRTUAL_HEIGHT - 84f)
            font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 20f, 32f)
            batch.end()
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    override fun dispose() {
        // Stop services
        AudioServiceHolder.audioService?.stopAllSounds()
        AudioServiceHolder.audioService?.clear()
        MidiServiceHolder.midiService?.stopAllSounds()
        batch.dispose()
        shapeRenderer.dispose()
        font.dispose()
        physicsWorld.dispose()
        DesktopProjectManager.getInstance().clear()
    }

    companion object {
        private const val TAG = "DesktopStageListener"
    }
}
