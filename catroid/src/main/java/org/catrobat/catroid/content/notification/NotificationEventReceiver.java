package org.catrobat.catroid.content.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.RemoteInput;

import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.notification.NotificationStorage;
import org.catrobat.catroid.notification.NotificationServiceHolder;
import org.catrobat.catroid.stage.StageActivity;

public class NotificationEventReceiver extends BroadcastReceiver {

    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_ACTION_ID = "action_id";
    public static final String EXTRA_BUTTON_TEXT = "button_text";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        String actionId = intent.getStringExtra(EXTRA_ACTION_ID);
        String buttonText = intent.getStringExtra(EXTRA_BUTTON_TEXT);

        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        String replyText = null;
        if (remoteInput != null) {
            replyText = remoteInput.getString("reply_text");
        }

        NotificationStorage.INSTANCE.setEventData(
                notificationId,
                actionId != null ? actionId : "",
                buttonText != null ? buttonText : "",
                replyText
        );

        String action = intent.getAction();

        if ("NOTIFICATION_ACTION_CLICKED".equals(action) || "NOTIFICATION_REPLY_SENT".equals(action)) {
            int eventId = EventId.NOTIFICATION_ACTION_CLICKED;
            if ("NOTIFICATION_REPLY_SENT".equals(action)) {
                eventId = EventId.NOTIFICATION_REPLY_SENT;
            }
            broadcastEvent(eventId);
        } else if ("NOTIFICATION_SHOWN".equals(action)) {
            broadcastEvent(EventId.NOTIFICATION_SHOWN);
        } else if ("NOTIFICATION_DISMISSED".equals(action)) {
            broadcastEvent(EventId.NOTIFICATION_DISMISSED);
        } else if ("SCHEDULED_NOTIFICATION".equals(action)) {
            int nid = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
            if (nid != -1) {
                NotificationServiceHolder.service.show(nid);
                NotificationStorage.removeNotification(nid);
            }
            broadcastEvent(EventId.NOTIFICATION_SHOWN);
        }
    }

    public void broadcastEvent(int eventId) {
        StageActivity stage = StageActivity.activeStageActivity.get();
        if (stage != null) {
            stage.broadcastEventToAllSprites(new EventId(eventId));
        }
    }
}