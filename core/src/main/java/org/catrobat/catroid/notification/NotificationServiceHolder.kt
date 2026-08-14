package org.catrobat.catroid.notification

object NotificationServiceHolder {
    lateinit var service: NotificationService
    fun isServiceInitialized(): Boolean = ::service.isInitialized
}
