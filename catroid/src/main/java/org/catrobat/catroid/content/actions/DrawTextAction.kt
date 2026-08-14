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

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.text.TextServiceHolder

class DrawTextAction : TemporalAction() {
    var scope: Scope? = null
    var x: Formula? = null
    var y: Formula? = null
    var text: Formula? = null

    private var batch: SpriteBatch? = null

    override fun update(delta: Float) {
        val currentScope = scope ?: return
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val penActor = stageListener.penActor ?: return

        val textStr = try {
            text?.interpretString(currentScope) ?: return
        } catch (_: InterpretationException) {
            return
        }
        if (textStr.isEmpty()) return

        val xVal = try {
            x?.interpretFloat(currentScope) ?: 0f
        } catch (_: InterpretationException) {
            0f
        }
        val yVal = try {
            y?.interpretFloat(currentScope) ?: 0f
        } catch (_: InterpretationException) {
            0f
        }

        val font = currentScope.sprite.penConfiguration.getFontName()
        val fontSize = currentScope.sprite.penConfiguration.getFontSize().toFloat().coerceAtLeast(8f)
        val penColor = currentScope.sprite.penConfiguration.getPenColor()
        val colorHex = String.format("#%02X%02X%02X%02X", (penColor.a * 255).toInt(), (penColor.r * 255).toInt(), (penColor.g * 255).toInt(), (penColor.b * 255).toInt())

        val rasterizedText = try {
            TextServiceHolder.textService.rasterizeText(
                textStr,
                fontSize,
                font,
                colorHex,
                false,
                0
            )
        } catch (_: UninitializedPropertyAccessException) {
            return
        }

        val pixmap = Pixmap(rasterizedText.width, rasterizedText.height, Pixmap.Format.RGBA8888)
        pixmap.pixels.put(rasterizedText.rgba)
        pixmap.pixels.position(0)

        val texture = Texture(pixmap)
        pixmap.dispose()

        val region = TextureRegion(texture).apply {
            flip(false, true)
        }

        val spriteBatch = batch ?: SpriteBatch().also { batch = it }
        val buffer = penActor.buffer
        val camera = penActor.canvasCamera

        buffer.begin()
        spriteBatch.setProjectionMatrix(camera.combined)
        spriteBatch.begin()
        spriteBatch.draw(region, xVal, yVal)
        spriteBatch.end()
        buffer.end()

        texture.dispose()
    }
}
