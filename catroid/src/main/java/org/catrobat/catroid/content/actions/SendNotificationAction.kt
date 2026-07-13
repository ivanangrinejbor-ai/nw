package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.notification.NotificationServiceHolder

class SendNotificationAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null

    // Execute only once per block invocation
    private var started = false

    override fun restart() {
        started = false
        super.restart()
    }

    override fun update(percent: Float) {
        if (started) return
        val id = notificationId?.interpretInteger(scope) ?: return
        started = true
        try {
            NotificationServiceHolder.service.show(id)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to send notification $id", e)
        }
    }
}
