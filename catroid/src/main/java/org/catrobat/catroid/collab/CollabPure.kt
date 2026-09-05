package org.catrobat.catroid.collab

object BorderSegments {
    data class Segment(val start: Float, val length: Float)

    fun compute(perimeter: Float, count: Int): List<Segment> {
        if (perimeter <= 0f || count <= 0) return emptyList()
        val seg = perimeter / count
        return (0 until count).map { Segment(it * seg, seg) }
    }

    fun phaseFor(perimeter: Float, start: Float): Float {
        if (perimeter <= 0f) return 0f
        return ((perimeter - start) % perimeter + perimeter) % perimeter
    }
}

object PresenceFreshness {
    fun isFresh(updatedAt: Long, now: Long, ttlMs: Long): Boolean {
        if (updatedAt <= 0) return false
        val age = now - updatedAt
        return age in 0..ttlMs
    }
}

object CollabAccess {
    fun isRevoked(errorCode: Any?): Boolean {
        return errorCode?.toString() == "PERMISSION_DENIED"
    }
}

object CollabCodes {
    const val SID_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun randomSessionId(): String {
        return (1..6).map { SID_ALPHABET.random() }.joinToString("")
    }

    fun randomInviteCode(): String {
        return (1..6).map { ('0'..'9').random() }.joinToString("")
    }

    fun isValidSessionId(value: String): Boolean {
        return value.length == 6 && value.all { SID_ALPHABET.contains(it) }
    }

    fun isValidInviteCode(value: String): Boolean {
        return value.length == 6 && value.all { it in '0'..'9' }
    }
}
