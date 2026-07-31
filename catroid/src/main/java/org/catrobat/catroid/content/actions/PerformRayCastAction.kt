package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class PerformRayCastAction : TemporalAction() {
    private var scope: Scope? = null
    private var rayId: Formula? = null
    private var startX: Formula? = null
    private var startY: Formula? = null
    private var endX: Formula? = null
    private var endY: Formula? = null

    override fun update(percent: Float) {
        if (scope == null) return
        val id: String
        val sX: Float
        val sY: Float
        val eX: Float
        val eY: Float
        try {
            id = rayId?.interpretString(scope) ?: return
            if (id.isEmpty()) return
            sX = startX?.interpretFloat(scope) ?: 0f
            sY = startY?.interpretFloat(scope) ?: 0f
            eX = endX?.interpretFloat(scope) ?: 0f
            eY = endY?.interpretFloat(scope) ?: 0f
        } catch (e: Exception) {
            Log.w(javaClass.simpleName, "Formula interpretation failed", e)
            return
        }

        if (sX == eX && sY == eY) {
            Log.w(javaClass.simpleName, "Zero-length ray (start==end), skipping")
            return
        }

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return

        scene.physicsWorld.performRayCast(id, Vector2(sX, sY), Vector2(eX, eY))
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setRayId(rayId: Formula?) {
        this.rayId = rayId
    }

    fun setStartX(startX: Formula?) {
        this.startX = startX
    }

    fun setStartY(startY: Formula?) {
        this.startY = startY
    }

    fun setEndX(endX: Formula?) {
        this.endX = endX
    }

    fun setEndY(endY: Formula?) {
        this.endY = endY
    }
}