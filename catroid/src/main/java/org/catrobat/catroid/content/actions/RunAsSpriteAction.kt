package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class RunAsSpriteAction(
    private val script: Script,
    private val nestedBricks: List<Brick>
) : Action() {

    var scope: Scope? = null
    var spriteName: Formula? = null

    private var executed = false
    
    companion object {
        // Recursion depth limit to prevent StackOverflowError
        private val recursionDepth = ThreadLocal.withInitial { 0 }
        private const val MAX_RECURSION_DEPTH = 10
    }

    override fun act(delta: Float): Boolean {
        if (executed) {
            return true
        }
        executed = true

        val targetName = spriteName?.interpretString(scope) ?: return true
        val stageListener = StageActivity.getActiveStageListener() ?: return true

        val targetSprites = stageListener.spritesFromStage.filter { sprite ->
            sprite.name == targetName
        }
        
        // Check recursion depth
        val currentDepth = recursionDepth.get()
        if (currentDepth >= MAX_RECURSION_DEPTH) {
            android.util.Log.w("RunAsSpriteAction", "Max recursion depth ($MAX_RECURSION_DEPTH) exceeded, stopping")
            return true
        }

        try {
            recursionDepth.set(currentDepth + 1)
            
            for (targetSprite in targetSprites) {
                val targetSequence = targetSprite.createSequenceAction(script)

                for (brick in nestedBricks) {
                    if (!brick.isCommentedOut) {
                        brick.addActionToSequence(targetSprite, targetSequence)
                    }
                }

                targetSprite.look.addAction(targetSequence)
            }
        } finally {
            recursionDepth.set(currentDepth)
        }

        return true
    }

    override fun restart() {
        executed = false
        super.restart()
    }
}
