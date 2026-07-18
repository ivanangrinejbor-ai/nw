package org.catrobat.catroid.content

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tracks in-memory mutations made by NeoScript runtime bricks (CreateObjectBrick,
 * AssignScriptsBrick, ImportScriptBrick) during a Stage session.
 *
 * Two flags are maintained:
 *  - [hasPersistentMutations] — at least one mutation with persist=true was performed.
 *    The canonical project was (or will be) saved to disk by the action itself.
 *  - [hasTemporaryMutations] — at least one mutation with persist=false was performed.
 *    These changes exist ONLY in the in-memory Project model and must be discarded
 *    when the Stage session ends by reloading the project from disk.
 *
 * Usage:
 *   - Set flags from runtime actions (CreateObjectAction, AssignScriptsAction, ImportScriptAction).
 *   - Call [reset] at the start of every Stage session (StageLifeCycleController.stageCreate).
 *   - Check [needsReload] in editor activities after returning from Stage.
 */
object RuntimeMutationTracker {

    private val _hasPersistentMutations = AtomicBoolean(false)
    private val _hasTemporaryMutations = AtomicBoolean(false)

    /** True if any persist=1 NeoScript mutation happened this Stage session. */
    var hasPersistentMutations: Boolean
        get() = _hasPersistentMutations.get()
        set(value) = _hasPersistentMutations.set(value)

    /** True if any persist=0 NeoScript mutation happened this Stage session. */
    var hasTemporaryMutations: Boolean
        get() = _hasTemporaryMutations.get()
        set(value) = _hasTemporaryMutations.set(value)

    /**
     * Returns true if the editor should reload the project from disk after this Stage session.
     * This is true whenever temporary mutations occurred (they pollute the in-memory model).
     * When only persistent mutations happened, the project was already saved — a reload is still
     * safe and ensures the editor shows exactly what is on disk.
     */
    val needsReload: Boolean
        get() = _hasPersistentMutations.get() || _hasTemporaryMutations.get()

    /**
     * Resets both mutation flags. Must be called at the start of every Stage session so that
     * mutations from a previous session do not trigger an unnecessary reload.
     */
    fun reset() {
        _hasPersistentMutations.set(false)
        _hasTemporaryMutations.set(false)
    }
}
