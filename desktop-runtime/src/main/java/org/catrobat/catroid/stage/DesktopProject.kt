package org.catrobat.catroid.stage

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite

class DesktopLook(
    val name: String,
    val fileName: String,
    texture: Texture? = null,
    val hitboxes: MutableList<DesktopHitbox> = mutableListOf()
) {
    private var triedLoad = false
    var texture: Texture? = texture
        get() {
            if (field == null && !triedLoad && fileName.isNotEmpty()) {
                triedLoad = true
                field = DesktopProjectManager.getInstance().loadTextureLazy(fileName)
            }
            return field
        }
}

data class DesktopHitbox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f
)

data class TextOverlay(
    val name: String,
    var text: String,
    var x: Float,
    var y: Float,
    var size: Float = 14f,
    var colorRed: Float = 0f,
    var colorGreen: Float = 0f,
    var colorBlue: Float = 0f,
    var visible: Boolean = true,
    var remainingSeconds: Float = -1f,
    val isThink: Boolean = false,
    val isVariable: Boolean = false,
    var rotation: Float = 0f
)

sealed interface PenDrawCommand {
    data class DrawLine(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val colorRed: Float, val colorGreen: Float, val colorBlue: Float,
        val size: Float
    ) : PenDrawCommand

    data class DrawCircle(
        val cx: Float, val cy: Float, val radius: Float,
        val colorRed: Float, val colorGreen: Float, val colorBlue: Float,
        val size: Float,
        val fill: Boolean
    ) : PenDrawCommand

    data class DrawRect(
        val x: Float, val y: Float,
        val width: Float, val height: Float,
        val colorRed: Float, val colorGreen: Float, val colorBlue: Float,
        val size: Float,
        val fill: Boolean
    ) : PenDrawCommand

    data class DrawText(
        val x: Float, val y: Float,
        val text: String,
        val colorRed: Float, val colorGreen: Float, val colorBlue: Float,
        val size: Float
    ) : PenDrawCommand

    data class FillPolygon(
        val points: List<Pair<Float, Float>>,
        val colorRed: Float, val colorGreen: Float, val colorBlue: Float
    ) : PenDrawCommand

    data class StampSprite(
        val texture: Texture?,
        val x: Float, val y: Float,
        val width: Float, val height: Float
    ) : PenDrawCommand
}

class DesktopSprite(
    var name: String,
    var x: Float = 0f,
    var y: Float = 0f,
    var direction: Float = 90f,
    var size: Float = 100f,
    val looks: MutableList<DesktopLook> = mutableListOf(),
    var currentLookIndex: Int = 0,
    var visible: Boolean = true,
    var transparency: Float = 0f,
    var brightness: Float = 100f,
    var color: Float = 0f,
    var filterBlur: Float = 0f,
    var filterPixelate: Float = 0f,
    var filterSepia: Float = 0f,
    var fontName: String = "",
    var fontSize: Float = 14f,
    var objectColorRed: Float = 1f,
    var objectColorGreen: Float = 1f,
    var objectColorBlue: Float = 1f,
    var objectTexturePath: String = "",
    var objectShaderVertex: String = "",
    var objectShaderFragment: String = "",
    val objectShaderUniforms: MutableMap<String, Triple<Float, Float, Float>> = mutableMapOf(),
    var rotationLockX: Boolean = false,
    var rotationLockY: Boolean = false,
    var rotationLockZ: Boolean = false,
    var canvasName: String = "",
    var width: Float = -1f,
    var height: Float = -1f,
    var penDown: Boolean = false,
    var penSize: Float = 1f,
    var penColorRed: Float = 0f,
    var penColorGreen: Float = 0f,
    var penColorBlue: Float = 0f,
    val penDrawCommands: MutableList<PenDrawCommand> = mutableListOf(),
    var penCornerRadius: Float = 0f,
    var penBorderWidth: Float = 1f,
    var penBorderColorRed: Float = 0f,
    var penBorderColorGreen: Float = 0f,
    var penBorderColorBlue: Float = 0f,
    var rotationStyle: Int = 0,
    var cloneIndex: Int = 0,
    var parentName: String? = null,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var zIndex: Int = 0
) {
    var sprite: Sprite? = null
        private set

    fun resetSprite() {
        sprite = null
    }

    fun currentLook(): DesktopLook? = looks.getOrNull(currentLookIndex)

    fun copy(): DesktopSprite {
        val clone = DesktopSprite(
            name = name + "_clone",
            x = x, y = y, direction = direction, size = size,
            looks = looks.toMutableList(),
            currentLookIndex = currentLookIndex,
            visible = visible, transparency = transparency,
            brightness = brightness, color = color,
            filterBlur = filterBlur, filterPixelate = filterPixelate, filterSepia = filterSepia,
            fontName = fontName, fontSize = fontSize,
            objectColorRed = objectColorRed, objectColorGreen = objectColorGreen, objectColorBlue = objectColorBlue,
            objectTexturePath = objectTexturePath, objectShaderVertex = objectShaderVertex, objectShaderFragment = objectShaderFragment,
            objectShaderUniforms = objectShaderUniforms.toMutableMap(),
            rotationLockX = rotationLockX, rotationLockY = rotationLockY, rotationLockZ = rotationLockZ,
            canvasName = canvasName,
            width = width, height = height,
            penDown = penDown, penSize = penSize,
            penColorRed = penColorRed, penColorGreen = penColorGreen, penColorBlue = penColorBlue,
            penDrawCommands = penDrawCommands.toMutableList(),
            penCornerRadius = penCornerRadius, penBorderWidth = penBorderWidth,
            penBorderColorRed = penBorderColorRed, penBorderColorGreen = penBorderColorGreen,
            penBorderColorBlue = penBorderColorBlue, rotationStyle = rotationStyle,
            cloneIndex = 0, parentName = null, scaleX = scaleX, scaleY = scaleY, zIndex = zIndex
        )
        clone.sprite = null
        return clone
    }

    val lookWidth: Float get() = currentLook()?.texture?.width?.toFloat() ?: 0f
    val lookHeight: Float get() = currentLook()?.texture?.height?.toFloat() ?: 0f

    fun buildSprite() {
        val tex = currentLook()?.texture ?: return
        if (sprite == null) {
            sprite = Sprite(tex)
        } else {
            sprite!!.texture = tex
        }
        sprite!!.setSize(
            tex.width * size / 100f * scaleX,
            tex.height * size / 100f * scaleY
        )
        sprite!!.setOriginCenter()
    }

    fun draw(batch: com.badlogic.gdx.graphics.g2d.SpriteBatch) {
        if (sprite == null) buildSprite()
        sprite?.draw(batch)
    }
}

class DesktopProject(
    var name: String,
    val sprites: MutableList<DesktopSprite> = mutableListOf(),
    var projectDir: java.io.File? = null,
    var imagesDir: java.io.File? = null,
    var soundsDir: java.io.File? = null,
    var stageWidth: Int = 480,
    var stageHeight: Int = 720,
    var activeSceneName: String? = null,
    val sceneNames: MutableList<String> = mutableListOf(),
    var hasGlobalScene: Boolean = false,
    var globalSpriteCount: Int = 0
)
