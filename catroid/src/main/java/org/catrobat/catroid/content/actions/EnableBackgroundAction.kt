package org.catrobat.catroid.content.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.service.ForegroundService
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.notification.NotificationService
import org.catrobat.catroid.notification.NotificationServiceHolder
import org.catrobat.catroid.stage.StageActivity

class EnableBackgroundAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null
    var channelName: Formula? = null
    var title: Formula? = null
    var text: Formula? = null
    var iconPath: Formula? = null
    var importanceLevel: Int = NotificationService.IMPORTANCE_DEFAULT

    override fun update(percent: Float) {
        try {
            val activity = StageActivity.activeStageActivity?.get() ?: return
            val id = notificationId?.interpretInteger(scope) ?: 1
            val channel = channelName?.interpretString(scope) ?: "default"
            val notifTitle = title?.interpretString(scope) ?: "Background Work"
            val notifText = text?.interpretString(scope) ?: "App is running in background"
            val icon = iconPath?.interpretString(scope) ?: ""

            // Create notification channel for API 26+ (via notification service seam)
            NotificationServiceHolder.service.ensureChannel(channel, NotificationService.IMPORTANCE_LOW)

            // Check POST_NOTIFICATIONS permission on API 33+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.w(javaClass.simpleName, "POST_NOTIFICATIONS permission not granted")
                    return
                }
            }

            val intent = Intent(activity, ForegroundService::class.java).apply {
                putExtra("notification_id", id)
                putExtra("channel_name", channel)
                putExtra("notification_title", notifTitle)
                putExtra("notification_text", notifText)
                putExtra("importance_level", importanceLevel)
            }

            ContextCompat.startForegroundService(activity, intent)
            Log.d(javaClass.simpleName, "Foreground service started (id=$id)")
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to start foreground service", e)
        }
    }
}
