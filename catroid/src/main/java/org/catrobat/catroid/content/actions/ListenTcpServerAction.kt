package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.LocalServer
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.UserVariable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ListenTcpServerAction() : Action() {
    companion object {
        @Volatile
        private var sharedScheduler: ScheduledExecutorService? = null
        private var scheduledTask: ScheduledFuture<*>? = null

        @Synchronized
        private fun getScheduler(): ScheduledExecutorService {
            val current = sharedScheduler
            if (current == null || current.isShutdown) {
                val newScheduler = Executors.newSingleThreadScheduledExecutor()
                sharedScheduler = newScheduler
                return newScheduler
            }
            return current
        }

        @Synchronized
        fun stopAll() {
            scheduledTask?.cancel(false)
            scheduledTask = null
            sharedScheduler?.shutdownNow()
            sharedScheduler = null
        }
    }

    var scope: Scope? = null
    var variables: List<UserVariable>? = null

    override fun act(delta: Float): Boolean {
        val vars = variables ?: return true
        if (vars.isEmpty()) {
            return true
        }
        val scheduler = getScheduler()
        synchronized(ListenTcpServerAction) {
            scheduledTask?.cancel(false)
            scheduledTask = scheduler.scheduleAtFixedRate({
                val messages = LocalServer.getMessages()
                if (messages.isNotEmpty()) {
                    val k = vars.size
                    val start = maxOf(0, messages.size - k)
                    for (i in 0 until k) {
                        val index = start + i
                        if (index < messages.size) {
                            vars[i]?.value = messages[index]
                        }
                    }
                }
            }, 0, 30, TimeUnit.MILLISECONDS)
        }
        return true
    }
}