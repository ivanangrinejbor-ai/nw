package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.formulaeditor.UserVariable
import android.util.Log

class TryCatchFinallyAction : Action() {
    companion object {
        // Limits catch block retries to prevent infinite recursion or execution loops if catch throws exceptions
        private const val MAX_CATCH_ITERATIONS = 10
    }

    var trySequence: ScriptSequenceAction? = null
    var catchSequence: ScriptSequenceAction? = null
    var finallySequence: ScriptSequenceAction? = null
    var errorVariable: UserVariable? = null

    private var state: State = State.READY
    private var catchCount = 0

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
                    } catch (e: Throwable) {
                        Log.w("TryCatchFinallyAction", "Exception caught in TRY block: ${e.message}")
                        errorVariable?.value = e.message ?: "An unknown error occurred"
                        state = State.CATCHING
                        continue
                    }
                    return false
                }

                State.CATCHING -> {
                    if (catchCount >= MAX_CATCH_ITERATIONS) {
                        Log.e("TryCatchFinallyAction", "Catch block exceeded max iterations ($MAX_CATCH_ITERATIONS), skipping")
                        state = State.FINALLY
                        continue
                    }
                    catchCount++
                    val cs = catchSequence
                    if (cs == null || cs.actions.size == 0) {
                        state = State.FINALLY
                        continue
                    }
                    try {
                        if (cs.act(delta) == true) {
                            state = State.FINALLY
                            continue
                        }
                    } catch (e: Throwable) {
                        Log.w("TryCatchFinallyAction", "Exception caught in CATCH block: ${e.message}")
                        errorVariable?.value = e.message ?: "An unknown error occurred"
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
                    try {
                        if (fs.act(delta) == true) {
                            state = State.DONE
                            return true
                        }
                    } catch (e: Throwable) {
                        Log.w("TryCatchFinallyAction", "Exception caught in FINALLY block: ${e.message}")
                        errorVariable?.value = e.message ?: "An unknown error occurred"
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
        catchCount = 0
        trySequence?.restart()
        catchSequence?.restart()
        finallySequence?.restart()
        super.restart()
    }
}