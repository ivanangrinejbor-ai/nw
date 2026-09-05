package org.catrobat.catroid.collab

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

data class ScriptLock(
    val uid: String = "",
    val name: String = "",
    val colorHue: Float = 0f,
    val at: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "name" to name,
        "colorHue" to colorHue.toDouble(),
        "at" to at
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): ScriptLock? {
            if (map == null) return null
            val atRaw = map["at"]
            val at = when (atRaw) {
                is Number -> atRaw.toLong()
                is Timestamp -> atRaw.toDate().time
                else -> 0L
            }
            return ScriptLock(
                uid = map["uid"] as? String ?: "",
                name = map["name"] as? String ?: "",
                colorHue = (map["colorHue"] as? Number)?.toFloat() ?: 0f,
                at = at
            )
        }
    }
}

object ScriptLockPolicy {
    const val LOCK_TTL_MS = 30000L

    fun isFresh(lock: ScriptLock?, now: Long): Boolean {
        if (lock == null || lock.uid.isEmpty()) return false
        return now - lock.at in 0..LOCK_TTL_MS
    }

    fun canClaim(existing: ScriptLock?, myUid: String, now: Long): Boolean {
        if (existing == null || existing.uid.isEmpty()) return true
        if (existing.uid == myUid) return true
        return !isFresh(existing, now)
    }

    fun lockedByOther(existing: ScriptLock?, myUid: String, now: Long): ScriptLock? {
        if (existing == null || existing.uid.isEmpty()) return null
        if (existing.uid == myUid) return null
        return if (isFresh(existing, now)) existing else null
    }
}

interface ScriptLockBackend {
    fun claim(sessionId: String, scriptId: String, lock: ScriptLock, now: Long, callback: (Boolean) -> Unit)
    fun release(sessionId: String, scriptId: String, myUid: String, callback: (Boolean) -> Unit = {})
    fun listen(sessionId: String, callback: (Map<String, ScriptLock>) -> Unit): Any?
    fun unlisten(handle: Any?)
}

class FirestoreScriptLockBackend : ScriptLockBackend {
    private val tag = "ScriptLockBackend"

    private fun locks(sessionId: String) = FirebaseFirestore.getInstance()
        .collection(CollabSession.ROOT).document(sessionId).collection("locks")

    override fun claim(sessionId: String, scriptId: String, lock: ScriptLock, now: Long, callback: (Boolean) -> Unit) {
        try {
            val doc = locks(sessionId).document(scriptId)
            FirebaseFirestore.getInstance().runTransaction { tx ->
                val existing = ScriptLock.fromMap(tx.get(doc).data)
                if (!ScriptLockPolicy.canClaim(existing, lock.uid, now)) {
                    throw FirebaseFirestoreException("locked", FirebaseFirestoreException.Code.ABORTED)
                }
                val payload = lock.toMap().toMutableMap()
                payload["at"] = FieldValue.serverTimestamp()
                tx.set(doc, payload)
                null
            }.addOnSuccessListener { callback(true) }
                .addOnFailureListener { callback(false) }
        } catch (e: Exception) {
            Log.w(tag, "claim failed", e)
            callback(false)
        }
    }

    override fun release(sessionId: String, scriptId: String, myUid: String, callback: (Boolean) -> Unit) {
        try {
            locks(sessionId).document(scriptId).delete()
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { callback(false) }
        } catch (e: Exception) {
            Log.w(tag, "release failed", e)
            callback(false)
        }
    }

    override fun listen(sessionId: String, callback: (Map<String, ScriptLock>) -> Unit): Any? {
        return try {
            locks(sessionId).addSnapshotListener { snap, error ->
                if (error != null) {
                    if (CollabAccess.isRevoked((error as? FirebaseFirestoreException)?.code)) {
                        callback(emptyMap())
                    }
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                val map = LinkedHashMap<String, ScriptLock>()
                for (doc in snap.documents) {
                    ScriptLock.fromMap(doc.data)?.let { map[doc.id] = it }
                }
                callback(map)
            }
        } catch (e: Exception) {
            Log.w(tag, "listen failed", e)
            null
        }
    }

    override fun unlisten(handle: Any?) {
        try {
            (handle as? ListenerRegistration)?.remove()
        } catch (e: Exception) {
            Log.w(tag, "unlisten failed", e)
        }
    }
}

class FakeScriptLockBackend(var now: Long = 0L) : ScriptLockBackend {
    val docs = LinkedHashMap<String, ScriptLock>()
    private val listeners = ArrayList<(Map<String, ScriptLock>) -> Unit>()
    var claims = 0
    var releases = 0

    private fun emit() {
        val copy = LinkedHashMap(docs)
        for (listener in listeners.toList()) listener(copy)
    }

    override fun claim(sessionId: String, scriptId: String, lock: ScriptLock, now: Long, callback: (Boolean) -> Unit) {
        claims++
        val at = now
        if (!ScriptLockPolicy.canClaim(docs[scriptId], lock.uid, at)) {
            callback(false)
            return
        }
        docs[scriptId] = lock.copy(at = at)
        emit()
        callback(true)
    }

    override fun release(sessionId: String, scriptId: String, myUid: String, callback: (Boolean) -> Unit) {
        releases++
        if (docs[scriptId]?.uid == myUid) {
            docs.remove(scriptId)
            emit()
        }
        callback(true)
    }

    override fun listen(sessionId: String, callback: (Map<String, ScriptLock>) -> Unit): Any? {
        listeners.add(callback)
        callback(LinkedHashMap(docs))
        return callback
    }

    override fun unlisten(handle: Any?) {
        listeners.remove(handle)
    }
}

data class LockIdentity(val uid: String, val name: String, val hue: Float)

interface LockHeartbeat {
    fun start(intervalMs: Long, tick: () -> Unit)
    fun stop()
}

class AndroidLockHeartbeat : LockHeartbeat {
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var runnable: Runnable? = null

    override fun start(intervalMs: Long, tick: () -> Unit) {
        if (runnable != null) return
        val scheduled = object : Runnable {
            override fun run() {
                try {
                    tick()
                } catch (e: Exception) {
                    Log.w("ScriptLockManager", "heartbeat failed", e)
                }
                try {
                    handler.postDelayed(this, intervalMs)
                } catch (e: Exception) {
                    Log.w("ScriptLockManager", "reschedule failed", e)
                }
            }
        }
        runnable = scheduled
        try {
            handler.postDelayed(scheduled, intervalMs)
        } catch (e: Exception) {
            Log.w("ScriptLockManager", "schedule failed", e)
        }
    }

    override fun stop() {
        val current = runnable
        runnable = null
        if (current == null) return
        try {
            handler.removeCallbacks(current)
        } catch (e: Exception) {
            Log.w("ScriptLockManager", "unschedule failed", e)
        }
    }
}

class ManualLockHeartbeat : LockHeartbeat {
    private var tick: (() -> Unit)? = null

    override fun start(intervalMs: Long, tick: () -> Unit) {
        this.tick = tick
    }

    override fun stop() {
        tick = null
    }

    fun fire() {
        tick?.invoke()
    }
}

object ScriptLockManager {
    private const val TAG = "ScriptLockManager"
    const val HEARTBEAT_MS = 10000L

    var backend: ScriptLockBackend = FirestoreScriptLockBackend()
    var nowProvider: () -> Long = { System.currentTimeMillis() }
    var sessionProvider: () -> LockIdentity? = {
        if (!CollabSession.isActive || CollabSession.myUid == null) null
        else LockIdentity(CollabSession.myUid!!, PresenceRenderer.myName, PresenceRenderer.myHue)
    }
    var heartbeatDriver: LockHeartbeat = AndroidLockHeartbeat()

    private val lock = Any()
    private var sessionId: String? = null
    private var listenHandle: Any? = null
    private val held = LinkedHashSet<String>()
    private var known: Map<String, ScriptLock> = emptyMap()
    private val observers = LinkedHashMap<String, () -> Unit>()

    fun addObserver(key: String, callback: () -> Unit) {
        synchronized(lock) { observers[key] = callback }
    }

    fun removeObserver(key: String) {
        synchronized(lock) { observers.remove(key) }
    }

    private fun notifyLocked() {
        val callbacks = synchronized(lock) { observers.values.toList() }
        for (callback in callbacks) {
            try {
                callback.invoke()
            } catch (e: Exception) {
                Log.w(TAG, "notify failed", e)
            }
        }
    }

    private fun ownLock(identity: LockIdentity): ScriptLock {
        return ScriptLock(
            uid = identity.uid,
            name = identity.name,
            colorHue = identity.hue,
            at = nowProvider()
        )
    }

    fun start(sid: String) {
        stop()
        sessionId = sid
        try {
            listenHandle = backend.listen(sid) { map ->
                synchronized(lock) { known = map }
                notifyLocked()
            }
        } catch (e: Exception) {
            Log.w(TAG, "start failed", e)
        }
        heartbeatDriver.start(HEARTBEAT_MS) {
            val currentSid = sessionId
            val identity = sessionProvider()
            if (currentSid != null && identity != null) {
                val mine: List<String>
                synchronized(lock) { mine = held.toList() }
                for (scriptId in mine) {
                    backend.claim(currentSid, scriptId, ownLock(identity), nowProvider(), {})
                }
            }
        }
    }

    fun stop() {
        releaseAllMine()
        try {
            listenHandle?.let { backend.unlisten(it) }
        } catch (e: Exception) {
            Log.w(TAG, "stop failed", e)
        }
        listenHandle = null
        sessionId = null
        heartbeatDriver.stop()
    }

    fun claimMine(scriptId: String): Boolean {
        val sid = sessionId
        val identity = sessionProvider()
        if (sid == null || identity == null) return true
        if (scriptId.isEmpty()) return true
        val existing = synchronized(lock) { known[scriptId] }
        if (!ScriptLockPolicy.canClaim(existing, identity.uid, nowProvider())) return false
        synchronized(lock) { held.add(scriptId) }
        backend.claim(sid, scriptId, ownLock(identity), nowProvider()) { ok ->
            if (!ok) {
                synchronized(lock) { held.remove(scriptId) }
                notifyLocked()
            }
        }
        return true
    }

    fun releaseMine(scriptId: String) {
        val sid = sessionId
        val identity = sessionProvider()
        synchronized(lock) { held.remove(scriptId) }
        if (sid == null || identity == null) return
        backend.release(sid, scriptId, identity.uid)
    }

    fun releaseAllMine() {
        val mine: List<String>
        synchronized(lock) {
            mine = held.toList()
            held.clear()
        }
        val sid = sessionId
        val identity = sessionProvider()
        if (sid == null || identity == null) return
        for (scriptId in mine) {
            backend.release(sid, scriptId, identity.uid)
        }
    }

    fun lockerOf(scriptId: String?): ScriptLock? {
        if (scriptId.isNullOrEmpty()) return null
        val identity = sessionProvider() ?: return null
        val existing = synchronized(lock) { known[scriptId] }
        return ScriptLockPolicy.lockedByOther(existing, identity.uid, nowProvider())
    }

    fun canEdit(scriptId: String?): Boolean {
        if (sessionProvider() == null) return true
        return lockerOf(scriptId) == null
    }

    fun isHeldByMe(scriptId: String): Boolean = synchronized(lock) { held.contains(scriptId) }
}
