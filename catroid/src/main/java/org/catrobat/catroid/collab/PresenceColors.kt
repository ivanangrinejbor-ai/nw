package org.catrobat.catroid.collab

import kotlin.math.abs
import kotlin.random.Random

object HsvColor {
    fun hsvToRgb(hue: Float, saturation: Float, value: Float): Int {
        val h = (((hue % 360f) + 360f) % 360f) / 60f
        val c = value * saturation
        val x = c * (1f - kotlin.math.abs(h % 2f - 1f))
        val (r1, g1, b1) = when (h.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = value - c
        val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}

object PresenceColors {
    private const val GOLDEN_ANGLE = 137.5f

    fun hueFor(taken: Collection<Float>): Float {
        if (taken.isEmpty()) return Random.nextFloat() * 360f
        var candidate = Random.nextFloat() * 360f
        var best = candidate
        var bestGap = -1f
        repeat(12) {
            var gap = 360f
            for (h in taken) {
                var d = abs(candidate - h) % 360f
                if (d > 180f) d = 360f - d
                if (d < gap) gap = d
            }
            if (gap > bestGap) {
                bestGap = gap
                best = candidate
            }
            candidate = (candidate + GOLDEN_ANGLE) % 360f
        }
        return best
    }

    fun colorInt(hue: Float): Int {
        return HsvColor.hsvToRgb((((hue % 360f) + 360f) % 360f), 0.78f, 0.95f)
    }

    fun initials(name: String): String {
        val clean = name.trim()
        if (clean.isEmpty()) return "?"
        val parts = clean.split("\\s+".toRegex())
        return if (parts.size > 1) {
            (parts[0].first() + "" + parts[1].first()).uppercase()
        } else {
            clean.take(2).uppercase()
        }
    }
}
