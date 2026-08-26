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

class SpriteDissolveShaderAction : TemporalAction() {
    var scope: Scope? = null
    var progressFormula: Formula? = null

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

        val t = ((progressFormula?.interpretFloat(s) ?: 0f) / 100f).coerceIn(0f, 1f)
        val original = originalCopy ?: return

        val dst = Pixmap(w, h, Pixmap.Format.RGBA8888)
        val temp = Color()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = original.getPixel(x, y)
                Color.rgba8888ToColor(temp, pixel)
                val n = noise(x, y)
                if (n < t) continue
                var out = Color.rgba8888(temp)
                out = (out and 0x00FFFFFF) or (pixel and 0xFF000000.toInt())
                val edge = ((n - t) / DISSOLVE_EDGE).coerceIn(0f, 1f)
                if (edge < 1f) {
                    val burnMix = 1f - edge
                    val r = out and 0x00FF0000.toInt() shr 16
                    val g = out and 0x0000FF00 shr 8
                    val b = out and 0x000000FF
                    val burnR = lerp(r, BURN_R, burnMix)
                    val burnG = lerp(g, BURN_G, burnMix)
                    val burnB = lerp(b, BURN_B, burnMix)
                    out = (out and 0xFF000000.toInt()) or (burnR shl 16) or (burnG shl 8) or burnB
                }
                dst.drawPixel(x, y, out)
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

    private fun noise(x: Int, y: Int): Float {
        val coarse = hash(x / 12f, y / 12f)
        val fine = hash(x / 4f, y / 4f)
        return coarse * 0.85f + fine * 0.15f
    }

    private fun hash(px: Float, py: Float): Float {
        val v = Math.sin((px * 127.1 + py * 311.7)).toFloat() * 43758.5453123f
        return v - floor(v)
    }

    private fun lerp(a: Int, b: Int, mix: Float): Int =
        (a + (b - a) * mix).toInt().coerceIn(0, 255)

    companion object {
        const val DISSOLVE_EDGE = 0.15f
        const val BURN_R = 230
        const val BURN_G = 90
        const val BURN_B = 20
    }
}
