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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import kotlinx.coroutines.runBlocking
import org.catrobat.catroid.ai.model.AiProvider
import org.catrobat.catroid.ai.model.CloudModelRuntime
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.content.EventWrapper
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.eventids.AiResponseEventId
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import java.io.ByteArrayOutputStream
import java.io.File

class AskAIVisionAction : TemporalAction() {
    var scope: Scope? = null
    var prompt: Formula? = null
    var systemPrompt: Formula? = null
    var model: Formula? = null
    var provider: String = AiProvider.GEMINI.id
    var lookData: LookData? = null
    var userVariable: UserVariable? = null

    private var started = false

    override fun update(percent: Float) {
        if (started) return
        started = true
        val sc = scope
        val variable = userVariable
        val look = lookData
        if (sc == null || variable == null || look == null) {
            return
        }

        val promptStr = prompt?.interpretObject(sc)?.toString() ?: ""
        val systemStr = systemPrompt?.interpretObject(sc)?.toString() ?: ""
        val modelStr = model?.interpretObject(sc)?.toString() ?: ""
        val imageFile = look.file
        if (imageFile == null || !imageFile.exists()) {
            variable.value = "Error: Look image not found"
            return
        }

        val providerObj = AiProvider.fromId(provider)

        Thread {
            val base64 = encodePngBase64(imageFile)
            val result = if (base64 == null) {
                "Error: Could not decode look image"
            } else {
                runBlocking {
                    CloudModelRuntime.generateVisionForProvider(
                        providerObj,
                        modelStr,
                        systemStr,
                        promptStr,
                        base64
                    )
                }
            }
            variable.value = result
            val sprite = sc.sprite
            if (sprite != null && sprite.look != null) {
                sprite.look.fire(EventWrapper(AiResponseEventId(sprite, providerObj.id), false))
            }
        }.start()
    }

    override fun restart() {
        super.restart()
        started = false
    }

    companion object {
        fun encodePngBase64(file: File): String? = try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val scaled = scaleDown(bitmap)
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 90, output)
            if (scaled !== bitmap) {
                bitmap.recycle()
            }
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }

        private const val MAX_DIMENSION = 1024

        private fun scaleDown(bitmap: Bitmap): Bitmap {
            val maxSide = maxOf(bitmap.width, bitmap.height)
            if (maxSide <= MAX_DIMENSION) {
                return bitmap
            }
            val scale = MAX_DIMENSION.toFloat() / maxSide
            return Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }
    }
}
