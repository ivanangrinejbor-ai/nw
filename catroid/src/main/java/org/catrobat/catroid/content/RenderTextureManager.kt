package org.catrobat.catroid.content

import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Vector4
import com.badlogic.gdx.utils.ScreenUtils
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.stage.StageActivity

class RenderTexture(val width: Int, val height: Int) {
    var fbo: FrameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
    var camera2D = OrthographicCamera(width.toFloat(), height.toFloat())
    var camera3D = PerspectiveCamera(67f, width.toFloat(), height.toFloat())
    var textureRegion: TextureRegion
    val spritesToRender = mutableListOf<Sprite>()

    var autoUpdate: Boolean = true
    var needsUpdate: Boolean = true

    var render2D: Boolean = true
    var render3D: Boolean = false

    // Post-processing and shader
    var postProcessingEnabled: Boolean = false
    var mipmappingEnabled: Boolean = false
    var customShader: ShaderProgram? = null
    val uniforms: MutableMap<String, Any> = mutableMapOf()

    // Text overlays: name -> text content
    val textOverlays: MutableMap<String, String> = mutableMapOf()
    var textBufferOnly: Boolean = false

    // Cached font for text rendering
    val font: BitmapFont = BitmapFont().apply { data.setScale(0.5f) }
    val glyphLayout: GlyphLayout = GlyphLayout()
    var textX: Float = -1f // -1 = center
    var textY: Float = -1f // -1 = center

    init {
        textureRegion = TextureRegion(fbo.colorBufferTexture).apply { flip(false, true) }
        camera3D.near = 0.1f
        camera3D.far = 2500f
    }

    fun dispose() {
        fbo.dispose()
        spritesToRender.clear()
        customShader?.dispose()
        customShader = null
        font.dispose()
        textOverlays.clear()
        uniforms.clear()
    }
}

object RenderTextureManager {
    val renderTextures = mutableMapOf<String, RenderTexture>()
    var isRenderingToBuffer: Boolean = false
        private set

    private const val GLOBAL_ROTATION_FIX = 90f

    private val tempMatrix = Matrix4()

    fun createRenderTarget(name: String, width: Int, height: Int) {
        val existing = renderTextures[name]
        if (existing != null && existing.width == width && existing.height == height) return

        Gdx.app.postRunnable {
            renderTextures[name]?.dispose()
            renderTextures[name] = RenderTexture(width, height)
        }
    }

    fun addSpriteToTarget(name: String, sprite: Sprite) {
        val target = renderTextures[name] ?: return
        if (!target.spritesToRender.contains(sprite)) {
            target.spritesToRender.add(sprite)
        }
    }

    fun removeSpriteFromTarget(name: String, sprite: Sprite) {
        renderTextures[name]?.spritesToRender?.remove(sprite)
    }

    fun setBufferMode(name: String, r2d: Boolean, r3d: Boolean) {
        renderTextures[name]?.let {
            it.render2D = r2d
            it.render3D = r3d
        }
    }

    fun setTargetCamera2D(name: String, x: Float, y: Float, zoom: Float, rotation: Float) {
        renderTextures[name]?.camera2D?.let {
            it.position.set(x, y, 0f)
            it.zoom = zoom
            it.up.set(0f, 1f, 0f)
            it.direction.set(0f, 0f, -1f)
            it.rotate(rotation + GLOBAL_ROTATION_FIX)
            it.update()
        }
    }

    fun setTargetCamera3D(name: String, x: Float, y: Float, z: Float, yaw: Float, pitch: Float, roll: Float, fov: Float) {
        renderTextures[name]?.camera3D?.let {
            it.position.set(x, y, z)
            it.fieldOfView = fov

            val rotation = com.badlogic.gdx.math.Quaternion().setEulerAngles(yaw, pitch, roll)
            it.direction.set(0f, 0f, -1f)
            rotation.transform(it.direction)
            it.up.set(0f, 1f, 0f)
            rotation.transform(it.up)

            it.update()
        }
    }

    fun setAutoUpdate(name: String, auto: Boolean) {
        renderTextures[name]?.autoUpdate = auto
    }

    fun saveBufferToFile(name: String, fileName: String) {
        Gdx.app.postRunnable {
            val target = renderTextures[name] ?: return@postRunnable

            target.fbo.begin()
            val pixels = ScreenUtils.getFrameBufferPixels(0, 0, target.width, target.height, true)
            target.fbo.end()

            Thread {
                val pixmap = Pixmap(target.width, target.height, Pixmap.Format.RGBA8888)
                val buffer = pixmap.pixels
                buffer.clear()
                buffer.put(pixels)
                buffer.position(0)

                val projectDir = ProjectManager.getInstance().currentProject.filesDir.absolutePath
                val file = Gdx.files.absolute("$projectDir/$fileName")

                PixmapIO.writePNG(file, pixmap)
                pixmap.dispose()
                Log.d("RenderTextureManager", "Сохранен скриншот буфера: ${file.path()}")
            }.start()
        }
    }

    fun getTextureRegion(name: String): TextureRegion? = renderTextures[name]?.textureRegion
    fun getWidth(name: String): Int = renderTextures[name]?.width ?: 0
    fun getHeight(name: String): Int = renderTextures[name]?.height ?: 0

    private val ppVertices = floatArrayOf(
        -1f, -1f, 0f, 0f,
         1f, -1f, 1f, 0f,
         1f,  1f, 1f, 1f,
        -1f,  1f, 0f, 1f
    )
    private val ppIndices = shortArrayOf(0, 1, 2, 2, 3, 0)
    private var ppMesh: Mesh? = null
    private val ppProjMatrix = Matrix4()

    fun renderAllTargets(batch: Batch) {
        if (renderTextures.isEmpty()) return
        isRenderingToBuffer = true

        tempMatrix.set(batch.projectionMatrix)
        val wasDrawing = batch.isDrawing
        if (wasDrawing) batch.end()

        for ((_, target) in renderTextures) {
            if (!target.autoUpdate && !target.needsUpdate) continue

            if (target.render3D) {
                StageActivity.getActiveStageListener()?.threeDManager?.renderSceneForCustomCamera(target.camera3D, target.fbo)
            }

            if (target.render2D) {
                target.fbo.begin()
                if (!target.render3D) {
                    Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
                }

                target.camera2D.update()
                batch.projectionMatrix = target.camera2D.combined
                batch.begin()

                if (!target.textBufferOnly) {
                    val prevShader = batch.shader
                    if (target.customShader != null && target.customShader!!.isCompiled) {
                        batch.shader = target.customShader
                        applyUniforms(target.customShader!!, target.uniforms)
                    }
                    for (i in 0 until target.spritesToRender.size) {
                        target.spritesToRender[i].look?.draw(batch, 1.0f)
                    }
                    batch.shader = prevShader
                }

                // Draw text overlays
                if (target.textOverlays.isNotEmpty()) {
                    for ((_, textContent) in target.textOverlays) {
                        if (textContent.isNotEmpty()) {
                            target.glyphLayout.setText(target.font, textContent)
                            val txtW = target.glyphLayout.width
                            val txtH = target.glyphLayout.height
                            val txtX = (target.width - txtW) / 2f
                            val txtY = (target.height + txtH) / 2f
                            target.font.draw(batch, textContent, txtX, txtY)
                        }
                    }
                }

                batch.end()
                target.fbo.end()

                // Post-processing pass
                if (target.postProcessingEnabled && target.customShader != null && target.customShader!!.isCompiled) {
                    runPostProcess(target)
                }

                // Mipmapping generation (one-time)
                if (target.mipmappingEnabled) {
                    target.fbo.colorBufferTexture.bind()
                    Gdx.gl.glGenerateMipmap(GL20.GL_TEXTURE_2D)
                }
            }
            target.needsUpdate = false
        }

        batch.projectionMatrix = tempMatrix
        if (wasDrawing) batch.begin()
        isRenderingToBuffer = false
    }

    private fun applyUniforms(shader: ShaderProgram, uniforms: Map<String, Any>) {
        shader.begin()
        for ((name, value) in uniforms) {
            when (value) {
                is Float -> shader.setUniformf(name, value)
                is Int -> shader.setUniformi(name, value)
                is Vector2 -> shader.setUniformf(name, value)
                is Vector3 -> shader.setUniformf(name, value)
                is Vector4 -> shader.setUniformf(name, value)
                else -> Log.w("RenderTextureManager", "Unsupported uniform type for $name: ${value::class.simpleName}")
            }
        }
        shader.end()
    }

    private fun runPostProcess(target: RenderTexture) {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)
        Gdx.gl.glViewport(0, 0, target.width, target.height)

        if (ppMesh == null) {
            ppMesh = Mesh(true, 4, 6,
                VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"))
            ppMesh!!.setVertices(ppVertices)
            ppMesh!!.setIndices(ppIndices)
        }

        target.fbo.begin()
        val shader = target.customShader!!
        shader.begin()
        shader.setUniformMatrix("u_projTrans", ppProjMatrix)
        shader.setUniformi("u_texture", 0)
        applyUniforms(shader, target.uniforms)
        shader.end()

        target.fbo.colorBufferTexture.bind(0)
        ppMesh!!.render(shader, GL20.GL_TRIANGLES)
        target.fbo.end()

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
    }

    var isMain2DRenderEnabled: Boolean = true
    var isMainFast2DRenderEnabled: Boolean = true
    var isMain3DRenderEnabled: Boolean = true

    fun addVariableTextToTarget(bufferName: String, text: String) {
        val target = renderTextures[bufferName] ?: return
        // Store text under a default key; subsequent calls with same bufferName update the text
        target.textOverlays["_text"] = text
    }

    fun setBufferPostProcessing(bufferName: String, enabled: Boolean) {
        val target = renderTextures[bufferName]
        if (target != null) {
            target.postProcessingEnabled = enabled
        } else {
            Log.w("RenderTextureManager", "setBufferPostProcessing: buffer not found: $bufferName")
        }
    }

    fun setBufferMipmapping(bufferName: String, enabled: Boolean) {
        val target = renderTextures[bufferName] ?: return
        target.mipmappingEnabled = enabled
        val texture = target.fbo.colorBufferTexture
        if (enabled) {
            texture.setFilter(
                Texture.TextureFilter.MipMapLinearLinear,
                Texture.TextureFilter.Linear
            )
            texture.bind()
            Gdx.gl.glGenerateMipmap(GL20.GL_TEXTURE_2D)
        } else {
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }

    fun setBufferShader(bufferName: String, vsh: String?, fsh: String?) {
        val target = renderTextures[bufferName]
        if (target == null) {
            Log.w("RenderTextureManager", "setBufferShader: buffer not found: $bufferName")
            return
        }
        target.customShader?.dispose()
        target.customShader = null
        if (vsh.isNullOrBlank() || fsh.isNullOrBlank()) {
            return
        }
        val program = ShaderProgram(vsh, fsh)
        if (!program.isCompiled) {
            Log.w("RenderTextureManager", "setBufferShader: compilation failed: $bufferName\n${program.log}")
            program.dispose()
            return
        }
        ShaderProgram.pedantic = false
        target.customShader = program
    }

    fun setBufferShaderUniform(bufferName: String, uniformName: String, value: Any) {
        val target = renderTextures[bufferName] ?: return
        target.uniforms[uniformName] = value
    }

    fun setTextBufferOnly(textName: String, only: Boolean) {
        val target = renderTextures[textName]
        if (target != null) {
            target.textBufferOnly = only
        } else {
            Log.w("RenderTextureManager", "setTextBufferOnly: buffer not found: $textName")
        }
    }

    fun clearAll() {
        renderTextures.values.forEach { it.dispose() }
        renderTextures.clear()
    }
}
