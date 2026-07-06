package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.notification.ActionBehavior
import org.catrobat.catroid.content.notification.NotificationActionData
import org.catrobat.catroid.content.notification.NotificationStorage
import org.catrobat.catroid.formulaeditor.Formula

class NotificationActionAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null
    var actionId: Formula? = null
    var text: Formula? = null
    var iconPath: Formula? = null
    var hint: Formula? = null
    var behaviorIndex: Int = 0
    var hasInput: Boolean = false

    override fun update(percent: Float) {
        val nid = notificationId?.interpretInteger(scope) ?: return
        val aid = actionId?.interpretString(scope) ?: return
        val txt = text?.interpretString(scope) ?: ""
        val icon = iconPath?.interpretString(scope) ?: ""
        val hnt = hint?.interpretString(scope) ?: ""
        val behavior = ActionBehavior.values().getOrElse(behaviorIndex) { ActionBehavior.LAUNCH_APP }

        val data = NotificationActionData(
            actionId = aid, text = txt, iconPath = icon,
            behavior = behavior, hasInput = hasInput,
            inputHint = hnt, autoCancel = true
        )
        NotificationStorage.addAction(nid, data)
    }
}
