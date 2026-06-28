/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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
import org.catrobat.catroid.content.LocalServer
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.UserVariable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ListenServerAction() : Action() {
    companion object {
        private var sharedScheduler: ScheduledExecutorService? = null

        private fun getScheduler(): ScheduledExecutorService {
            if (sharedScheduler == null || sharedScheduler!!.isShutdown) {
                sharedScheduler = Executors.newSingleThreadScheduledExecutor()
            }
            return sharedScheduler!!
        }

        fun stopAll() {
            sharedScheduler?.shutdownNow()
            sharedScheduler = null
        }
    }

    var scope: Scope? = null
    var variable: UserVariable? = null

    override fun act(delta: Float): Boolean {
        if (variable == null) return true
        val v = variable!!

        getScheduler().scheduleAtFixedRate({
            v.value = LocalServer.getValue()
        }, 0, 30, TimeUnit.MILLISECONDS)

        return true
    }
}
