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

class TintShaderAction : TemporalAction() {
    var scope: Scope? = null
    var red: Formula? = null
    var green: Formula? = null
    var blue: Formula? = null
    var amount: Formula? = null

    override fun update(delta: Float) {
        val s = scope ?: return
        val tr = ((red?.interpretFloat(s) ?: 255f) / 255f).coerceIn(0f, 1f)
        val tg = ((green?.interpretFloat(s) ?: 0f) / 255f).coerceIn(0f, 1f)
        val tb = ((blue?.interpretFloat(s) ?: 0f) / 255f).coerceIn(0f, 1f)
        val factor = ((amount?.interpretFloat(s) ?: 50f) / 100f).coerceIn(0f, 1f)

        val lookData = s.sprite.look.lookData ?: return
        val src = lookData.pixmap ?: return
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return

        var work = src
        var converted = false
        if (src.format != Pixmap.Format.RGBA8888) {
            work = Pixmap(w, h, Pixmap.Format.RGBA8888)
            work.drawPixmap(src, 0, 0)
            converted = true
        }

        val dst = Pixmap(w, h, Pixmap.Format.RGBA8888)
        val temp = Color()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = work.getPixel(x, y)
                Color.rgba8888ToColor(temp, pixel)
                temp.r = (temp.r * (1f - factor) + tr * factor).coerceIn(0f, 1f)
                temp.g = (temp.g * (1f - factor) + tg * factor).coerceIn(0f, 1f)
                temp.b = (temp.b * (1f - factor) + tb * factor).coerceIn(0f, 1f)
                var out = Color.rgba8888(temp)
                out = (out and 0x00FFFFFF) or (pixel and 0xFF000000.toInt())
                dst.drawPixel(x, y, out)
            }
        }

        if (converted) work.dispose()
        val oldTex = lookData.textureRegion
        lookData.setPixmap(dst)
        lookData.setTextureRegion(TextureRegion(Texture(dst)))
        s.sprite.look.refreshTextures(true)
        if (oldTex != null) oldTex.texture.dispose()
        if (src != dst) src.dispose()
    }
}
