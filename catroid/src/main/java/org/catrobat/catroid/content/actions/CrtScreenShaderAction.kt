/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
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
import kotlin.math.floor
import kotlin.math.sin

class CrtScreenShaderAction : TemporalAction() {
    var scope: Scope? = null
    var intensityFormula: Formula? = null

    private var originalRef: Pixmap? = null
    private var originalCopy: Pixmap? = null
    private var lastDst: Pixmap? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val lookData = s.sprite.look.lookData ?: return
        val src = lookData.pixmap ?: return
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return

        if (originalRef !== src || originalCopy == null) {
            originalRef = src
            originalCopy?.dispose()
            originalCopy = Pixmap(w, h, Pixmap.Format.RGBA8888).also { it.drawPixmap(src, 0, 0) }
            lastDst?.dispose()
            lastDst = null
        }

        val f = ((intensityFormula?.interpretFloat(s) ?: 80f) / 100f).coerceIn(0f, 1f)
        val original = originalCopy ?: return
        val dst = Pixmap(w, h, Pixmap.Format.RGBA8888)
        val temp = Color()

        val dw = (w - 1).coerceAtLeast(1).toFloat()
        val dh = (h - 1).coerceAtLeast(1).toFloat()

        for (y in 0 until h) {
            val ny = (y / dh - 0.5f) * 2f
            val scan = if (y % 3 == 2) 0.72f else 1f
            for (x in 0 until w) {
                val nx = (x / dw - 0.5f) * 2f
                Color.rgba8888ToColor(temp, original.getPixel(x, y))
                val r = temp.r
                val g = temp.g
                val b = temp.b

                var mr = 0.95f
                var mg = 0.95f
                var mb = 0.95f
                when (x % 3) {
                    0 -> mr = 1.06f
                    1 -> mg = 1.06f
                    2 -> mb = 1.06f
                }

                val d2 = (nx * nx + ny * ny) * 0.5f
                val vig = 1f - 0.45f * d2

                val grain = (hash(x, y) - 0.5f) * 0.08f

                val cr = ((r * scan * mr * vig + grain) * f + r * (1f - f)).coerceIn(0f, 1f)
                val cg = ((g * scan * mg * vig + grain) * f + g * (1f - f)).coerceIn(0f, 1f)
                val cb = ((b * scan * mb * vig + grain) * f + b * (1f - f)).coerceIn(0f, 1f)

                temp.r = cr
                temp.g = cg
                temp.b = cb
                dst.drawPixel(x, y, Color.rgba8888(temp))
            }
        }

        lastDst?.dispose()
        lastDst = dst

        val oldTex = lookData.textureRegion
        lookData.setPixmap(dst)
        lookData.setTextureRegion(TextureRegion(Texture(dst)))
        s.sprite.look.refreshTextures(true)
        if (oldTex != null) oldTex.texture.dispose()
    }

    companion object {
        private fun hash(x: Int, y: Int): Float {
            val h = sin((x * 12.9898 + y * 78.233).toDouble()) * 43758.5453
            return (h - floor(h)).toFloat()
        }
    }
}
