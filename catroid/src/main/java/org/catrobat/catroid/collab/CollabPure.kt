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
