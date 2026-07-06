package org.catrobat.catroid.content.notification

import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

enum class ActionBehavior {
    LAUNCH_APP, RUN_IN_BACKGROUND, SILENT_BACKGROUND, ADD_INPUT_FIELD
}

data class NotificationActionData(
    val actionId: String,
    val text: String,
    val iconPath: String,
    val behavior: ActionBehavior,
    val hasInput: Boolean,
    val inputHint: String,
    val autoCancel: Boolean
) : Serializable

data class NotificationData(
    val id: Int,
    val channelName: String,
    val title: String,
    val text: String,
    val iconPath: String,
    val importanceLevel: Int,
    val isPinned: Boolean
) : Serializable

object NotificationStorage {
    private val notifications = ConcurrentHashMap<Int, NotificationData>()
    private val notificationActions = ConcurrentHashMap<Int, CopyOnWriteArrayList<NotificationActionData>>()

    @Volatile
    var lastActionId: String = ""
        private set
    @Volatile
    var lastReplyText: String = ""
        private set
    @Volatile
    var lastNotificationId: Int = -1
        private set
    @Volatile
    var lastActionButtonText: String = ""
        private set

    fun save(id: Int, data: NotificationData) {
        notifications[id] = data
    }

    fun get(id: Int): NotificationData? = notifications[id]

    fun clear() {
        notifications.clear()
        notificationActions.clear()
        lastActionId = ""
        lastReplyText = ""
        lastNotificationId = -1
        lastActionButtonText = ""
    }

    fun getAll(): Map<Int, NotificationData> = notifications.toMap()

    fun addAction(notificationId: Int, action: NotificationActionData) {
        val actions = notificationActions.getOrPut(notificationId) { CopyOnWriteArrayList() }
        actions.add(action)
    }

    fun getActions(notificationId: Int): List<NotificationActionData> {
        return notificationActions[notificationId]?.toList() ?: emptyList()
    }

    fun setEventData(notifId: Int, actId: String, buttonText: String, reply: String?) {
        lastNotificationId = notifId
        lastActionId = actId
        lastActionButtonText = buttonText
        if (reply != null) lastReplyText = reply
    }

    @JvmStatic
    fun removeStatic(id: Int) {
        notifications.remove(id)
        notificationActions.remove(id)
    }
}
