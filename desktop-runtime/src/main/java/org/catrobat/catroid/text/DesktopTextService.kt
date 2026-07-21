package org.catrobat.catroid.text

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage

class DesktopTextService : TextService {
    override fun rasterizeText(
        text: String,
        textSizePx: Float,
        color: String?,
        typefaceName: String?,
        isWrapped: Boolean,
        alignment: Int
    ): RasterizedText {
        val size = textSizePx.toInt().coerceAtLeast(1)
        val font = if (typefaceName != null) {
            try {
                Font(typefaceName, Font.PLAIN, size)
            } catch (_: Exception) {
                Font(Font.SANS_SERIF, Font.PLAIN, size)
            }
        } else {
            Font(Font.SANS_SERIF, Font.PLAIN, size)
        }

        val lines = if (isWrapped) text.split("\n") else listOf(text)

        val probe = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
        probe.font = font
        val fm = probe.getFontMetrics(font)
        val totalWidth = (if (lines.isEmpty()) 1 else lines.maxOf { if (it.isEmpty()) 1 else fm.stringWidth(it) })
            .coerceAtLeast(1)
        val totalHeight = (fm.height * (if (isWrapped) kotlin.math.max(lines.size, 1) else 1))
            .coerceAtLeast(1)
        val ascent = fm.ascent
        probe.dispose()

        val img = BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = img.createGraphics()
        g.font = font
        g.color = parseColor(color)
        var y = ascent
        for (line in lines) {
            var x = 0
            if (isWrapped) {
                val w = fm.stringWidth(line)
                x = when (alignment) {
                    2 -> totalWidth - w
                    1 -> (totalWidth - w) / 2
                    else -> 0
                }
            }
            if (line.isNotEmpty()) g.drawString(line, x, y)
            y += fm.height
        }
        g.dispose()

        val rgba = ByteArray(totalWidth * totalHeight * 4)
        var i = 0
        for (yy in 0 until totalHeight) {
            for (xx in 0 until totalWidth) {
                val p = img.getRGB(xx, yy)
                rgba[i++] = (p shr 16 and 0xFF).toByte()
                rgba[i++] = (p shr 8 and 0xFF).toByte()
                rgba[i++] = (p and 0xFF).toByte()
                rgba[i++] = (p shr 24 and 0xFF).toByte()
            }
        }
        return RasterizedText(totalWidth, totalHeight, rgba)
    }

    private fun parseColor(color: String?): Color {
        if (color == null) return Color.BLACK
        return try {
            val c = color.uppercase()
            if (c.startsWith("#") && (c.length == 7 || c.length == 9)) Color.decode(c) else Color.BLACK
        } catch (_: Exception) {
            Color.BLACK
        }
    }
}
