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

class SetFilterSepiaAction : TemporalAction() {
    var scope: Scope? = null
    var intensity: Formula? = null

    override fun update(delta: Float) {
        val s = scope ?: return
        val intensityVal = (intensity?.interpretFloat(s) ?: 0f).coerceIn(0f, 1f)
        val lookData = s.sprite.look.lookData ?: return
        val src = lookData.pixmap ?: return
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return

        val dst = Pixmap(w, h, src.format)
        val temp = Color()

        for (y in 0 until h) {
            for (x in 0 until w) {
                Color.rgba8888ToColor(temp, src.getPixel(x, y))
                val r = temp.r
                val g = temp.g
                val b = temp.b
                val sr = (r * 0.393f + g * 0.769f + b * 0.189f).coerceIn(0f, 1f)
                val sg = (r * 0.349f + g * 0.686f + b * 0.168f).coerceIn(0f, 1f)
                val sb = (r * 0.272f + g * 0.534f + b * 0.131f).coerceIn(0f, 1f)
                temp.r = r + (sr - r) * intensityVal
                temp.g = g + (sg - g) * intensityVal
                temp.b = b + (sb - b) * intensityVal
                dst.drawPixel(x, y, Color.rgba8888(temp))
            }
        }

        val oldTex = lookData.textureRegion
        lookData.setPixmap(dst)
        lookData.setTextureRegion(TextureRegion(Texture(dst)))
        s.sprite.look.refreshTextures(true)
        if (oldTex != null) oldTex.texture.dispose()
        src.dispose()
    }
}
