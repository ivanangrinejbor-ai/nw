package org.catrobat.catroid.collab

import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.R
import org.catrobat.catroid.utils.ToastUtil

class CollabDialog(
    private val activity: AppCompatActivity,
    private val projectName: String
) {
    private var dialog: AlertDialog? = null
    private var root: LinearLayout? = null
    private var membersBox: LinearLayout? = null
    private var requestsBox: LinearLayout? = null
    private var requestsHeader: TextView? = null
    private var statusView: TextView? = null
    private var shownCode: String? = null

    private var members: Map<String, CollabMember> = emptyMap()
    private var requests: Map<String, CollabRequest> = emptyMap()
    private var meta: CollabMeta? = null

    fun show() {
        CollabSession.initOnce(activity.applicationContext)
        build()
        dialog?.show()
        attachCallbacks()
        if (!CollabSession.isActive) {
            CollabSession.restoreSession {
                activity.runOnUiThread { refresh() }
            }
        }
        refresh()
    }

    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }

    private fun build() {
        val scroll = ScrollView(activity)
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        scroll.addView(container)
        root = container
        statusView = TextView(activity).apply { container.addView(this) }
        membersBox = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            container.addView(this)
        }
        requestsHeader = TextView(activity).apply {
            text = activity.getString(R.string.collab_requests)
            visibility = View.GONE
            container.addView(this)
        }
        requestsBox = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            container.addView(this)
        }
        dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.collab_title)
            .setView(scroll)
            .setNeutralButton(R.string.collab_close, null)
            .create()
        dialog?.setOnDismissListener {
            CollabSession.onMembersChanged = null
            CollabSession.onRequestsChanged = null
            CollabSession.onMetaChanged = null
            CollabSession.onAccessRevoked = null
            PresenceRenderer.removeObserver(OBSERVER_KEY)
        }
    }

    private fun attachCallbacks() {
        CollabSession.onMembersChanged = { map ->
            activity.runOnUiThread {
                members = map
                refreshLists()
            }
        }
        CollabSession.onRequestsChanged = { map ->
            activity.runOnUiThread {
                requests = map
                refreshLists()
            }
        }
        CollabSession.onMetaChanged = { meta ->
            activity.runOnUiThread {
                this.meta = meta
                refreshLists()
            }
        }
        CollabSession.onAccessRevoked = {
            activity.runOnUiThread { refresh() }
        }
        PresenceRenderer.addObserver(OBSERVER_KEY) {
            activity.runOnUiThread { refreshLists() }
        }
    }

    private fun refresh() {
        val container = root ?: return
        container.removeAllViews()
        statusView = TextView(activity).apply { container.addView(this) }
        if (!CollabSession.isActive) {
            buildInactive(container)
        } else {
            buildActive(container)
        }
        membersBox = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            container.addView(this)
        }
        requestsHeader = TextView(activity).apply {
            text = activity.getString(R.string.collab_requests)
            visibility = View.GONE
            container.addView(this)
        }
        requestsBox = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            container.addView(this)
        }
        refreshLists()
    }

    private fun buildInactive(container: LinearLayout) {
        val nameInput = EditText(activity).apply {
            hint = activity.getString(R.string.collab_name_hint)
            setText(CollabAuth.savedDisplayName())
            inputType = InputType.TYPE_CLASS_TEXT
            container.addView(this)
        }
        val sessionInput = EditText(activity).apply {
            hint = activity.getString(R.string.collab_session_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            container.addView(this)
        }
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            container.addView(this)
        }
        row.addView(Button(activity).apply {
            text = activity.getString(R.string.collab_create)
            setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    ToastUtil.showError(activity, R.string.collab_name_hint)
                    return@setOnClickListener
                }
                startCreate(name)
            }
        })
        row.addView(Button(activity).apply {
            text = activity.getString(R.string.collab_join)
            setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    ToastUtil.showError(activity, R.string.collab_name_hint)
                    return@setOnClickListener
                }
                startJoin(name, sessionInput.text.toString())
            }
        })
        statusView?.text = activity.getString(R.string.collab_offline)
    }

    private fun buildActive(container: LinearLayout) {
        val sid = CollabSession.sessionId ?: ""
        val info = StringBuilder()
        info.append(activity.getString(R.string.collab_session)).append(": ").append(sid)
        info.append("\n").append(activity.getString(R.string.collab_role)).append(": ")
            .append(roleLabel(CollabSession.myRole))
        val currentMeta = meta
        if (currentMeta != null && currentMeta.projectName != projectName) {
            info.append("\n").append(activity.getString(R.string.collab_project_mismatch))
        }
        if (shownCode != null && CollabSession.isHost) {
            info.append("\n").append(activity.getString(R.string.collab_code)).append(": ")
                .append(sid).append("-").append(shownCode)
        }
        statusView?.text = info.toString()
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            container.addView(this)
        }
        if (CollabSession.isHost) {
            row.addView(Button(activity).apply {
                text = activity.getString(R.string.collab_new_code)
                setOnClickListener {
                    CollabSession.createInvite(CollabRoles.EDITOR) { code ->
                        activity.runOnUiThread {
                            if (code == null) {
                                ToastUtil.showError(activity, R.string.collab_no_connection)
                            } else {
                                shownCode = code
                                refresh()
                            }
                        }
                    }
                }
            })
            row.addView(Button(activity).apply {
                val closed = meta?.closed == true
                text = activity.getString(if (closed) R.string.collab_open_room else R.string.collab_close_room)
                setOnClickListener { CollabSession.setClosed(!closed) }
            })
        }
        row.addView(Button(activity).apply {
            text = activity.getString(R.string.collab_leave)
            setOnClickListener {
                CollabSession.leave()
                shownCode = null
                refresh()
            }
        })
    }

    private fun startCreate(name: String) {
        CollabAuth.saveDisplayName(name)
        val hue = PresenceColors.hueFor(emptyList())
        PresenceRenderer.myHue = hue
        PresenceRenderer.myName = name
        statusView?.text = activity.getString(R.string.collab_connecting)
        CollabSession.createSession(projectName, name, hue) { sid, code ->
            activity.runOnUiThread {
                if (sid == null) {
                    ToastUtil.showError(activity, R.string.collab_no_connection)
                    refresh()
                } else {
                    shownCode = code
                    attachCallbacks()
                    CollabSession.startListeners()
                    refresh()
                }
            }
        }
    }

    private fun startJoin(name: String, raw: String) {
        val parts = raw.trim().uppercase().split("-")
        if (parts.size != 2 || !CollabCodes.isValidSessionId(parts[0]) || !CollabCodes.isValidInviteCode(parts[1])) {
            ToastUtil.showError(activity, R.string.collab_bad_code)
            return
        }
        CollabAuth.saveDisplayName(name)
        val hue = PresenceColors.hueFor(emptyList())
        PresenceRenderer.myHue = hue
        PresenceRenderer.myName = name
        statusView?.text = activity.getString(R.string.collab_connecting)
        CollabSession.claimInvite(parts[0], parts[1], name, hue) { ok ->
            activity.runOnUiThread {
                if (!ok) {
                    ToastUtil.showError(activity, R.string.collab_bad_code)
                    refresh()
                    return@runOnUiThread
                }
                CollabSession.awaitApproval(90000L) { role ->
                    activity.runOnUiThread {
                        if (role == null) {
                            ToastUtil.showError(activity, R.string.collab_no_connection)
                            CollabSession.leave()
                        } else {
                            ToastUtil.showSuccess(activity, R.string.collab_joined)
                            attachCallbacks()
                        }
                        refresh()
                    }
                }
            }
        }
    }

    private fun refreshLists() {
        if (dialog?.isShowing != true) return
        membersBox?.removeAllViews()
        requestsBox?.removeAllViews()
        val box = membersBox ?: return
        box.addView(TextView(activity).apply { text = activity.getString(R.string.collab_members) })
        addMemberRow(box, PresenceColors.colorInt(PresenceRenderer.myHue), memberTitle(PresenceRenderer.myName, null, true), null)
        val myUid = CollabSession.myUid
        for ((uid, member) in members.entries.sortedBy { it.value.name }) {
            if (uid == myUid) continue
            addMemberRow(
                box,
                PresenceColors.colorInt(member.colorHue),
                memberTitle(member.name, member.role, false) + whereSuffix(uid),
                if (CollabSession.isHost) uid else null
            )
        }
        val reqBox = requestsBox ?: return
        val isHost = CollabSession.isHost
        requestsHeader?.visibility = if (isHost && requests.isNotEmpty()) View.VISIBLE else View.GONE
        if (!isHost) return
        for ((uid, req) in requests) {
            val line = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            line.addView(TextView(activity).apply {
                text = req.name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            line.addView(Button(activity).apply {
                text = activity.getString(R.string.collab_approve_editor)
                setOnClickListener { CollabSession.approveRequest(uid, req, CollabRoles.EDITOR) }
            })
            line.addView(Button(activity).apply {
                text = activity.getString(R.string.collab_approve_viewer)
                setOnClickListener { CollabSession.approveRequest(uid, req, CollabRoles.VIEWER) }
            })
            reqBox.addView(line)
        }
    }

    private fun memberTitle(name: String, role: String?, isSelf: Boolean): String {
        val label = StringBuilder(if (name.isEmpty()) "?" else name)
        if (isSelf) {
            label.append(" (").append(activity.getString(R.string.collab_you)).append(")")
        } else if (role != null) {
            label.append(" · ").append(roleLabel(role))
        }
        return label.toString()
    }

    private fun whereSuffix(uid: String): String {
        val where = PresenceRenderer.whereFor(uid)
        return if (where.isEmpty()) "" else " → $where"
    }

    private fun addMemberRow(box: LinearLayout, color: Int, title: String, kickUid: String?) {
        val line = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        line.addView(View(activity).apply {
            val size = dp(14)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        })
        line.addView(TextView(activity).apply {
            text = title
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (kickUid != null) {
            line.addView(Button(activity).apply {
                text = "×"
                setOnClickListener { CollabSession.kick(kickUid) }
            })
        }
        box.addView(line)
    }

    private fun roleLabel(role: String): String {
        return when (role) {
            CollabRoles.HOST -> activity.getString(R.string.collab_role_host)
            CollabRoles.EDITOR -> activity.getString(R.string.collab_role_editor)
            else -> activity.getString(R.string.collab_role_viewer)
        }
    }

    companion object {
        private const val OBSERVER_KEY = "collab_dialog"
    }
}
