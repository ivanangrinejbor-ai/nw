package org.catrobat.catroid.notification

/**
 * Platform-independent notification surface used by the notification bricks.
 *
 * Mirrors the Android notification display (channels, notify, scheduled alarm,
 * foreground channel) so the desktop player can supply an alternative
 * implementation (e.g. a system tray entry) without touching the action classes.
 *
 * Notification data lives in
 * [org.catrobat.catroid.content.notification.NotificationStorage] (portable) and is
 * read by the active implementation when displaying by id.
 */
interface NotificationService {
    companion object {
        const val IMPORTANCE_NONE = 0
        const val IMPORTANCE_MIN = 1
        const val IMPORTANCE_LOW = 2
        const val IMPORTANCE_DEFAULT = 3
        const val IMPORTANCE_HIGH = 4
        const val IMPORTANCE_MAX = 5
    }

    /** Display the notification identified by [id] (data read from NotificationStorage). No-op if not prepared. */
    fun show(id: Int)

    /** Display after [delayMs] milliseconds. If <= 0, show immediately and clean up. */
    fun showScheduled(id: Int, delayMs: Long)

    /** Cancel the notification and clean up in-memory data. */
    fun remove(id: Int)

    /** Ensure a notification channel exists (Android) / no-op otherwise. */
    fun ensureChannel(name: String, importance: Int)
}
