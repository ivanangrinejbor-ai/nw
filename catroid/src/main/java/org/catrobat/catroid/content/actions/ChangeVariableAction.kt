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

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.bluetooth.base.BluetoothDevice
import org.catrobat.catroid.common.CatroidService
import org.catrobat.catroid.common.ServiceProvider
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.devices.multiplayer.MultiplayerInterface
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable

class ChangeVariableAction : Action() {
    var scope: Scope? = null
    var changeVariable: Formula? = null
    var userVariable: UserVariable? = null

    override fun act(delta: Float): Boolean {
        val variable = userVariable ?: return true
        val originalValue: Double = when (val v = variable.value) {
            is Double -> v
            is Boolean -> if (v) 1.0 else 0.0
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val change = changeVariable?.interpretObject(scope)
        val changeValue: Double = when (change) {
            is Double -> change
            is Boolean -> if (change) 1.0 else 0.0
            is String -> change.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        updateUserVariable(originalValue, changeValue)
        return true
    }

    private fun updateUserVariable(originalValue: Double, value: Double) {
        val original = originalValue.takeUnless { it.isNaN() } ?: 0.0
        val valueToAdd = value.takeUnless { it.isNaN() } ?: 0.0
        userVariable?.value = original + valueToAdd

        val multiplayerVariable = ProjectManager.getInstance().currentProject.getMultiplayerVariable(userVariable?.name)
        multiplayerVariable?.let {
            val multiplayerDevice = getMultiplayerDevice()
            multiplayerDevice?.sendChangedMultiplayerVariables(userVariable)
        }
    }

    fun getMultiplayerDevice(): MultiplayerInterface? = ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE).getDevice(BluetoothDevice.MULTIPLAYER)
}
