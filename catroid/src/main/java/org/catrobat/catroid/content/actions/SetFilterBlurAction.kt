/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
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
package org.catrobat.catroid.content.actions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetFilterBlurAction : TemporalAction() {
    var scope: Scope? = null
    var radius: Formula? = null

    override fun update(delta: Float) {
        val s = scope ?: return
        val kernel = (radius?.interpretFloat(s) ?: 3f).toInt().coerceAtLeast(1)
        val lookData = s.sprite.look.lookData ?: return
        val src = lookData.pixmap ?: return
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return

        val dst = Pixmap(w, h, src.format)
        val temp = Color()
        val horizontal = FloatArray(w * h * 3)
        val windowSize = kernel * 2 + 1

        for (y in 0 until h) {
            var sumR = 0f
            var sumG = 0f
            var sumB = 0f
            for (dx in -kernel..kernel) {
                Color.rgba8888ToColor(temp, src.getPixel(dx.coerceIn(0, w - 1), y))
                sumR += temp.r
                sumG += temp.g
                sumB += temp.b
            }
            for (x in 0 until w) {
                val index = (y * w + x) * 3
                horizontal[index] = sumR / windowSize
                horizontal[index + 1] = sumG / windowSize
                horizontal[index + 2] = sumB / windowSize

                val removeX = (x - kernel).coerceIn(0, w - 1)
                val addX = (x + kernel + 1).coerceIn(0, w - 1)
                Color.rgba8888ToColor(temp, src.getPixel(removeX, y))
                sumR -= temp.r
                sumG -= temp.g
                sumB -= temp.b
                Color.rgba8888ToColor(temp, src.getPixel(addX, y))
                sumR += temp.r
                sumG += temp.g
                sumB += temp.b
            }
        }

        val oldTex = lookData.textureRegion
        for (x in 0 until w) {
            var sumR = 0f
            var sumG = 0f
            var sumB = 0f
            for (dy in -kernel..kernel) {
                val index = (dy.coerceIn(0, h - 1) * w + x) * 3
                sumR += horizontal[index]
                sumG += horizontal[index + 1]
                sumB += horizontal[index + 2]
            }
            for (y in 0 until h) {
                val index = (y * w + x) * 3
                Color.rgba8888ToColor(temp, src.getPixel(x, y))
                temp.r = (sumR / windowSize).coerceIn(0f, 1f)
                temp.g = (sumG / windowSize).coerceIn(0f, 1f)
                temp.b = (sumB / windowSize).coerceIn(0f, 1f)
                dst.drawPixel(x, y, Color.rgba8888(temp))

                val removeY = (y - kernel).coerceIn(0, h - 1) * w * 3 + x * 3
                val addY = (y + kernel + 1).coerceIn(0, h - 1) * w * 3 + x * 3
                sumR -= horizontal[removeY]
                sumG -= horizontal[removeY + 1]
                sumB -= horizontal[removeY + 2]
                sumR += horizontal[addY]
                sumG += horizontal[addY + 1]
                sumB += horizontal[addY + 2]
            }
        }

        lookData.setPixmap(dst)
        lookData.setTextureRegion(TextureRegion(Texture(dst)))
        s.sprite.look.refreshTextures(true)
        if (oldTex != null) oldTex.texture.dispose()
        src.dispose()
    }
}
