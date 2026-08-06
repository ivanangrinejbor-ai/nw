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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import org.catrobat.catroid.paintroid.command.Command
import org.catrobat.catroid.paintroid.command.serialization.SerializableTypeface
import org.catrobat.catroid.paintroid.common.ITALIC_FONT_BOX_ADJUSTMENT
import org.catrobat.catroid.paintroid.contract.LayerContracts

class TextToolCommand(
    multilineText: Array<String>,
    textPaint: Paint,
    boxOffset: Float,
    boxWidth: Float,
    boxHeight: Float,
    toolPosition: PointF,
    rotationAngle: Float,
    typeFaceInfo: SerializableTypeface
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
