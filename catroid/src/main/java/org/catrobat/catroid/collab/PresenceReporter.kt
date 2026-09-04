package org.catrobat.catroid.collab

import android.util.Log
import org.catrobat.catroid.ProjectManager

object PresenceReporter {
    private const val TAG = "PresenceReporter"

    @Volatile private var tab: String = CollabTabs.SPRITES
    @Volatile private var spriteId: String = ""
    @Volatile private var detail: String = ""

    fun reportList() {
        spriteId = ""
        detail = ""
        tab = CollabTabs.SPRITES
        push()
    }

    fun reportSprite(id: String) {
        spriteId = id
        detail = ""
        push()
    }

    fun reportTab(position: Int, scripts: Int, looks: Int, sounds: Int) {
        tab = when (position) {
            scripts -> CollabTabs.SCRIPTS
            looks -> CollabTabs.LOOKS
            sounds -> CollabTabs.SOUNDS
            else -> CollabTabs.SCRIPTS
        }
        push()
    }

    fun reportTabKey(key: String) {
        tab = key
        push()
    }

    fun reportTabFresh(key: String) {
        tab = key
        detail = ""
        push()
    }

    fun reportDetail(value: String) {
        detail = value
        push()
    }

    fun clearDetail() {
        detail = ""
        push()
    }

    private fun push() {
        try {
            if (!CollabSession.isActive) return
            val manager = ProjectManager.getInstance()
            val project = manager.currentProject ?: return
            val scene = manager.currentlyEditedScene ?: return
            val uid = CollabSession.myUid ?: return
            CollabSession.report(
                MemberPresence(
                    uid = uid,
                    name = CollabAuth.savedDisplayName(),
                    colorHue = PresenceRenderer.myHue,
                    role = CollabSession.myRole,
                    sceneId = scene.sceneId,
                    spriteId = spriteId,
                    tab = tab,
                    detail = detail
                )
            )
            if (project.name != CollabSession.projectName && CollabSession.projectName.isNotEmpty()) {
                Log.w(TAG, "session project differs from open project")
            }
        } catch (e: Exception) {
            Log.w(TAG, "report failed", e)
        }
    }
}
