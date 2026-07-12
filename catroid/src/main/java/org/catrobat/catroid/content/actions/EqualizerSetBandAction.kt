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

import android.media.audiofx.Equalizer
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException

class EqualizerSetBandAction : TemporalAction() {
    var scope: Scope? = null
    var band: Formula? = null
    var gain: Formula? = null
    private var equalizer: Equalizer? = null

    override fun begin() {
        try {
            equalizer = Equalizer(0, 0)
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Equalizer init failed", e)
        }
    }

    override fun update(percent: Float) {
        val eq = equalizer ?: return
        try {
            val bandIndex = band?.interpretInteger(scope) ?: return
            val gainMb = gain?.interpretInteger(scope) ?: return
            val numberOfBands = eq.numberOfBands
            if (bandIndex in 0 until numberOfBands) {
                eq.setBandLevel(bandIndex.toShort(), gainMb.toShort())
            }
        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Formula interpretation failed", e)
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Equalizer failed", e)
        }
    }

    override fun end() {
        equalizer?.release()
        equalizer = null
    }
}
