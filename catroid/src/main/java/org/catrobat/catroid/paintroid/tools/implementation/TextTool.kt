/*
 * Paintroid: An image manipulation application for Android.
 *  Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.paintroid.tools.implementation

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.test.espresso.idling.CountingIdlingResource
import org.catrobat.catroid.R

import org.catrobat.catroid.paintroid.command.CommandManager
import org.catrobat.catroid.paintroid.command.serialization.SerializableTypeface
import org.catrobat.catroid.paintroid.common.ITALIC_FONT_BOX_ADJUSTMENT
import org.catrobat.catroid.paintroid.tools.ContextCallback
import org.catrobat.catroid.paintroid.tools.FontEntry
import org.catrobat.catroid.paintroid.tools.FontType
import org.catrobat.catroid.paintroid.tools.ImportedFont
import org.catrobat.catroid.paintroid.tools.ImportedFontRegistry
import org.catrobat.catroid.paintroid.tools.ToolPaint
import org.catrobat.catroid.paintroid.tools.ToolType
import org.catrobat.catroid.paintroid.tools.Workspace
import org.catrobat.catroid.paintroid.tools.options.TextToolOptionsView
import org.catrobat.catroid.paintroid.tools.options.ToolOptionsViewController
import kotlin.Exception
import kotlin.math.abs

@VisibleForTesting
const val TEXT_SIZE_MAGNIFICATION_FACTOR = 3f

@VisibleForTesting
const val BOX_OFFSET = 20

@VisibleForTesting
const val MARGIN_TOP = 200f

private const val ROTATION_ENABLED = true
private const val RESIZE_POINTS_VISIBLE = true
private const val ITALIC_TEXT_SKEW = -0.25f
private const val DEFAULT_TEXT_SKEW = 0.0f
private const val DEFAULT_TEXT_SIZE = 20
private const val BUNDLE_TOOL_UNDERLINED = "BUNDLE_TOOL_UNDERLINED"
private const val BUNDLE_TOOL_ITALIC = "BUNDLE_TOOL_ITALIC"
private const val BUNDLE_TOOL_BOLD = "BUNDLE_TOOL_BOLD"
private const val BUNDLE_TOOL_TEXT = "BUNDLE_TOOL_TEXT"
private const val BUNDLE_TOOL_TEXT_SIZE = "BUNDLE_TOOL_TEXT_SIZE"
private const val BUNDLE_TOOL_FONT = "BUNDLE_TOOL_FONT"
private const val BUNDLE_TOOL_IMPORTED_FONT = "BUNDLE_TOOL_IMPORTED_FONT"
private const val TAG = "Can't set custom font"

class TextTool(
    private val textToolOptionsView: TextToolOptionsView,
    contextCallback: ContextCallback,
    toolOptionsViewController: ToolOptionsViewController,
    toolPaint: ToolPaint,
    workspace: Workspace,
    idlingResource: CountingIdlingResource,
    commandManager: CommandManager,
    override var drawTime: Long
) : BaseToolWithRectangleShape(
    contextCallback,
    toolOptionsViewController,
    toolPaint,
    workspace,
    idlingResource,
    commandManager
), BaseToolWithRectangleShape.ShapeSizeChangedListener {
    @VisibleForTesting
    @JvmField
    val textPaint: Paint

    @VisibleForTesting
    @JvmField
    var text = ""

    @VisibleForTesting
    @JvmField
    var font = FontType.SANS_SERIF

    private var importedFontName: String? = null

    @VisibleForTesting
    @JvmField
    var underlined = false

    @VisibleForTesting
    @JvmField
    var italic = false

    @VisibleForTesting
    @JvmField
    var bold = false

    private var textSize = DEFAULT_TEXT_SIZE
    private val stc: Typeface?
    private val dubai: Typeface?
    private var projectFontTypeface: Typeface? = null
    private var oldBoxWidth = 0f
    private var oldBoxHeight = 0f
    private var oldToolPosition: PointF? = null

    @get:VisibleForTesting
    val multilineText: Array<String>
        get() = text.split("\n").toTypedArray()

    override val toolType: ToolType
        get() = ToolType.TEXT

    override fun handleUpAnimations(coordinate: PointF?) {
        showTextToolLayout()
        toolOptionsViewController.animateBottomAndTopNavigation(false)
    }

    override fun handleDownAnimations(coordinate: PointF?) {
        hideTextToolLayout()
    }

    override fun toolPositionCoordinates(coordinate: PointF): PointF = coordinate

    init {
        rotationEnabled = ROTATION_ENABLED
        resizePointsVisible = RESIZE_POINTS_VISIBLE
        stc = contextCallback.getFont(R.font.stc_regular)
        dubai = contextCallback.getFont(R.font.dubai)
        setShapeSizeChangedListener(this)
        textPaint = Paint()
        initializePaint()
        resetPreview()
        resetBoxPosition()

        val callback: TextToolOptionsView.Callback = object : TextToolOptionsView.Callback {
            override fun setText(text: String) {
                this@TextTool.text = text
                resetPreview()
                workspace.invalidate()
            }

            override fun setFont(fontEntry: FontEntry, typeface: Typeface?) {
                when (fontEntry) {
                    is FontEntry.BuiltIn -> {
                        if (fontEntry.fontType === font && importedFontName == null) return
                        this@TextTool.font = fontEntry.fontType
                        importedFontName = null
                        projectFontTypeface = null
                        updateTypeface()
                    }
                    is FontEntry.Imported -> {
                        this@TextTool.font = FontType.PROJECT_FONT
                        importedFontName = fontEntry.font.name
                        if (typeface != null) {
                            projectFontTypeface = typeface
                        }
                        updateTypeface()
                    }
                }
                storeAttributes()
                resetPreview()
                workspace.invalidate()
                applyAttributes()
            }

            override fun setUnderlined(underlined: Boolean) {
                this@TextTool.underlined = underlined
                textPaint.isUnderlineText = this@TextTool.underlined
                storeAttributes()
                resetPreview()
                workspace.invalidate()
                applyAttributes()
            }

            override fun setItalic(italic: Boolean) {
                this@TextTool.italic = italic
                if (italic) {
                    storeAttributes(italic)
                } else {
                    storeAttributes()
                }
                updateTypeface()
                resetPreview()
                workspace.invalidate()
                if (italic) {
                    applyAttributes(italic)
                } else {
                    applyAttributes()
                }
            }

            override fun setBold(bold: Boolean) {
                this@TextTool.bold = bold
                storeAttributes()
                textPaint.isFakeBoldText = this@TextTool.bold
                resetPreview()
                workspace.invalidate()
                applyAttributes()
            }

            override fun setTextSize(size: Int) {
                textSize = size
                textPaint.textSize = textSize * TEXT_SIZE_MAGNIFICATION_FACTOR
                resetPreview()
                workspace.invalidate()
            }

            override fun hideToolOptions() {
                this@TextTool.toolOptionsViewController.hide()
            }
        }
        textToolOptionsView.setCallback(callback)
        toolOptionsViewController.showDelayed()
    }

    private fun initializePaint() {
        textPaint.isAntiAlias = DEFAULT_ANTIALIASING_ON
        textPaint.color = toolPaint.previewColor
        textPaint.textSize = DEFAULT_TEXT_SIZE * TEXT_SIZE_MAGNIFICATION_FACTOR
        textPaint.isUnderlineText = underlined
        textPaint.isFakeBoldText = bold
        updateTypeface()
    }

    fun hideTextToolLayout() {
        if (textToolOptionsView.getTopLayout().visibility == View.VISIBLE) {
            toolOptionsViewController.slideUp(
                textToolOptionsView.getTopLayout(),
                willHide = true,
                showOptionsView = false
            )
        }

        if (textToolOptionsView.getBottomLayout().visibility == View.VISIBLE) {
            toolOptionsViewController.slideDown(
                textToolOptionsView.getBottomLayout(),
                willHide = true,
                showOptionsView = false
            )
        }
    }

     fun showTextToolLayout() {
         if (textToolOptionsView.getTopLayout().visibility == View.INVISIBLE) {
             if (!toolOptionsViewController.isVisible) {
                 toolOptionsViewController.show()
             }
             toolOptionsViewController.slideDown(
                 textToolOptionsView.getTopLayout(),
                 willHide = false,
                 showOptionsView = true
             )
         }

         if (textToolOptionsView.getBottomLayout().visibility == View.INVISIBLE) {
             if (!toolOptionsViewController.isVisible) {
                 toolOptionsViewController.show()
             }
             toolOptionsViewController.slideUp(
                 textToolOptionsView.getBottomLayout(),
                 willHide = false,
                 showOptionsView = true
             )
         }
    }

    override fun handleMove(coordinate: PointF?, shouldAnimate: Boolean): Boolean {
        textToolOptionsView.hideKeyboard()
        hideTextToolLayout()
        return super.handleMove(coordinate, false)
    }

    override fun handleUp(coordinate: PointF?): Boolean {
        coordinate?.let {
            if (abs(toolPosition.x - it.x) <= boxWidth / 2 && abs(toolPosition.y - it.y) <= boxHeight / 2) {
                showTextToolLayout()
                super.handleUp(coordinate)
                textToolOptionsView.showKeyboard()
            } else {
                textToolOptionsView.hideKeyboard()
                hideTextToolLayout()
                super.handleUp(coordinate)
            }
        }
        return true
    }

    override fun drawBitmap(canvas: Canvas, boxWidth: Float, boxHeight: Float) {
        val textAscent = textPaint.ascent()
        val textDescent = textPaint.descent()
        val textHeight = (textDescent - textAscent) * multilineText.size
        val lineHeight = textHeight / multilineText.size
        var maxTextWidth = multilineText.maxOf { line ->
            textPaint.measureText(line)
        }

        if (italic) {
            maxTextWidth *= ITALIC_FONT_BOX_ADJUSTMENT
        }

        canvas.save()

        val widthScaling = if (maxTextWidth > 0) (boxWidth - 2 * BOX_OFFSET) / maxTextWidth else 1f
        val heightScaling = if (textHeight > 0) (boxHeight - 2 * BOX_OFFSET) / textHeight else 1f

        canvas.scale(widthScaling, heightScaling)

        val scaledHeightOffset = BOX_OFFSET / heightScaling
        val scaledWidthOffset = BOX_OFFSET / widthScaling
        val scaledBoxWidth = boxWidth / widthScaling
        val scaledBoxHeight = boxHeight / heightScaling

        multilineText.forEachIndexed { index, textLine ->
            canvas.drawText(
                textLine,
                scaledWidthOffset - scaledBoxWidth / 2 / if (italic) ITALIC_FONT_BOX_ADJUSTMENT else 1f,
                -(scaledBoxHeight / 2) + scaledHeightOffset - textAscent + lineHeight * index,
                textPaint
            )
        }
        canvas.restore()
    }

    private fun resetPreview() {
        val textDescent = textPaint.descent()
        val textAscent = textPaint.ascent()
        val textHeight = textDescent - textAscent

        val maxTextWidth = multilineText.maxOf { line ->
            textPaint.measureText(line)
        }
        boxHeight = textHeight * multilineText.size + 2 * BOX_OFFSET
        boxWidth = maxTextWidth + 2 * BOX_OFFSET
        createAndSetShapeSizeText(boxWidth, boxHeight)
    }

    private fun storeAttributes(italic: Boolean = false) {
        if (italic) {
            boxWidth *= ITALIC_FONT_BOX_ADJUSTMENT
        }
        oldBoxWidth = boxWidth
        oldBoxHeight = boxHeight
        oldToolPosition = PointF(toolPosition.x, toolPosition.y)
    }
    private fun applyAttributes(italic: Boolean = false) {
        boxWidth = oldBoxWidth / if (italic) ITALIC_FONT_BOX_ADJUSTMENT else 1f
        boxHeight = oldBoxHeight
        if (oldToolPosition != null) {
            toolPosition = oldToolPosition as PointF
        } else {
            resetBoxPosition()
        }
        createAndSetShapeSizeText(boxWidth, boxHeight)
    }

    override fun onSaveInstanceState(bundle: Bundle?) {
        super.onSaveInstanceState(bundle)
        bundle?.apply {
            putBoolean(BUNDLE_TOOL_UNDERLINED, underlined)
            putBoolean(BUNDLE_TOOL_ITALIC, italic)
            putBoolean(BUNDLE_TOOL_BOLD, bold)
            putString(BUNDLE_TOOL_TEXT, text)
            putInt(BUNDLE_TOOL_TEXT_SIZE, textSize)
            putString(BUNDLE_TOOL_FONT, font.name)
            putString(BUNDLE_TOOL_IMPORTED_FONT, importedFontName ?: "")
        }
    }

    override fun onRestoreInstanceState(bundle: Bundle?) {
        super.onRestoreInstanceState(bundle)
        bundle?.apply {
            underlined = getBoolean(BUNDLE_TOOL_UNDERLINED, underlined)
            italic = getBoolean(BUNDLE_TOOL_ITALIC, italic)
            bold = getBoolean(BUNDLE_TOOL_BOLD, bold)
            text = getString(BUNDLE_TOOL_TEXT, text)
            textSize = getInt(BUNDLE_TOOL_TEXT_SIZE, textSize)
            font = FontType.valueOf(getString(BUNDLE_TOOL_FONT, font.name))
            importedFontName = getString(BUNDLE_TOOL_IMPORTED_FONT, "")?.ifEmpty { null }
        }
        textToolOptionsView.setState(bold, italic, underlined, text, textSize, currentFontEntry())
        textPaint.isUnderlineText = underlined
        textPaint.isFakeBoldText = bold
        updateTypeface()
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    private fun updateTypeface() {
        val style = if (italic) Typeface.ITALIC else Typeface.NORMAL
        val textSkewX = if (italic) ITALIC_TEXT_SKEW else DEFAULT_TEXT_SKEW
        textPaint.textSkewX = textSkewX
        if (font == FontType.PROJECT_FONT) {
            textPaint.typeface = projectFontTypeface ?: Typeface.create(Typeface.SANS_SERIF, style)
            return
        }
        when (font) {
            FontType.SANS_SERIF -> textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, style)
            FontType.SERIF -> textPaint.typeface = Typeface.create(Typeface.SERIF, style)
            FontType.MONOSPACE -> textPaint.typeface = Typeface.create(Typeface.MONOSPACE, style)
            FontType.STC ->
                try {
                    textPaint.typeface = stc
                } catch (e: Exception) {
                    Log.e(TAG, "stc_regular")
                }
            FontType.DUBAI ->
                try {
                    textPaint.typeface = dubai
                } catch (e: Exception) {
                    Log.e(TAG, "dubai")
                }
            FontType.PROJECT_FONT -> { /* handled above */ }
        }
    }

    private fun currentFontEntry(): FontEntry =
        if (font == FontType.PROJECT_FONT) {
            val name = importedFontName
            if (name != null) FontEntry.Imported(ImportedFont(name, name)) else FontEntry.BuiltIn(font)
        } else {
            FontEntry.BuiltIn(font)
        }

    private fun changeTextColor() {
        val width = boxWidth
        val height = boxHeight
        val position = PointF(toolPosition.x, toolPosition.y)
        textPaint.color = toolPaint.previewColor
        toolPosition.set(position)
        boxWidth = width
        boxHeight = height
        workspace.invalidate()
    }

    override fun resetInternalState() = Unit

    override fun onClickOnButton() {
        highlightBox()
        val toolPosition = PointF(toolPosition.x, toolPosition.y)

        val typeFaceInfo = SerializableTypeface(
            font,
            bold,
            underlined,
            italic,
            textPaint.textSize,
            textPaint.textSkewX,
            importedFontName
        )

        val command = commandFactory.createTextToolCommand(
            multilineText,
            textPaint,
            BOX_OFFSET,
            boxWidth,
            boxHeight,
            toolPosition,
            boxRotation,
            typeFaceInfo
        )
        commandManager.addCommand(command)
        projectFontTypeface = null
    }


    @VisibleForTesting
    fun resetBoxPosition() {
        if (workspace.scale <= 1) {
            toolPosition.x = workspace.width / 2.0f
            toolPosition.y = boxHeight / 2.0f + MARGIN_TOP
        }
    }

    override fun changePaintColor(color: Int, invalidate: Boolean) {
        super.changePaintColor(color, invalidate)
        changeTextColor()
    }

    override fun onShapeSizeChanged(shapeText: String) {
        textToolOptionsView.setShapeSizeText(shapeText)
    }

    override fun onToggleVisibility(isVisible: Boolean) {
        textToolOptionsView.toggleShapeSizeVisibility(isVisible)
    }
}
