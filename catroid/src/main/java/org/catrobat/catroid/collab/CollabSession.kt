package org.catrobat.catroid.collab

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.atomic.AtomicBoolean

object CollabSession {
    const val ROOT = "collabSessions"
    const val HEARTBEAT_MS = 5000L
    const val PRESENCE_TTL_MS = 20000L
    const val INVITE_TTL_MS = 24L * 60L * 60L * 1000L
    private const val TAG = "CollabSession"
    private const val SID_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    @Volatile var sessionId: String? = null
        private set
    @Volatile var myUid: String? = null
        private set
    @Volatile var myRole: String = CollabRoles.VIEWER
    @Volatile var isHost: Boolean = false
    @Volatile var projectName: String = ""
    @Volatile var foregroundActivities = 0

    var onMembersChanged: ((Map<String, CollabMember>) -> Unit)? = null
    var onPresenceChanged: ((List<MemberPresence>) -> Unit)? = null
    var onRequestsChanged: ((Map<String, CollabRequest>) -> Unit)? = null
    var onMetaChanged: ((CollabMeta) -> Unit)? = null

    val isActive: Boolean get() = sessionId != null && myUid != null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lifecycleHooked = AtomicBoolean(false)
    private var lastPresence: MemberPresence? = null
    @Volatile private var cachedDb: FirebaseFirestore? = null

    private var metaReg: ListenerRegistration? = null
    private var membersReg: ListenerRegistration? = null
    private var presenceReg: ListenerRegistration? = null
    private var requestsReg: ListenerRegistration? = null
    private var approvalReg: ListenerRegistration? = null

    private val heartbeat = object : Runnable {
        override fun run() {
            try {
                val presence = lastPresence
                if (isActive && foregroundActivities > 0 && presence != null) {
                    writePresence(presence)
                }
            } catch (e: Exception) {
                Log.w(TAG, "heartbeat failed", e)
            }
            mainHandler.postDelayed(this, HEARTBEAT_MS)
        }
    }

    fun initOnce(context: Context) {
        if (!lifecycleHooked.compareAndSet(false, true)) return
        try {
            val app = context.applicationContext as? Application ?: return
            app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    foregroundActivities++
                }

                override fun onActivityStopped(activity: Activity) {
                    if (foregroundActivities > 0) foregroundActivities--
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
            mainHandler.postDelayed(heartbeat, HEARTBEAT_MS)
            if (onPresenceChanged == null) {
                onPresenceChanged = { list -> PresenceRenderer.ingest(list) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "lifecycle hook failed", e)
        }
    }

    private fun db(): FirebaseFirestore? {
        cachedDb?.let { return it }
        return try {
            FirebaseFirestore.getInstance().also { cachedDb = it }
        } catch (e: Exception) {
            Log.w(TAG, "firestore unavailable", e)
            null
        }
    }

    private fun metaRef(sid: String) = db()?.collection(ROOT)?.document(sid)
    private fun membersRef(sid: String) = metaRef(sid)?.collection("members")
    private fun presenceRef(sid: String) = metaRef(sid)?.collection("presence")
    private fun invitesRef(sid: String) = metaRef(sid)?.collection("invites")
    private fun requestsRef(sid: String) = metaRef(sid)?.collection("requests")

    fun randomSessionId(): String {
        return (1..6).map { SID_ALPHABET.random() }.joinToString("")
    }

    fun randomInviteCode(): String {
        return (1..6).map { ('0'..'9').random() }.joinToString("")
    }

    fun createSession(projectName: String, displayName: String, hue: Float, callback: (String?, String?) -> Unit) {
        CollabAuth.ensureSignedIn { uid ->
            val database = db()
            if (uid == null || database == null) {
                callback(null, null)
                return@ensureSignedIn
            }
            val sid = randomSessionId()
            val code = randomInviteCode()
            val now = System.currentTimeMillis()
            metaRef(sid)!!.document("meta").set(CollabMeta(
                ownerUid = uid, ownerName = displayName, projectName = projectName,
                createdAt = now
            ).toMap()).addOnSuccessListener {
                val batch = database.batch()
                batch.set(membersRef(sid)!!.document(uid), CollabMember(
                    role = CollabRoles.HOST, colorHue = hue, name = displayName, joinedAt = now
                ).toMap())
                batch.set(invitesRef(sid)!!.document(code), CollabInvite(
                    role = CollabRoles.EDITOR, expiresAt = now + INVITE_TTL_MS
                ).toMap())
                batch.commit()
                    .addOnSuccessListener {
                        enterLocalState(sid, uid, projectName)
                        isHost = true
                        myRole = CollabRoles.HOST
                        startListeners()
                        callback(sid, code)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "create failed", e)
                        callback(null, null)
                    }
            }.addOnFailureListener { e ->
                Log.w(TAG, "create failed", e)
                callback(null, null)
            }
        }
    }

    fun createInvite(role: String, callback: (String?) -> Unit) {
        val sid = sessionId
        val database = db()
        if (sid == null || database == null || !isHost) {
            callback(null)
            return
        }
        val code = randomInviteCode()
        invitesRef(sid)!!.document(code)
            .set(CollabInvite(role = role, expiresAt = System.currentTimeMillis() + INVITE_TTL_MS).toMap())
            .addOnSuccessListener { callback(code) }
            .addOnFailureListener { e ->
                Log.w(TAG, "invite failed", e)
                callback(null)
            }
    }

    fun claimInvite(sid: String, code: String, displayName: String, hue: Float, callback: (Boolean) -> Unit) {
        CollabAuth.ensureSignedIn { uid ->
            val database = db()
            if (uid == null || database == null) {
                callback(false)
                return@ensureSignedIn
            }
            val inviteDoc = invitesRef(sid)!!.document(code)
            val requestDoc = requestsRef(sid)!!.document(uid)
            database.runTransaction { tx ->
                val snap = tx.get(inviteDoc)
                val invite = CollabInvite.fromMap(snap.data)
                if (invite == null || invite.usedBy.isNotEmpty() || invite.expiresAt < System.currentTimeMillis()) {
                    throw FirebaseFirestoreException("invite invalid", FirebaseFirestoreException.Code.ABORTED)
                }
                tx.update(inviteDoc, "usedBy", uid)
                tx.set(requestDoc, CollabRequest(
                    name = displayName, colorHue = hue, at = System.currentTimeMillis()
                ).toMap())
                null
            }.addOnSuccessListener {
                enterLocalState(sid, uid, "")
                myRole = CollabRoles.VIEWER
                callback(true)
            }.addOnFailureListener { e ->
                Log.w(TAG, "claim failed", e)
                callback(false)
            }
        }
    }

    fun awaitApproval(timeoutMs: Long, callback: (String?) -> Unit) {
        val sid = sessionId
        val uid = myUid
        val database = db()
        if (sid == null || uid == null || database == null) {
            callback(null)
            return
        }
        var done = false
        val timeout = Runnable {
            if (!done) {
                done = true
                try {
                    approvalReg?.remove()
                } catch (e: Exception) {
                }
                approvalReg = null
                callback(null)
            }
        }
        mainHandler.postDelayed(timeout, timeoutMs)
        approvalReg = membersRef(sid)!!.document(uid).addSnapshotListener { snap, error ->
            if (done || error != null) return@addSnapshotListener
            if (snap != null && snap.exists()) {
                done = true
                mainHandler.removeCallbacks(timeout)
                try {
                    approvalReg?.remove()
                } catch (e: Exception) {
                }
                approvalReg = null
                val member = CollabMember.fromMap(snap.data)
                myRole = member.role
                startListeners()
                callback(member.role)
            }
        }
    }

    fun approveRequest(uid: String, request: CollabRequest, role: String, callback: (Boolean) -> Unit = {}) {
        val sid = sessionId
        val database = db()
        if (sid == null || database == null || !isHost) {
            callback(false)
            return
        }
        val batch = database.batch()
        batch.set(membersRef(sid)!!.document(uid), CollabMember(
            role = role, colorHue = request.colorHue, name = request.name,
            joinedAt = System.currentTimeMillis()
        ).toMap())
        batch.delete(requestsRef(sid)!!.document(uid))
        batch.commit()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Log.w(TAG, "approve failed", e)
                callback(false)
            }
    }

    fun rejectRequest(uid: String) {
        val sid = sessionId
        if (sid == null || !isHost) return
        try {
            requestsRef(sid)?.document(uid)?.delete()
        } catch (e: Exception) {
            Log.w(TAG, "reject failed", e)
        }
    }

    fun kick(uid: String) {
        val sid = sessionId
        if (sid == null || !isHost || uid == myUid) return
        try {
            membersRef(sid)?.document(uid)?.delete()
            presenceRef(sid)?.document(uid)?.delete()
        } catch (e: Exception) {
            Log.w(TAG, "kick failed", e)
        }
    }

    fun setClosed(closed: Boolean) {
        val sid = sessionId
        if (sid == null || !isHost) return
        try {
            metaRef(sid)?.document("meta")?.update("closed", closed)
        } catch (e: Exception) {
            Log.w(TAG, "close failed", e)
        }
    }

    fun report(presence: MemberPresence) {
        if (!isActive) return
        lastPresence = presence
        if (foregroundActivities > 0) writePresence(presence)
    }

    private fun writePresence(presence: MemberPresence) {
        try {
            val sid = sessionId
            val uid = myUid
            if (sid == null || uid == null) return
            val payload = presence.toMap().toMutableMap()
            payload["updatedAt"] = FieldValue.serverTimestamp()
            presenceRef(sid)?.document(uid)?.set(payload)
        } catch (e: Exception) {
            Log.w(TAG, "presence write failed", e)
        }
    }

    fun startListeners() {
        val sid = sessionId
        val database = db()
        if (sid == null || database == null) return
        stopListeners()
        metaReg = metaRef(sid)?.document("meta")?.addSnapshotListener { snap, error ->
            if (error != null || snap == null || !snap.exists()) return@addSnapshotListener
            val meta = CollabMeta.fromMap(snap.data)
            if (projectName.isEmpty()) projectName = meta.projectName
            onMetaChanged?.invoke(meta)
        }
        membersReg = membersRef(sid)?.addSnapshotListener { snap, error ->
            if (error != null || snap == null) return@addSnapshotListener
            val map = LinkedHashMap<String, CollabMember>()
            for (doc in snap.documents) map[doc.id] = CollabMember.fromMap(doc.data)
            if (myUid != null && map.containsKey(myUid)) myRole = map[myUid]?.role ?: myRole
            onMembersChanged?.invoke(map)
        }
        presenceReg = presenceRef(sid)?.addSnapshotListener { snap, error ->
            if (error != null || snap == null) return@addSnapshotListener
            val now = System.currentTimeMillis()
            val list = ArrayList<MemberPresence>()
            for (doc in snap.documents) {
                if (doc.id == myUid) continue
                val ts = try {
                    doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
                if (ts > 0 && !PresenceFreshness.isFresh(ts, now, PRESENCE_TTL_MS)) continue
                list.add(MemberPresence.fromSnapshot(doc.id, doc.data, ts))
            }
            onPresenceChanged?.invoke(list)
        }
        if (isHost) {
            requestsReg = requestsRef(sid)?.addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                val map = LinkedHashMap<String, CollabRequest>()
                for (doc in snap.documents) {
                    CollabRequest.fromMap(doc.data)?.let { map[doc.id] = it }
                }
                onRequestsChanged?.invoke(map)
            }
        }
    }

    fun leave() {
        try {
            val sid = sessionId
            val uid = myUid
            if (sid != null && uid != null) {
                presenceRef(sid)?.document(uid)?.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "leave cleanup failed", e)
        }
        stopListeners()
        try {
            approvalReg?.remove()
        } catch (e: Exception) {
        }
        approvalReg = null
        sessionId = null
        myUid = null
        myRole = CollabRoles.VIEWER
        isHost = false
        projectName = ""
        lastPresence = null
        PresenceRenderer.clear()
    }

    private fun enterLocalState(sid: String, uid: String, project: String) {
        sessionId = sid
        myUid = uid
        projectName = project
    }

    private fun stopListeners() {
        for (reg in listOf(metaReg, membersReg, presenceReg, requestsReg)) {
            try {
                reg?.remove()
            } catch (e: Exception) {
            }
        }
        metaReg = null
        membersReg = null
        presenceReg = null
        requestsReg = null
    }
}
