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

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.audio.AudioServiceHolder
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException

class PlayToneAction : TemporalAction() {
    var scope: Scope? = null
    var frequency: Formula? = null
    var duration: Formula? = null

    override fun begin() {
        try {
            val freq = frequency?.interpretFloat(scope) ?: 440f
            val rawDur = duration?.interpretFloat(scope) ?: 1f
            val dur = rawDur.coerceIn(0f, 60f)
            super.setDuration(dur)

            val sampleRate = 44100
            val numSamples = (sampleRate * dur).toInt()
            if (numSamples <= 0) return
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val sample = (Math.sin(2.0 * Math.PI * freq / sampleRate * i) * Short.MAX_VALUE).toInt().toShort()
                samples[i] = sample
            }
            AudioServiceHolder.audioService.playTone(samples, sampleRate)
        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Formula interpretation failed", e)
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Failed to play tone", e)
        }
    }

    override fun update(percent: Float) {
    }

    override fun end() {
        AudioServiceHolder.audioService.stopTone()
    }
}
