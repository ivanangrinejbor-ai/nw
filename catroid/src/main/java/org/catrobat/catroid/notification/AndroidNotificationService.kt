package org.catrobat.catroid.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import org.catrobat.catroid.content.notification.ActionBehavior
import org.catrobat.catroid.content.notification.NotificationActionData
import org.catrobat.catroid.content.notification.NotificationData
import org.catrobat.catroid.content.notification.NotificationEventReceiver
import org.catrobat.catroid.content.notification.NotificationStorage
import org.json.JSONArray
import org.json.JSONObject

class AndroidNotificationService(private val context: Context) : NotificationService {

    private val createdChannels = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val prefs = context.getSharedPreferences("neocatroid_notifications", Context.MODE_PRIVATE)

    private fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted. Notification skipped.")
                return false
            }
        }
        return true
    }

    private fun createChannel(name: String, importance: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channelName = name.ifEmpty { "default" }
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(channelName)
            if (existing != null && existing.importance != importance) {
                manager.deleteNotificationChannel(channelName)
                createdChannels.remove(channelName)
            }
            if (createdChannels.add(channelName)) {
                val channel = NotificationChannel(channelName, channelName, importance)
                manager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create channel $channelName", e)
        }
    }

    private fun persist(id: Int, data: NotificationData) {
        try {
            val json = JSONObject().apply {
                put("channel", data.channelName)
                put("title", data.title)
                put("text", data.text)
                put("icon", data.iconPath)
                put("importance", data.importanceLevel)
                put("pinned", data.isPinned)
                val actionsArr = JSONArray()
                NotificationStorage.getActions(id).forEach { act ->
                    actionsArr.put(JSONObject().apply {
                        put("id", act.actionId)
                        put("text", act.text)
                        put("icon", act.iconPath)
                        put("behavior", act.behavior.name)
                        put("hasInput", act.hasInput)
                        put("inputHint", act.inputHint)
                        put("autoCancel", act.autoCancel)
                    })
                }
                put("actions", actionsArr)
            }
            prefs.edit().putString("data_$id", json.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist notification $id", e)
        }
    }

    private fun restore(id: Int): NotificationData? {
        val raw = prefs.getString("data_$id", null) ?: return null
        return try {
            val json = JSONObject(raw)
            NotificationStorage.save(id, NotificationData(
                id = id,
                channelName = json.optString("channel", "default"),
                title = json.optString("title", ""),
                text = json.optString("text", ""),
                iconPath = json.optString("icon", ""),
                importanceLevel = json.optInt("importance", NotificationService.IMPORTANCE_DEFAULT),
                isPinned = json.optBoolean("pinned", false)
            ))
            val actionsArr = json.optJSONArray("actions")
            if (actionsArr != null) {
                for (i in 0 until actionsArr.length()) {
                    val act = actionsArr.getJSONObject(i)
                    NotificationStorage.addAction(id, NotificationActionData(
                        actionId = act.optString("id", ""),
                        text = act.optString("text", ""),
                        iconPath = act.optString("icon", ""),
                        behavior = runCatching { ActionBehavior.valueOf(act.optString("behavior", "LAUNCH_APP")) }
                            .getOrDefault(ActionBehavior.LAUNCH_APP),
                        hasInput = act.optBoolean("hasInput", false),
                        inputHint = act.optString("inputHint", ""),
                        autoCancel = act.optBoolean("autoCancel", false)
                    ))
                }
            }
            NotificationStorage.get(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore notification $id", e)
            null
        }
    }

    private fun clearPersisted(id: Int) {
        try {
            prefs.edit().remove("data_$id").apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear persisted notification $id", e)
        }
    }

    override fun show(id: Int) {
        val data = NotificationStorage.get(id) ?: restore(id) ?: return
        try {
            if (!canPost()) return
            createChannel(data.channelName, data.importanceLevel)

            val builder = NotificationCompat.Builder(context, data.channelName.ifEmpty { "default" })
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(data.title)
                .setContentText(data.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(data.text))
                .setAutoCancel(!data.isPinned)
                .setOngoing(data.isPinned)

            NotificationStorage.getActions(id).forEachIndexed { index, act ->
                val intent = Intent(context, NotificationEventReceiver::class.java).apply {
                    action = if (act.hasInput) "NOTIFICATION_REPLY_SENT" else "NOTIFICATION_ACTION_CLICKED"
                    putExtra("notification_id", id)
                    putExtra("action_id", act.actionId)
                    putExtra("button_text", act.text)
                }
                val requestCode = id * 1000 + index
                val pIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
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

            val showIntent = Intent(context, NotificationEventReceiver::class.java).apply {
                action = "NOTIFICATION_SHOWN"
                putExtra("notification_id", id)
            }
            val showPending = PendingIntent.getBroadcast(
                context, 100000 + id, showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(showPending)

            val deleteIntent = Intent(context, NotificationEventReceiver::class.java).apply {
                action = "NOTIFICATION_DISMISSED"
                putExtra("notification_id", id)
            }
            val deletePending = PendingIntent.getBroadcast(
                context, 200000 + id, deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setDeleteIntent(deletePending)

            NotificationManagerCompat.from(context).notify(id, builder.build())
            clearPersisted(id)
            Log.d(TAG, "Notification $id sent: ${data.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification $id", e)
        }
    }

    override fun showScheduled(id: Int, delayMs: Long) {
        val data = NotificationStorage.get(id) ?: return
        try {
            if (!canPost()) return
            createChannel(data.channelName, data.importanceLevel)

            if (delayMs <= 0) {
                show(id)
                NotificationStorage.removeNotification(id)
                return
            }

            persist(id, data)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val scheduleIntent = Intent(context, NotificationEventReceiver::class.java).apply {
                action = "SCHEDULED_NOTIFICATION"
                putExtra("notification_id", id)
            }
            val schedPending = PendingIntent.getBroadcast(
                context, 300000 + id, scheduleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAt = System.currentTimeMillis() + delayMs
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, schedPending)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, schedPending)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, schedPending)
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact alarm not allowed, falling back to inexact for notification $id", e)
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, schedPending)
            }
            Log.d(TAG, "Notification $id scheduled in ${delayMs}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show/schedule notification $id", e)
        }
    }

    override fun remove(id: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(id)
            NotificationStorage.removeNotification(id)
            clearPersisted(id)
            Log.d(TAG, "Notification $id removed from tray")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove notification $id", e)
        }
    }

    override fun ensureChannel(name: String, importance: Int) {
        createChannel(name, importance)
    }

    companion object {
        private const val TAG = "AndroidNotificationService"
    }
}
