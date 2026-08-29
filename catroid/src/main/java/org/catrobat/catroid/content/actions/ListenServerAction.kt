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
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ListenServerAction() : Action() {
    companion object {
        private const val POLL_INTERVAL_MS = 30L
        private const val MAX_TASKS = 32

        private var sharedScheduler: ScheduledExecutorService? = null
        private val tasks = mutableListOf<ScheduledFuture<*>>()

        @Synchronized
        private fun register(action: ListenServerAction) {
            var scheduler = sharedScheduler
            if (scheduler == null || scheduler.isShutdown) {
                scheduler = Executors.newSingleThreadScheduledExecutor()
                sharedScheduler = scheduler
            }
            while (tasks.size >= MAX_TASKS) {
                tasks.removeAt(0).cancel(false)
            }
            tasks.add(scheduler.scheduleAtFixedRate({
                action.variable?.let { variable -> variable.value = LocalServer.getValue() }
            }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS))
        }

        @Synchronized
        fun stopAll() {
            for (task in tasks) {
                task.cancel(false)
            }
            tasks.clear()
            sharedScheduler?.shutdownNow()
            sharedScheduler = null
        }
    }

    var scope: Scope? = null
    var variable: UserVariable? = null

    override fun act(delta: Float): Boolean {
        val v = variable ?: return true

        register(this)

        return true
    }
}
