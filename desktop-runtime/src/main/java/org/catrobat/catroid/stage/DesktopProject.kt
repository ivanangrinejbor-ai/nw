package org.catrobat.catroid.stage

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite

/**
 * Десктопные data-классы для представления проекта NeoCatroid.
 *
 * Минимальная, но функциональная модель: проект содержит спрайты,
 * каждый спрайт имеет позицию, размер, направление и набор "луков"
 * (текстур). Загрузка выполняется [DesktopProjectManager]'ом из
 * директории проекта (формат Catrobat: code.xml + images/).
 */
data class DesktopLook(
    val name: String,
    val fileName: String,
    var texture: Texture? = null
)

/** Text overlay on the stage — rendered each frame. */
data class TextOverlay(
    val name: String,
    var text: String,
    var x: Float,    // Catrobat coordinate (centre of screen = 0,0)
    var y: Float,
    var size: Float = 14f,
    var colorRed: Float = 0f,
    var colorGreen: Float = 0f,
    var colorBlue: Float = 0f,
    var visible: Boolean = true,
    /** For think/say bubbles: remaining visible time in seconds, -1 = permanent */
    var remainingSeconds: Float = -1f,
    /** true = "Think" (thought bubble prefix), false = "Say" (speech bubble prefix) */
    val isThink: Boolean = false,
    /** true = this overlay displays a variable value that should update each frame */
    val isVariable: Boolean = false,
    /** Rotation of the overlay text in degrees (ShowTextRotationBrick). */
    var rotation: Float = 0f
)

/**
 * Команда рисования пера — хранится в [DesktopSprite.penDrawCommands]
 * и рендерится каждый кадр до очистки.
 */
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
    // ── Graphic effects ──
    var transparency: Float = 0f,       // 0..100
    var brightness: Float = 100f,       // 0..200
    var color: Float = 0f,              // 0..200
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
    // ── Dimensions (override) ──
    var width: Float = -1f,             // -1 = use look native
    var height: Float = -1f,
    // ── Pen ──
    var penDown: Boolean = false,
    var penSize: Float = 1f,
    var penColorRed: Float = 0f,
    var penColorGreen: Float = 0f,
    var penColorBlue: Float = 0f,
    // ── Pen drawing commands (shapes) ──
    val penDrawCommands: MutableList<PenDrawCommand> = mutableListOf(),
    var penCornerRadius: Float = 0f,
    var penBorderWidth: Float = 1f,
    var penBorderColorRed: Float = 0f,
    var penBorderColorGreen: Float = 0f,
    var penBorderColorBlue: Float = 0f,
    // ── Rotation ──
    var rotationStyle: Int = 0,         // 0=free, 1=mirror, 2=no_rotation
    // ── Clone / parenting ──
    var cloneIndex: Int = 0,            // 0 = original, 1+ = clone number
    var parentName: String? = null,     // имя родительского спрайта (SetParent)
    // ── Fast2D scale / z-order ──
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

    /** Creates a shallow copy of this sprite for cloning. */
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

    /** Строит libGDX Sprite из текущего look (текстуры). */
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
    /** Resolved directory holding look images (may be nested under a project-name folder). */
    var imagesDir: java.io.File? = null,
    /** Resolved directory holding sound files (may be nested under a project-name folder). */
    var soundsDir: java.io.File? = null,
    var stageWidth: Int = 480,
    var stageHeight: Int = 720
)
