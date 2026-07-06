package org.catrobat.catroid.content.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.notification.NotificationStorage
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SendNotificationAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null

    override fun update(percent: Float) {
        val id = notificationId?.interpretInteger(scope) ?: return
        val data = NotificationStorage.get(id) ?: return

        try {
            val activity = StageActivity.activeStageActivity.get() ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    data.channelName,
                    data.channelName,
                    data.importanceLevel
                )
                val manager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(activity, data.channelName)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(data.title)
                .setContentText(data.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(data.text))
                .setAutoCancel(!data.isPinned)
                .setOngoing(data.isPinned)

            NotificationManagerCompat.from(activity).notify(id, builder.build())
            Log.d(javaClass.simpleName, "Notification $id sent: ${data.title}")
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to send notification $id", e)
        }
    }
}
