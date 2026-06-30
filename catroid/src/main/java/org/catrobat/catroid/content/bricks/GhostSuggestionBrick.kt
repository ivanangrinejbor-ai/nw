package org.catrobat.catroid.content.bricks

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import org.catrobat.catroid.R
import org.catrobat.catroid.codeanalysis.AiProjectAssistant
import org.catrobat.catroid.codeanalysis.Suggestion
import org.catrobat.catroid.content.Script
import java.util.UUID

class GhostSuggestionBrick(
    val targetScript: Script,
    val suggestion: Suggestion,
    val parentBrickId: UUID? = null
) : BrickBaseType() {

    companion object {
        val COMPOSITE_END_MAP: Map<String, () -> Brick> = mapOf(
            "ForeverBrick" to { LoopEndlessBrick() },
            "RepeatBrick" to { LoopEndBrick() },
            "RepeatUntilBrick" to { LoopEndBrick() },
            "AsyncRepeatBrick" to { LoopEndBrick() },
            "IntervalRepeatBrick" to { LoopEndBrick() },
            "ForVariableFromToBrick" to { LoopEndBrick() },
            "ForItemInUserListBrick" to { LoopEndBrick() },
            "IfThenLogicBeginBrick" to { IfThenLogicEndBrick() },
            "IfLogicBeginBrick" to { IfLogicEndBrick() },
            "PhiroIfLogicBeginBrick" to { IfLogicEndBrick() },
            "RaspiIfLogicBeginBrick" to { IfLogicEndBrick() }
        )
    }

    private var isDismissed = false
    private var swipeTranslationX = 0f
    private val swipeThreshold = 200f

    override fun getViewResource(): Int = R.layout.brick_ghost_suggestion

    override fun getView(context: Context): View {
        val v = super.getView(context)

        v.alpha = 0.45f
        v.isEnabled = false

        val typeText = v.findViewById<TextView>(R.id.ghost_brick_type)
        typeText?.text = "+ ${suggestion.brickType}"

        val reasonText = v.findViewById<TextView>(R.id.ghost_reason)
        reasonText?.text = suggestion.reason

        val acceptBtn = v.findViewById<TextView>(R.id.ghost_accept)
        acceptBtn?.setOnClickListener { onAccept(context, v) }

        val dismissBtn = v.findViewById<TextView>(R.id.ghost_dismiss)
        dismissBtn?.setOnClickListener { onDismiss(context, v) }

        setupSwipe(context, v)

        return v
    }

    private fun setupSwipe(context: Context, view: View) {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                if (dx < -swipeThreshold) {
                    view.post { onDismiss(context, view) }
                    return true
                }
                return false
            }
        })

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    override fun addToFlatList(bricks: List<Brick?>?) {
    }

    override fun addActionToSequence(sprite: org.catrobat.catroid.content.Sprite, sequence: org.catrobat.catroid.content.actions.ScriptSequenceAction) {
    }

    override fun getDragAndDropTargetList(): List<Brick>? = null

    fun onAccept(context: Context, view: View) {
        if (isDismissed) return

        val newBrick = AiProjectAssistant.addSuggestedBrick(suggestion.brickType) ?: run {
            Toast.makeText(context, "Could not create ${suggestion.brickType}", Toast.LENGTH_SHORT).show()
            return
        }

        val endBrick = COMPOSITE_END_MAP[suggestion.brickType]?.invoke()
        val bricksToInsert = if (endBrick != null) listOf(newBrick, endBrick) else listOf(newBrick)

        if (parentBrickId != null) {
            targetScript.insertBrickAfter(parentBrickId, 0, bricksToInsert)
        } else {
            for (brick in bricksToInsert) {
                targetScript.addBrick(brick)
            }
        }
        isDismissed = true
        Toast.makeText(context, "Added ${suggestion.brickType}", Toast.LENGTH_SHORT).show()

        view.animate().alpha(0f).setDuration(200).withEndAction {
            view.visibility = View.GONE
        }.start()
    }

    fun onDismiss(context: Context, view: View) {
        if (isDismissed) return
        isDismissed = true

        AiProjectAssistant.rejectSuggestion(suggestion.brickType)

        view.animate()
            .translationX(-view.width.toFloat())
            .alpha(0f)
            .setDuration(250)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }

    fun isActive(): Boolean = !isDismissed

    override fun getScript(): Script? = targetScript

    override fun getParent(): Brick? = null
}
