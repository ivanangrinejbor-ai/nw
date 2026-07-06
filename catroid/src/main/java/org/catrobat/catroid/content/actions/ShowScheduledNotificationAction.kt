package org.catrobat.catroid.content.actions

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.notification.NotificationEventReceiver
import org.catrobat.catroid.content.notification.NotificationStorage
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class ShowScheduledNotificationAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null
    var delay: Formula? = null

    override fun update(percent: Float) {
        val id = notificationId?.interpretInteger(scope) ?: return
        val delaySec = delay?.interpretInteger(scope) ?: 0
        val data = NotificationStorage.get(id) ?: return

        try {
            val activity = StageActivity.activeStageActivity.get() ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(data.channelName, data.channelName, data.importanceLevel)
                val manager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            if (delaySec <= 0) {
                showNotification(activity, id)
                NotificationStorage.remove(id)
            } else {
                val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val scheduleIntent = Intent(activity, NotificationEventReceiver::class.java)
                scheduleIntent.action = "SCHEDULED_NOTIFICATION"
                scheduleIntent.putExtra("notification_id", id)
                val schedPending = PendingIntent.getBroadcast(activity, 300000 + id, scheduleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val triggerAt = System.currentTimeMillis() + delaySec * 1000L
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, schedPending)
                Log.d(javaClass.simpleName, "Notification $id scheduled in ${delaySec}s")
            }
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to show/schedule notification $id", e)
        }
    }

    companion object {
        fun showNotification(context: Context, id: Int) {
            val data = NotificationStorage.get(id) ?: return
            val actions = NotificationStorage.getActions(id)

            val builder = NotificationCompat.Builder(context, data.channelName)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(data.title)
                .setContentText(data.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(data.text))
                .setAutoCancel(false)
                .setOngoing(data.isPinned)

            for (act in actions) {
                val intent = Intent(context, NotificationEventReceiver::class.java)
                intent.action = if (act.hasInput) "NOTIFICATION_REPLY_SENT" else "NOTIFICATION_ACTION_CLICKED"
                intent.putExtra("notification_id", id)
                intent.putExtra("action_id", act.actionId)
                intent.putExtra("button_text", act.text)

                val requestCode = "$id:${act.actionId}".hashCode()
                val pIntent = PendingIntent.getBroadcast(context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                if (act.hasInput) {
                    val remoteInput = RemoteInput.Builder("reply_text")
                        .setLabel(act.inputHint.ifEmpty { "Reply" })
                        .build()
                    val action = NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_edit, act.text, pIntent
                    ).addRemoteInput(remoteInput).build()
                    builder.addAction(action)
                } else {
                    builder.addAction(android.R.drawable.ic_menu_edit, act.text, pIntent)
                }
            }

            val showIntent = Intent(context, NotificationEventReceiver::class.java)
            showIntent.action = "NOTIFICATION_SHOWN"
            showIntent.putExtra("notification_id", id)
            val showPending = PendingIntent.getBroadcast(context, 100000 + id, showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.setContentIntent(showPending)

            val deleteIntent = Intent(context, NotificationEventReceiver::class.java)
            deleteIntent.action = "NOTIFICATION_DISMISSED"
            deleteIntent.putExtra("notification_id", id)
            val deletePending = PendingIntent.getBroadcast(context, 200000 + id, deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.setDeleteIntent(deletePending)

            NotificationManagerCompat.from(context).notify(id, builder.build())
        }
    }
}
