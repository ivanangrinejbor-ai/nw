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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.io.DeviceListAccessor

class ReadListFromDeviceAction : AsynchronousAction() {
    private var userList: UserList? = null

    @Volatile
    private var readActionFinished = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun act(delta: Float): Boolean {
        if (userList == null) return true
        return super.act(delta)
    }

    override fun initialize() {
        readActionFinished = false
        val list = userList ?: run {
            readActionFinished = true
            return
        }
        scope.launch {
            try {
                val projectDirectory = ProjectManager.getInstance().currentProject.directory
                val accessor = DeviceListAccessor(projectDirectory)
                accessor.readUserData(list)
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "Failed to read list from device", e)
            } finally {
                readActionFinished = true
            }
        }
    }

    override fun isFinished(): Boolean = readActionFinished

    fun setUserList(userList: UserList?) {
        this.userList = userList
    }
}
