package org.catrobat.catroid.collab

object CollabRoles {
    const val HOST = "host"
    const val EDITOR = "editor"
    const val VIEWER = "viewer"
}

object CollabTabs {
    const val SPRITES = "sprites"
    const val SCRIPTS = "scripts"
    const val LOOKS = "looks"
    const val SOUNDS = "sounds"
}

data class CollabMeta(
    val ownerUid: String = "",
    val ownerName: String = "",
    val projectName: String = "",
    val repoOwner: String = "",
    val repoName: String = "",
    val closed: Boolean = false,
    val createdAt: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "ownerUid" to ownerUid,
        "ownerName" to ownerName,
        "projectName" to projectName,
        "repoOwner" to repoOwner,
        "repoName" to repoName,
        "closed" to closed,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): CollabMeta {
            if (map == null) return CollabMeta()
            return CollabMeta(
                ownerUid = map["ownerUid"] as? String ?: "",
                ownerName = map["ownerName"] as? String ?: "",
                projectName = map["projectName"] as? String ?: "",
                repoOwner = map["repoOwner"] as? String ?: "",
                repoName = map["repoName"] as? String ?: "",
                closed = map["closed"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

data class CollabMember(
    val role: String = CollabRoles.VIEWER,
    val githubUsername: String = "",
    val colorHue: Float = 0f,
    val name: String = "",
    val joinedAt: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "role" to role,
        "githubUsername" to githubUsername,
        "colorHue" to colorHue.toDouble(),
        "name" to name,
        "joinedAt" to joinedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): CollabMember {
            if (map == null) return CollabMember()
            return CollabMember(
                role = map["role"] as? String ?: CollabRoles.VIEWER,
                githubUsername = map["githubUsername"] as? String ?: "",
                colorHue = (map["colorHue"] as? Number)?.toFloat() ?: 0f,
                name = map["name"] as? String ?: "",
                joinedAt = (map["joinedAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

data class CollabInvite(
    val role: String = CollabRoles.EDITOR,
    val expiresAt: Long = 0L,
    val usedBy: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "role" to role,
        "expiresAt" to expiresAt,
        "usedBy" to usedBy
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): CollabInvite? {
            if (map == null) return null
            return CollabInvite(
                role = map["role"] as? String ?: CollabRoles.EDITOR,
                expiresAt = (map["expiresAt"] as? Number)?.toLong() ?: 0L,
                usedBy = map["usedBy"] as? String ?: ""
            )
        }
    }
}

data class CollabRequest(
    val githubUsername: String = "",
    val name: String = "",
    val colorHue: Float = 0f,
    val at: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "githubUsername" to githubUsername,
        "name" to name,
        "colorHue" to colorHue.toDouble(),
        "at" to at
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): CollabRequest? {
            if (map == null) return null
            return CollabRequest(
                githubUsername = map["githubUsername"] as? String ?: "",
                name = map["name"] as? String ?: "",
                colorHue = (map["colorHue"] as? Number)?.toFloat() ?: 0f,
                at = (map["at"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

data class MemberPresence(
    val uid: String = "",
    val name: String = "",
    val colorHue: Float = 0f,
    val role: String = CollabRoles.VIEWER,
    val sceneId: String = "",
    val spriteId: String = "",
    val tab: String = CollabTabs.SPRITES,
    val detail: String = "",
    val updatedAt: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "colorHue" to colorHue.toDouble(),
        "role" to role,
        "sceneId" to sceneId,
        "spriteId" to spriteId,
        "tab" to tab,
        "detail" to detail
    )

    companion object {
        fun fromSnapshot(uid: String, map: Map<String, Any?>?, updatedAt: Long): MemberPresence {
            if (map == null) return MemberPresence(uid = uid)
            return MemberPresence(
                uid = uid,
                name = map["name"] as? String ?: "",
                colorHue = (map["colorHue"] as? Number)?.toFloat() ?: 0f,
                role = map["role"] as? String ?: CollabRoles.VIEWER,
                sceneId = map["sceneId"] as? String ?: "",
                spriteId = map["spriteId"] as? String ?: "",
                tab = map["tab"] as? String ?: CollabTabs.SPRITES,
                detail = map["detail"] as? String ?: "",
                updatedAt = updatedAt
            )
        }
    }
}
