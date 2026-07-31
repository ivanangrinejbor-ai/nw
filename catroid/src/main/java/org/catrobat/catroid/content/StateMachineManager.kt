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

package org.catrobat.catroid.content

import java.util.Collections
import java.util.IdentityHashMap

object StateMachineManager {

    private class StateEntry(var state: String, var enterTimeMillis: Long)

    private val spriteMachines: MutableMap<Sprite, MutableMap<String, StateEntry>> =
        Collections.synchronizedMap(IdentityHashMap())

    private fun normalize(machine: String?): String = machine?.trim().orEmpty()

    @JvmStatic
    fun setState(sprite: Sprite?, machine: String?, state: String?) {
        if (sprite == null) {
            return
        }
        val machineName = normalize(machine)
        val stateName = state?.trim().orEmpty()
        synchronized(spriteMachines) {
            val machines = spriteMachines.getOrPut(sprite) { HashMap() }
            val existing = machines[machineName]
            if (existing == null || existing.state != stateName) {
                machines[machineName] = StateEntry(stateName, System.currentTimeMillis())
            }
        }
    }

    @JvmStatic
    fun getState(sprite: Sprite?, machine: String?): String {
        if (sprite == null) {
            return ""
        }
        synchronized(spriteMachines) {
            return spriteMachines[sprite]?.get(normalize(machine))?.state ?: ""
        }
    }

    @JvmStatic
    fun isInState(sprite: Sprite?, machine: String?, state: String?): Boolean =
        getState(sprite, machine) == state?.trim().orEmpty()

    @JvmStatic
    fun getStateTimeSeconds(sprite: Sprite?, machine: String?): Double {
        if (sprite == null) {
            return 0.0
        }
        synchronized(spriteMachines) {
            val entry = spriteMachines[sprite]?.get(normalize(machine)) ?: return 0.0
            return (System.currentTimeMillis() - entry.enterTimeMillis) / 1000.0
        }
    }

    @JvmStatic
    fun reset() {
        synchronized(spriteMachines) {
            spriteMachines.clear()
        }
    }
}
