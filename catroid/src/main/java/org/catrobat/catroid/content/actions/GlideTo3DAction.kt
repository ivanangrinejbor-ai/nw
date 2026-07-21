package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.EasingFunctions
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.stage.StageActivity

class GlideTo3DAction : TemporalAction() {
    private var scope: Scope? = null
    private var objectId: Formula? = null
    private var xFormula: Formula? = null
    private var yFormula: Formula? = null
    private var zFormula: Formula? = null
    private var durationFormula: Formula? = null

    // Spinner index into R.array.brick_easing_types ("None" = 0, "Linear" = 1, ...).
    private var typeIndex = 0

    private var startX = 0f
    private var startY = 0f
    private var startZ = 0f
    private var targetX = 0f
    private var targetY = 0f
    private var targetZ = 0f

    private val easingTypes = EasingFunctions.EasingType.entries.toTypedArray()

    override fun begin() {
        try {
            duration = durationFormula?.interpretFloat(scope) ?: 1f

            targetX = xFormula?.interpretFloat(scope) ?: 0f
            targetY = yFormula?.interpretFloat(scope) ?: 0f
            targetZ = zFormula?.interpretFloat(scope) ?: 0f

            startX = targetX
            startY = targetY
            startZ = targetZ

            val stageListener = StageActivity.getActiveStageListener()
            val idStr = objectId?.interpretString(scope)
            if (stageListener != null && !idStr.isNullOrEmpty()) {
                val gameObject = stageListener.sceneManager?.findGameObject(idStr)
                if (gameObject != null) {
                    startX = gameObject.transform.position.x
                    startY = gameObject.transform.position.y
                    startZ = gameObject.transform.position.z
                } else {
                    val pos = stageListener.threeDManager?.getPosition(idStr)
                    if (pos != null) {
                        startX = pos.x
                        startY = pos.y
                        startZ = pos.z
                    }
                }
            }
        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Formula interpretation failed in GlideTo3DAction.", e)
            duration = 0f
        }
        super.begin()
    }

    override fun update(percent: Float) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val sceneMgr = stageListener.sceneManager
        val threeD = stageListener.threeDManager ?: return

        val idStr = objectId?.interpretString(scope) ?: return
        if (idStr.isEmpty()) return

        val currentTime = time.coerceAtMost(duration)

        // Spinner index is offset by 1 because entry 0 is "None"; map both "None" and
        // "Linear" to EasingType.LINEAR (index 0), everything else to typeIndex - 1.
        val enumIndex = (typeIndex - 1).coerceIn(0, easingTypes.size - 1)
        val easingType = easingTypes[enumIndex]

        val newX = EasingFunctions.calculate(easingType, currentTime, duration, startX, targetX)
        val newY = EasingFunctions.calculate(easingType, currentTime, duration, startY, targetY)
        val newZ = EasingFunctions.calculate(easingType, currentTime, duration, startZ, targetZ)

        val gameObject = sceneMgr?.findGameObject(idStr)
        if (gameObject != null) {
            gameObject.transform.position.x = newX
            gameObject.transform.position.y = newY
            gameObject.transform.position.z = newZ
        } else {
            threeD.setPosition(idStr, newX, newY, newZ)
        }
    }

    override fun reset() {
        super.reset()
        scope = null
        objectId = null
        xFormula = null
        yFormula = null
        zFormula = null
        durationFormula = null
        typeIndex = 0
    }

    fun setScope(scope: Scope?) { this.scope = scope }
    fun setObjectId(formula: Formula?) { this.objectId = formula }
    fun setXFormula(formula: Formula?) { this.xFormula = formula }
    fun setYFormula(formula: Formula?) { this.yFormula = formula }
    fun setZFormula(formula: Formula?) { this.zFormula = formula }
    fun setDurationFormula(formula: Formula?) { this.durationFormula = formula }
    fun setTypeIndex(index: Int) { this.typeIndex = index }
}
