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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.stage.StageActivity

class DrawTextAction : TemporalAction() {
    var scope: Scope? = null
    var x: Formula? = null
    var y: Formula? = null
    var text: Formula? = null

    private var batch: SpriteBatch? = null

    override fun update(delta: Float) {
        if (scope == null) return
        val s = scope ?: return
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val penActor = stageListener.penActor ?: return

        val textStr: String = try {
            text?.interpretString(s) ?: return
        } catch (e: InterpretationException) {
            return
        }
        if (textStr.isEmpty()) return

        val xVal = try { x?.interpretFloat(s) ?: 0f } catch (e: InterpretationException) { 0f }
        val yVal = try { y?.interpretFloat(s) ?: 0f } catch (e: InterpretationException) { 0f }

        // Render the text into an Android Bitmap (mirrors ShowTextActor.drawText),
        // then upload it as a libGDX Texture and draw it into the pen FrameBuffer.
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 40f
        }
        val baseline = -paint.ascent()
        val textHeight = (baseline + paint.descent()).toInt().coerceAtLeast(1)
        val textWidth = paint.measureText(textStr).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(textWidth, textHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(textStr, 0f, baseline, paint)

        val texture = Texture(bitmap.width, bitmap.height, Pixmap.Format.RGBA8888)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.getTextureObjectHandle())
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        bitmap.recycle()

        // Pre-flip vertically: the pen layer is displayed with a vertical flip,
        // so flipping here keeps the glyphs upright on screen.
        val region = TextureRegion(texture)
        region.flip(false, true)

        val buffer = penActor.buffer
        val camera = penActor.canvasCamera
        val spriteBatch = batch ?: SpriteBatch().also { batch = it }

        buffer.begin()
        spriteBatch.setProjectionMatrix(camera.combined)
        spriteBatch.begin()
        spriteBatch.draw(region, xVal, yVal)
        spriteBatch.end()
        buffer.end()

        texture.dispose()
    }
}
