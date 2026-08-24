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

package org.catrobat.catroid.paintroid.command.implementation

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Shader
import org.catrobat.catroid.paintroid.command.Command
import org.catrobat.catroid.paintroid.command.serialization.SerializableTypeface
import org.catrobat.catroid.paintroid.common.ITALIC_FONT_BOX_ADJUSTMENT
import org.catrobat.catroid.paintroid.contract.LayerContracts
import org.catrobat.catroid.paintroid.tools.TextToolEffects

class TextToolCommand(
    multilineText: Array<String>,
    textPaint: Paint,
    boxOffset: Float,
    boxWidth: Float,
    boxHeight: Float,
    toolPosition: PointF,
    rotationAngle: Float,
    typeFaceInfo: SerializableTypeface,
    var effects: TextToolEffects? = null
) : Command {

    var multilineText = multilineText.clone(); private set
    var textPaint = textPaint; private set
    var boxOffset = boxOffset; private set
    var boxWidth = boxWidth; private set
    var boxHeight = boxHeight; private set
    var toolPosition = toolPosition; private set
    var rotationAngle = rotationAngle; private set
    var typeFaceInfo = typeFaceInfo; private set

    override fun run(canvas: Canvas, layerModel: LayerContracts.Model) {
        val fx = effects
        if (fx != null && fx.autoDimBackground) {
            val dimPaint = Paint().apply {
                color = android.graphics.Color.argb(80, 0, 0, 0)
                style = Paint.Style.FILL
            }
            val dimRect = android.graphics.RectF(
                -boxWidth / 2f - 10f, -boxHeight / 2f - 10f, boxWidth / 2f + 10f, boxHeight / 2f + 10f
            )
            canvas.save()
            canvas.translate(toolPosition.x, toolPosition.y)
            canvas.rotate(rotationAngle)
            canvas.drawRoundRect(dimRect, 16f, 16f, dimPaint)
            canvas.restore()
        }

        textPaint.isAntiAlias = fx?.pixelCrisp != true

        if (fx != null && fx.useGradient) {
            textPaint.shader = LinearGradient(
                0f, -boxHeight / 2f, 0f, boxHeight / 2f,
                fx.gradientTopColor, fx.gradientBottomColor,
                Shader.TileMode.CLAMP
            )
        } else {
            textPaint.shader = null
        }

        if (fx != null && fx.shadowEnabled && fx.shadowRadius > 0f) {
            textPaint.setShadowLayer(fx.shadowRadius, fx.shadowDx, fx.shadowDy, fx.shadowColor)
        } else {
            textPaint.clearShadowLayer()
        }

        val textAscent = textPaint.ascent()
        val textDescent = textPaint.descent()
        val textHeight = (textDescent - textAscent) * multilineText.size
        val lineHeight = textHeight / multilineText.size
        var maxTextWidth = multilineText.maxOf { line ->
            textPaint.measureText(line)
        }

        if (typeFaceInfo.italic) {
            maxTextWidth *= ITALIC_FONT_BOX_ADJUSTMENT
        }

        with(canvas) {
            save()
            translate(toolPosition.x, toolPosition.y)
            rotate(rotationAngle)

            val widthScaling = if (maxTextWidth > 0) (boxWidth - 2 * boxOffset) / maxTextWidth else 1f
            val heightScaling = if (textHeight > 0) (boxHeight - 2 * boxOffset) / textHeight else 1f
            canvas.scale(widthScaling, heightScaling)

            val scaledHeightOffset = boxOffset / heightScaling
            val scaledWidthOffset = boxOffset / widthScaling
            val scaledBoxWidth = boxWidth / widthScaling
            val scaledBoxHeight = boxHeight / heightScaling

            if (fx != null && fx.glowIntensity > 0) {
                val glowPaint = Paint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = fx.glowIntensity * 4f
                    color = fx.glowColor
                    shader = null
                    maskFilter = BlurMaskFilter(fx.glowIntensity * 3f, BlurMaskFilter.Blur.NORMAL)
                }
                multilineText.forEachIndexed { index, textLine ->
                    canvas.drawText(
                        textLine,
                        scaledWidthOffset - scaledBoxWidth / 2 / if (typeFaceInfo.italic) ITALIC_FONT_BOX_ADJUSTMENT else 1f,
                        -(scaledBoxHeight / 2) + scaledHeightOffset - textAscent + lineHeight * index,
                        glowPaint
                    )
                }
            }

            if (fx != null && fx.strokeWidth > 0f) {
                val strokePaint = Paint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = fx.strokeWidth
                    color = fx.strokeColor
                    shader = null
                }
                multilineText.forEachIndexed { index, textLine ->
                    canvas.drawText(
                        textLine,
                        scaledWidthOffset - scaledBoxWidth / 2 / if (typeFaceInfo.italic) ITALIC_FONT_BOX_ADJUSTMENT else 1f,
                        -(scaledBoxHeight / 2) + scaledHeightOffset - textAscent + lineHeight * index,
                        strokePaint
                    )
                }
            }

            multilineText.forEachIndexed { index, textLine ->
                canvas.drawText(
                    textLine,
                    scaledWidthOffset - scaledBoxWidth / 2 / if (typeFaceInfo.italic) ITALIC_FONT_BOX_ADJUSTMENT else 1f,
                    -(scaledBoxHeight / 2) + scaledHeightOffset - textAscent + lineHeight * index,
                    textPaint
                )
            }
            restore()
        }
    }

    override fun freeResources() {
    }
}
