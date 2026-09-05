package org.catrobat.catroid.collab

import android.content.Context
import org.catrobat.catroid.R
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.utils.ToastUtil

object CollabGuards {
    @JvmStatic
    fun scriptIdOf(brick: Brick?): String {
        return try {
            brick?.script?.scriptId?.toString().orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    @JvmStatic
    fun isLockedByOther(brick: Brick?): Boolean {
        return !ScriptLockManager.canEdit(scriptIdOf(brick).ifEmpty { return false })
    }

    @JvmStatic
    fun lockerName(brick: Brick?): String? {
        return ScriptLockManager.lockerOf(scriptIdOf(brick))?.name
    }

    @JvmStatic
    fun claimForEdit(context: Context?, brick: Brick?): Boolean {
        val scriptId = scriptIdOf(brick)
        if (scriptId.isEmpty()) return true
        if (ScriptLockManager.canEdit(scriptId)) {
            ScriptLockManager.claimMine(scriptId)
            return true
        }
        refuseToast(context, brick)
        return false
    }

    @JvmStatic
    fun firstLockedByOther(bricks: List<Brick>?): Brick? {
        if (bricks == null) return null
        for (brick in bricks) {
            if (isLockedByOther(brick)) return brick
        }
        return null
    }

    @JvmStatic
    fun refuseToast(context: Context?, brick: Brick?) {
        if (context == null) return
        try {
            val name = lockerName(brick) ?: return
            ToastUtil.showError(context, context.getString(R.string.collab_locked_by, name))
        } catch (e: Exception) {
        }
    }
}
