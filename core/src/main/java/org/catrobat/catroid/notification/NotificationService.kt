package org.catrobat.catroid.notification

interface NotificationService {
    companion object {
        const val IMPORTANCE_NONE = 0
        const val IMPORTANCE_MIN = 1
        const val IMPORTANCE_LOW = 2
        const val IMPORTANCE_DEFAULT = 3
        const val IMPORTANCE_HIGH = 4
        const val IMPORTANCE_MAX = 5
    }

    fun show(id: Int)
    fun showScheduled(id: Int, delayMs: Long)
    fun remove(id: Int)
    fun ensureChannel(name: String, importance: Int)
}
