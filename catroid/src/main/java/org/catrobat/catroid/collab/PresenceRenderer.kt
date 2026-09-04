package org.catrobat.catroid.collab

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import org.catrobat.catroid.ProjectManager

object PresenceRenderer {
    private const val TAG = "PresenceRenderer"

    @Volatile var myHue: Float = 0f
    @Volatile var myName: String = ""

    private val observers = LinkedHashMap<String, () -> Unit>()

    fun addObserver(key: String, callback: () -> Unit) {
        synchronized(lock) { observers[key] = callback }
    }

    fun removeObserver(key: String) {
        synchronized(lock) { observers.remove(key) }
    }

    private val lock = Any()
    private var bySprite: Map<String, List<MemberPresence>> = emptyMap()
    private var latest: List<MemberPresence> = emptyList()
    private val drawableCache = HashMap<String, PresenceBorderDrawable?>()
    private val handler = Handler(Looper.getMainLooper())
    private var pendingNotify = false

    private val debounced = Runnable {
        pendingNotify = false
        val callbacks = synchronized(lock) { observers.values.toList() }
        for (callback in callbacks) {
            try {
                callback.invoke()
            } catch (e: Exception) {
                Log.w(TAG, "notify failed", e)
            }
        }
    }

    fun ingest(list: List<MemberPresence>) {
        synchronized(lock) {
            latest = list
            bySprite = list
                .filter { it.spriteId.isNotEmpty() && it.detail != "creating" }
                .groupBy { it.spriteId }
            drawableCache.clear()
        }
        if (!pendingNotify) {
            pendingNotify = true
            handler.postDelayed(debounced, 300L)
        }
    }

    fun clear() {
        synchronized(lock) {
            latest = emptyList()
            bySprite = emptyMap()
            drawableCache.clear()
        }
    }

    fun snapshot(): List<MemberPresence> = synchronized(lock) { latest }

    fun borderFor(view: View, spriteId: String?) {
        if (!CollabSession.isActive || spriteId.isNullOrEmpty()) {
            if (view.foreground != null) view.foreground = null
            return
        }
        val colors = synchronized(lock) {
            bySprite[spriteId]?.map { PresenceColors.colorInt(it.colorHue) } ?: emptyList()
        }
        if (colors.isEmpty()) {
            if (view.foreground != null) view.foreground = null
            return
        }
        val key = spriteId + colors.hashCode()
        val drawable = synchronized(lock) {
            drawableCache.getOrPut(key) {
                PresenceBorderDrawable(colors, view.resources.displayMetrics.density)
            }
        }
        if (view.foreground !== drawable) view.foreground = drawable
    }

    fun tabColors(spriteId: String?, tab: String): List<Int> {
        if (!CollabSession.isActive || spriteId.isNullOrEmpty()) return emptyList()
        return synchronized(lock) {
            latest.filter { it.spriteId == spriteId && it.tab == tab }
                .map { PresenceColors.colorInt(it.colorHue) }
        }
    }

    fun isAnyoneCreating(): Boolean = synchronized(lock) {
        latest.any { it.detail == "creating" }
    }

    fun whereFor(uid: String): String {
        val p = synchronized(lock) { latest.firstOrNull { it.uid == uid } } ?: return ""
        return describe(p)
    }

    private fun describe(p: MemberPresence): String {
        if (p.detail.startsWith("paint:")) return "paint:" + p.detail.removePrefix("paint:")
        if (p.detail == "creating") return "creating"
        if (p.detail.startsWith("hitbox:")) return "hitbox"
        if (p.spriteId.isEmpty()) return p.tab
        return try {
            val manager = ProjectManager.getInstance()
            val scene = manager.currentlyEditedScene
            val sprite = scene?.spriteList?.firstOrNull { it.spriteId == p.spriteId }
            (sprite?.name ?: "?") + " · " + p.tab
        } catch (e: Exception) {
            p.tab
        }
    }
}
