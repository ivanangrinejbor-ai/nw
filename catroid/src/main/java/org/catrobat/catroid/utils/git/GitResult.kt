package org.catrobat.catroid.utils.git

import org.catrobat.catroid.content.Project

sealed class GitResult<out T> {
    data class Success<T>(val data: T) : GitResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : GitResult<Nothing>()
    data class MergeConflict(val conflicts: List<Conflict>) : GitResult<Nothing>()
}

data class MergeResult(
    val mergedProject: Project,
    val conflicts: List<Conflict>
)

class MergeConflictException(val conflicts: List<Conflict>) : Exception("Merge conflicts detected")

data class Conflict(
    val path: String,
    val fieldName: String,
    val baseValue: Any?,
    val localValue: Any?,
    val remoteValue: Any?
)

data class MergeResultData(
    val mergedProject: Project,
    val conflicts: List<Conflict>
)