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

class SetFilterPixelateAction : TemporalAction() {
    var scope: Scope? = null
    var size: Formula? = null

    override fun update(delta: Float) {
        val s = scope ?: return
        val blockSize = (size?.interpretFloat(s) ?: 8f).toInt().coerceAtLeast(1)
        val lookData = s.sprite.look.lookData ?: return
        val src = lookData.pixmap ?: return
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return

        val dst = Pixmap(w, h, src.format)
        val temp = Color()

        for (y in 0 until h step blockSize) {
            for (x in 0 until w step blockSize) {
                Color.argb8888ToColor(temp, src.getPixel(x, y))
                val ex = (x + blockSize).coerceAtMost(w)
                val ey = (y + blockSize).coerceAtMost(h)
                dst.setColor(temp)
                dst.fillRectangle(x, y, ex - x, ey - y)
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
