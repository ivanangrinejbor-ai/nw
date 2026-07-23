/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.formulaeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.ImageSpan
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory

private const val COLOR_SQUARE_PADDING_LEFT = 15
private const val COLOR_SQUARE_PADDING_TOP = 0
private const val COLOR_STRING_CONVERSION_CONSTANT = 16
private const val COLOR_SQUARE_ROUNDED_CORNER_DIVIDER = 4
private const val CHECKER_CELL_SIZE = 4

class VisualizeColorString(
    context: Context,
    colorString: String,
    bitmapSize: Float
) {

    var drawable: RoundedBitmapDrawable
    var imageSpan: VisualizeColorImageSpan
    var colorValue = 0

    init {
        colorValue = getColorValueFromColorString(colorString)
        val size = bitmapSize.toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (Color.alpha(colorValue) < 255) {
            val checkerPaint = Paint()
            val lightColor = Color.rgb(204, 204, 204)
            val darkColor = Color.rgb(255, 255, 255)
            for (y in 0 until size step CHECKER_CELL_SIZE) {
                for (x in 0 until size step CHECKER_CELL_SIZE) {
                    checkerPaint.color = if ((x / CHECKER_CELL_SIZE + y / CHECKER_CELL_SIZE) % 2 == 0) darkColor else lightColor
                    canvas.drawRect(x.toFloat(), y.toFloat(),
                        (x + CHECKER_CELL_SIZE).toFloat(),
                        (y + CHECKER_CELL_SIZE).toFloat(), checkerPaint)
                }
            }
        }

        val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        colorPaint.color = colorValue
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), colorPaint)

        drawable = RoundedBitmapDrawableFactory.create(context.resources, bitmap)
        drawable.cornerRadius = bitmapSize / COLOR_SQUARE_ROUNDED_CORNER_DIVIDER
        drawable.setBounds(
            COLOR_SQUARE_PADDING_LEFT, COLOR_SQUARE_PADDING_TOP,
            drawable.intrinsicWidth + COLOR_SQUARE_PADDING_LEFT,
            drawable.intrinsicHeight + COLOR_SQUARE_PADDING_TOP
        )
        imageSpan = VisualizeColorImageSpan(drawable, colorValue)
    }

    private fun getColorValueFromColorString(colorString: String): Int {
        val newString = colorString.replace(Regex("[^A-Za-z0-9]"), "")
        return try {
            val parsed = newString.toLong(COLOR_STRING_CONVERSION_CONSTANT).toInt()
            if (newString.length == 6) {
                parsed or 0xFF000000.toInt()
            } else {
                parsed
            }
        } catch (nfe: NumberFormatException) {
            0
        }
    }
}

class VisualizeColorImageSpan(
    drawable: RoundedBitmapDrawable,
    val colorValue: Int
) : ImageSpan(drawable, ALIGN_BOTTOM)
