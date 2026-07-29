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

class DesktopStageListener(private val projectDir: File? = null) : ApplicationAdapter() {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var physicsWorld: DesktopPhysicsWorld
    private lateinit var scriptEngine: DesktopScriptEngine
    private lateinit var scriptRunner: DesktopScriptRunner
    private lateinit var input: DesktopInput
    private lateinit var cameraState: DesktopCameraState
    private var lastFrameNanos: Long = 0L

    private var penFbo: FrameBuffer? = null
    private var penFboSprite: com.badlogic.gdx.graphics.g2d.Sprite? = null
    private val penRenderedCounts = mutableMapOf<String, Int>()

    private var VIRTUAL_WIDTH: Float = 1280f
    private var VIRTUAL_HEIGHT: Float = 720f
    private var frameCount: Int = 0

    override fun create() {
        Gdx.app.log(TAG, "DesktopStageListener created")

        projectDir?.let { DesktopProjectManager.getInstance().loadProject(it) }
        DesktopProjectManager.getInstance().getCurrentProject()?.let { proj ->
            // Use the project's declared stage size as the world coordinate system. Sprites are
            // positioned in this space; FitViewport then scales the world to fit the window.
            VIRTUAL_WIDTH = proj.stageWidth.toFloat().coerceAtLeast(1f)
            VIRTUAL_HEIGHT = proj.stageHeight.toFloat().coerceAtLeast(1f)
            Gdx.app.log(TAG, "Using project viewport ${VIRTUAL_WIDTH.toInt()}x${VIRTUAL_HEIGHT.toInt()}")
        }

        camera = OrthographicCamera()
        viewport = FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)

        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        font = BitmapFont()

        try {
            penFbo = FrameBuffer(Pixmap.Format.RGBA8888, VIRTUAL_WIDTH.toInt(), VIRTUAL_HEIGHT.toInt(), false)
            penFbo?.let { fbo ->
                fbo.begin()
                ScreenUtils.clear(0f, 0f, 0f, 0f)
                fbo.end()
            }
            penFboSprite = com.badlogic.gdx.graphics.g2d.Sprite(penFbo!!.colorBufferTexture)
            penFboSprite?.setFlip(false, true)
        } catch (_: Exception) {
            penFbo = null
        }

        input = DesktopInput()
        physicsWorld = DesktopPhysicsWorld(0f, -9.8f)
        cameraState = DesktopCameraState()
        val project = DesktopProjectManager.getInstance().getCurrentProject()
        scriptEngine = project?.let { DesktopScriptEngine(it, physicsWorld, input, cameraState) } ?: DesktopScriptEngine(DesktopProject("empty"), physicsWorld, input, cameraState)
        scriptRunner = project?.let { DesktopScriptRunner(it, input) } ?: DesktopScriptRunner(DesktopProject("empty"), input)
        lastFrameNanos = System.nanoTime()

        scriptEngine.start()

        Gdx.app.log(TAG, "Services: audio=${AudioServiceHolder.audioService != null}, " +
                "midi=${MidiServiceHolder.midiService != null}, " +
                "text=${TextServiceHolder.textService != null}, " +
                "notifications=${NotificationServiceHolder.service != null}")

        DesktopProjectManager.getInstance().getCurrentProject()
            ?: run { Gdx.app.log(TAG, "No project loaded — running empty stage") }
    }

    private var spriteDiagLogged = false
    override fun render() {
        input.update()

        // Stage background: black when no background look covers the stage (matches the
        // editor's default and fills the letterbox area outside the FitViewport).
        ScreenUtils.clear(0f, 0f, 0f, 1f)

        camera.direction.set(0f, 0f, -1f)
        camera.up.set(0f, 1f, 0f)
        // Catroid Y-up: camera sits at world (VW/2 + camX, VH/2 + camY), matching the sprite
        // render (screenY = VH/2 + y). A sprite at Catroid y=-N therefore appears BELOW centre.
        camera.position.set(VIRTUAL_WIDTH / 2f + cameraState.x, VIRTUAL_HEIGHT / 2f + cameraState.y, 0f)
        val projectForCamera = DesktopProjectManager.getInstance().getCurrentProject()
        cameraState.followTargetName?.let { targetName ->
            val target = projectForCamera?.sprites?.find { it.name == targetName }
            if (target != null) {
                camera.position.set(
                    VIRTUAL_WIDTH / 2f + target.x + cameraState.followOffsetX,
                    VIRTUAL_HEIGHT / 2f + (target.y + cameraState.followOffsetY),
                    0f
                )
            }
        }
        camera.zoom = cameraState.zoom
        if (cameraState.rotation != 0f) {
            camera.rotate(com.badlogic.gdx.math.Vector3(0f, 0f, 1f), cameraState.rotation)
        }
        camera.update()
        viewport.apply()
        batch.projectionMatrix = camera.combined

        // Map the window cursor to STAGE coords via the FitViewport so touch/hit-tests use the
        // same space as sprite.x/y. Catroid is Y-up (top = +Y); viewport.unproject already returns
        // world coords, so fingerY = wc.y - VH/2 (matching render screenY = VH/2 + y).
        run {
            val wc = viewport.unproject(com.badlogic.gdx.math.Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
            input.setStageFinger(wc.x - VIRTUAL_WIDTH / 2f, wc.y - VIRTUAL_HEIGHT / 2f)
        }

        val project = DesktopProjectManager.getInstance().getCurrentProject()
        if (project != null) {
            try {
                scriptEngine.update(Gdx.graphics.deltaTime)
                scriptRunner.update(Gdx.graphics.deltaTime)
                physicsWorld.step(Gdx.graphics.deltaTime)

                batch.begin()
                var drawnCount = 0
                for (sprite in project.sprites) {
                    if (!sprite.visible) continue
                    if (sprite.sprite == null) sprite.buildSprite()
                    val sp = sprite.sprite ?: continue
                    cameraState.cameraPinned[sprite.name]?.let { (sx, sy) ->
                        sprite.x = cameraState.x + sx / cameraState.zoom
                        sprite.y = cameraState.y + sy / cameraState.zoom
                    }
                    val screenX = VIRTUAL_WIDTH / 2f + sprite.x
                    val screenY = VIRTUAL_HEIGHT / 2f + sprite.y
                    sp.setPosition(
                        screenX - sp.width / 2f,
                        screenY - sp.height / 2f
                    )
                    sp.setColor(
                        sprite.objectColorRed.coerceIn(0f, 1f),
                        sprite.objectColorGreen.coerceIn(0f, 1f),
                        sprite.objectColorBlue.coerceIn(0f, 1f),
                        (1f - sprite.transparency / 100f).coerceIn(0f, 1f)
                    )
                    sp.draw(batch)
                    drawnCount++
                }
                batch.end()

                frameCount++
                if (frameCount % 60 == 0) {
                    project.sprites.forEach { s ->
                        if (s.transparency != 0f) Gdx.app.log("FadeDiag", "f=$frameCount '${s.name}' transp=${s.transparency} vis=${s.visible}")
                    }
                }
                if (!spriteDiagLogged) {
                    spriteDiagLogged = true
                    Gdx.app.log("Render", "frame=$frameCount VIRTUAL=${VIRTUAL_WIDTH}x${VIRTUAL_HEIGHT} viewport=${camera.viewportWidth}x${camera.viewportHeight} camPos=${camera.position.x},${camera.position.y} camZoom=${camera.zoom} drawn=$drawnCount/${project.sprites.size}")
                    for (s in project.sprites) {
                        Gdx.app.log("Render", "  '${s.name}' vis=${s.visible} built=${s.sprite != null} x=${s.x} y=${s.y} sz=${s.size} transp=${s.transparency} bright=${s.brightness} color=${s.color} objColor=${s.objectColorRed},${s.objectColorGreen},${s.objectColorBlue} w=${s.sprite?.width} h=${s.sprite?.height}")
                    }
                }

                // Diagnostic: skip pen FBO overlay to rule out it covering the scene.
                // val fbo = penFbo
                // ...

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
                            else -> { }
                        }
                    }
                }

                val VIRTUAL_WIDTH_F = VIRTUAL_WIDTH
                val VIRTUAL_HEIGHT_F = VIRTUAL_HEIGHT
                for (overlay in scriptEngine.textOverlays.values) {
                    if (!overlay.visible) continue
                    val sx = VIRTUAL_WIDTH_F / 2f + overlay.x
                    val sy = VIRTUAL_HEIGHT_F / 2f + overlay.y
                    val prefix = if (overlay.isThink) "🤔 " else "💬 "
                    font.draw(batch, "$prefix${overlay.text}", sx, sy)
                }

                // FPS counter in the top-left corner (project/sprites HUD label removed).
                font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 20f, VIRTUAL_HEIGHT - 10f)
                batch.end()
            } catch (e: Exception) {
                Gdx.app.error("Render", "Exception during render", e)
                e.printStackTrace()
                if (batch.isDrawing) batch.end()
            }
        } else {
            batch.begin()
            font.draw(batch, "NeoCatroid Desktop Player (LWJGL3)", 32f, VIRTUAL_HEIGHT - 48f)
            font.draw(batch, "No project loaded. Pass project path as argument.", 32f, VIRTUAL_HEIGHT - 84f)
            font.draw(batch, "FPS: ${Gdx.graphics.framesPerSecond}", 20f, 32f)
            batch.end()
        }

        val requestedFps = scriptEngine.getTargetFps()
        if (requestedFps > 0 && requestedFps != 60) {
        }
        lastFrameNanos = System.nanoTime()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun dispose() {
        AudioServiceHolder.audioService?.stopAllSounds()
        AudioServiceHolder.audioService?.clear()
        MidiServiceHolder.midiService?.stopAllSounds()
        batch.dispose()
        shapeRenderer.dispose()
        font.dispose()
        penFbo?.dispose()
        penFbo = null
        physicsWorld.dispose()
        DesktopProjectManager.getInstance().clear()
    }

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
            is PenDrawCommand.StampSprite -> { }
        }
    }

    companion object {
        private const val TAG = "DesktopStageListener"
    }
}
