package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.notification.NotificationData
import org.catrobat.catroid.content.notification.NotificationStorage
import org.catrobat.catroid.formulaeditor.Formula

class PrepareNotificationAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null
    var channelName: Formula? = null
    var title: Formula? = null
    var text: Formula? = null
    var iconPath: Formula? = null
    var importanceLevel: Int = android.app.NotificationManager.IMPORTANCE_DEFAULT
    var isPinned: Boolean = false

    private var started = false

    override fun restart() {
        super.restart()
        started = false
    }

    override fun update(percent: Float) {
        if (started) return
        started = true

        val id = notificationId?.interpretInteger(scope) ?: return
        val channel = channelName?.interpretString(scope) ?: "default"
        val notifTitle = title?.interpretString(scope) ?: ""
        val notifText = text?.interpretString(scope) ?: ""
        val icon = iconPath?.interpretString(scope) ?: ""

        val data = NotificationData(
            id = id,
            channelName = channel,
            title = notifTitle,
            text = notifText,
            iconPath = icon,
            importanceLevel = importanceLevel,
            isPinned = isPinned
        )
        NotificationStorage.save(id, data)
    }
}
