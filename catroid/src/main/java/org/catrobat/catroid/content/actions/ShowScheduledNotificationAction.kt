package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.notification.NotificationServiceHolder

class ShowScheduledNotificationAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null
    var delay: Formula? = null

    private var started = false

    override fun restart() {
        started = false
        super.restart()
    }

    override fun update(percent: Float) {
        if (started) return
        val s = scope ?: return
        // Interpret notification ID as integer and schedule delay as double seconds
        val id = notificationId?.interpretInteger(s) ?: return
        val rawDelay = delay?.interpretDouble(s) ?: 0.0
        val delayMs = if (rawDelay > Long.MAX_VALUE / 1000) 0L else (rawDelay * 1000).toLong()
        started = true
        try {
            NotificationServiceHolder.service.showScheduled(id, delayMs)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to show/schedule notification $id", e)
        }
    }
}
