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

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import kotlin.math.cos
import kotlin.math.sin

class SpriteWaveShaderAction : TemporalAction() {
    var scope: Scope? = null
    var amplitudeFormula: Formula? = null
    var phaseFormula: Formula? = null

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

        val ampNorm = ((amplitudeFormula?.interpretFloat(s) ?: 30f) / 100f).coerceIn(0f, 1f)
        val ampX = ampNorm * w * MAX_AMPLITUDE_FACTOR
        val ampY = ampNorm * h * MAX_AMPLITUDE_FACTOR * 0.6f
        val phase = Math.toRadians(((phaseFormula?.interpretFloat(s) ?: 0f) % 360f).toDouble())

        val original = originalCopy ?: return
        val dst = Pixmap(w, h, Pixmap.Format.RGBA8888)

        for (y in 0 until h) {
            val ny = y.toFloat() / h
            val dx = (sin(ny * FREQUENCY.toDouble() + phase) * ampX).toInt()
            for (x in 0 until w) {
                val nx = x.toFloat() / w
                val dy = (cos(nx * FREQUENCY.toDouble() + phase * 0.7) * ampY).toInt()
                val sx = (x - dx).coerceIn(0, w - 1)
                val sy = (y - dy).coerceIn(0, h - 1)
                dst.drawPixel(x, y, original.getPixel(sx, sy))
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
        const val MAX_AMPLITUDE_FACTOR = 0.08f
        const val FREQUENCY = 18.0
    }
}
