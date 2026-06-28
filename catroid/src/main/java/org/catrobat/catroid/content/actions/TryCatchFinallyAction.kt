package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.formulaeditor.UserVariable
import android.util.Log

class TryCatchFinallyAction : Action() {
    var trySequence: ScriptSequenceAction? = null
    var catchSequence: ScriptSequenceAction? = null
    var finallySequence: ScriptSequenceAction? = null
    var errorVariable: UserVariable? = null

    private var state: State = State.READY

    private enum class State {
        READY, TRYING, CATCHING, FINALLY, DONE
    }

    override fun act(delta: Float): Boolean {
        while (true) {
            when (state) {
                State.READY -> {
                    state = State.TRYING
                }

                State.TRYING -> {
                    try {
                        if (trySequence?.act(delta) == true) {
                            state = State.FINALLY
                            continue
                        }
                    } catch (e: Exception) {
                        Log.w("TryCatchFinallyAction", "Exception caught in TRY block: ${e.message}")
                        errorVariable?.value = e.message ?: "An unknown error occurred"
                        state = State.CATCHING
                        continue
                    }
                    return false
                }

                State.CATCHING -> {
                    val cs = catchSequence
                    if (cs == null || cs.actions.size == 0) {
                        state = State.FINALLY
                        continue
                    }
                    if (cs.act(delta) == true) {
                        state = State.FINALLY
                        continue
                    }
                    return false
                }

                State.FINALLY -> {
                    val fs = finallySequence
                    if (fs == null || fs.actions.size == 0) {
                        state = State.DONE
                        return true
                    }
                    if (fs.act(delta) == true) {
                        state = State.DONE
                        return true
                    }
                    return false
                }

                State.DONE -> {
                    return true
                }
            }
        }
    }

    override fun restart() {
        state = State.READY
        trySequence?.restart()
        catchSequence?.restart()
        finallySequence?.restart()
        super.restart()
    }
}