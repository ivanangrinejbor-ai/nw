package org.catrobat.catroid.content

import java.util.concurrent.atomic.AtomicBoolean

object RuntimeMutationTracker {

    private val _hasPersistentMutations = AtomicBoolean(false)
    private val _hasTemporaryMutations = AtomicBoolean(false)

    var hasPersistentMutations: Boolean
        get() = _hasPersistentMutations.get()
        set(value) = _hasPersistentMutations.set(value)

    var hasTemporaryMutations: Boolean
        get() = _hasTemporaryMutations.get()
        set(value) = _hasTemporaryMutations.set(value)

    val needsReload: Boolean
        get() = _hasPersistentMutations.get() || _hasTemporaryMutations.get()

    fun reset() {
        _hasPersistentMutations.set(false)
        _hasTemporaryMutations.set(false)
    }
}
