package org.catrobat.catroid.stage

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import java.awt.Color as AwtColor
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File

/**
 * Offline-safe Cyrillic text rasterizer.
 *
 * libGDX's default [com.badlogic.gdx.graphics.g2d.BitmapFont] only ships
 * Latin glyphs, so any Cyrillic project name / say-think text renders as
 * missing-glyph boxes. We rasterize text with a system TrueType font
 * (Arial on Windows, which includes Cyrillic) into a libGDX [Texture]
 * that can be drawn with the SpriteBatch.
 *
 * AWT is part of the standard JRE, so no extra desktop dependency is needed.
 */
object CyrillicText {

    private val baseFont: Font = try {
        val ttf = File("C:/Windows/Fonts/arial.ttf")
        if (ttf.exists()) {
            Font.createFont(Font.TRUETYPE_FONT, ttf)
        } else {
            Font("Arial", Font.PLAIN, 18)
        }
    } catch (_: Exception) {
        Font("Arial", Font.PLAIN, 18)
    }

    fun render(text: String, color: Color = Color.WHITE, size: Int = 18): Texture {
        val font = baseFont.deriveFont(Font.PLAIN, size.toFloat())

        // First pass: measure with a 1x1 scratch image.
        val scratch = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val sg = scratch.createGraphics()
        sg.font = font
        val fm = sg.getFontMetrics(font)
        val w = kotlin.math.max(1, fm.stringWidth(text))
        val h = kotlin.math.max(1, fm.height)
        sg.dispose()

        // Second pass: real raster.
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g.font = font
        g.color = AwtColor(
            (color.r * 255f).toInt().coerceIn(0, 255),
            (color.g * 255f).toInt().coerceIn(0, 255),
            (color.b * 255f).toInt().coerceIn(0, 255),
            (color.a * 255f).toInt().coerceIn(0, 255)
        )
        g.drawString(text, 0, fm.ascent)
        g.dispose()

        val data = (img.raster.dataBuffer as java.awt.image.DataBufferInt).data
        val pixmap = Pixmap(w, h, Pixmap.Format.RGBA8888)
        val buf = pixmap.pixels
        buf.position(0)
        for (p in data) {
            buf.put(((p shr 16) and 0xFF).toByte()) // R
            buf.put(((p shr 8) and 0xFF).toByte())  // G
            buf.put((p and 0xFF).toByte())           // B
            buf.put(((p shr 24) and 0xFF).toByte()) // A
        }
        buf.position(0)
        val tex = Texture(pixmap)
        pixmap.dispose()
        return tex
    }
}
