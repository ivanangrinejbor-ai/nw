package org.catrobat.catroid.notification

/**
 * Global injection point for the active [NotificationService] implementation.
 *
 * Initialized once in [org.catrobat.catroid.stage.StageActivity.onCreate] with the
 * Android-backed implementation. The desktop runtime installs its own.
 */
object NotificationServiceHolder {
    lateinit var service: NotificationService
}
