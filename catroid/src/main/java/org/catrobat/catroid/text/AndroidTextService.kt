package org.catrobat.catroid.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import org.catrobat.catroid.utils.ShowTextUtils
import java.io.File

class AndroidTextService : TextService {

    private val typefaceCache = object : LinkedHashMap<String, Typeface>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Typeface>): Boolean =
            size > CACHE_SIZE
    }

    private fun resolveTypeface(typefaceName: String): Typeface? {
        val file = File(typefaceName)
        if (!file.isFile) {
            return null
        }
        val cacheKey = "${file.absolutePath}:${file.lastModified()}"
        return synchronized(typefaceCache) {
            try {
                typefaceCache.getOrPut(cacheKey) {
                    Typeface.createFromFile(typefaceName)
                }
            } catch (ignored: Exception) {
                null
            }
        }
    }

    override fun rasterizeText(
        text: String,
        textSizePx: Float,
        color: String?,
        typefaceName: String?,
        isWrapped: Boolean,
        alignment: Int
    ): RasterizedText {
        val paint = Paint()
        paint.textSize = textSizePx
        if (typefaceName != null) {
            paint.typeface = resolveTypeface(typefaceName)
        }
        paint.isAntiAlias = true

        val colorRgb = if (ShowTextUtils.isValidColorString(color)) {
            val upper = color!!.uppercase(java.util.Locale.getDefault())
            ShowTextUtils.calculateColorRGBs(upper)
        } else intArrayOf(0, 0, 0)

        paint.color = (0xFF000000.toInt()) or (colorRgb[0] shl 16) or (colorRgb[1] shl 8) or colorRgb[2]

        val baseline = -paint.ascent()
        val textHeight = (baseline + paint.descent()).toInt()
        val lines = if (isWrapped) text.split("\n") else listOf(text)

        var totalWidth = 0f
        for (line in lines) {
            if (line.isNotEmpty()) {
                val w = paint.measureText(line)
                if (w > totalWidth) totalWidth = w
            }
        }
        val totalHeight = textHeight * (if (isWrapped) kotlin.math.max(lines.size, 1) else 1)
        val bitmapWidth = kotlin.math.max(totalWidth.toInt(), 1)
        val bitmapHeight = kotlin.math.max(totalHeight, 1)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        var drawPosY = if (isWrapped) textHeight.toFloat() else baseline
        for (line in lines) {
            if (line.isEmpty()) continue
            var drawPosX = 0f
            if (isWrapped) {
                drawPosX = when (alignment) {
                    ShowTextUtils.ALIGNMENT_STYLE_CENTERED -> (totalWidth - paint.measureText(line)) / 2
                    ShowTextUtils.ALIGNMENT_STYLE_RIGHT -> totalWidth - paint.measureText(line)
                    else -> 0f
                }
            }
            canvas.drawText(line, drawPosX, drawPosY, paint)
            if (isWrapped) drawPosY += textHeight.toFloat()
        }

        val pixels = IntArray(bitmapWidth * bitmapHeight)
        bitmap.getPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
        bitmap.recycle()

        val rgba = ByteArray(bitmapWidth * bitmapHeight * 4)
        for (i in pixels.indices) {
            val p = pixels[i]
            rgba[i * 4] = (p shr 16 and 0xFF).toByte()
            rgba[i * 4 + 1] = (p shr 8 and 0xFF).toByte()
            rgba[i * 4 + 2] = (p and 0xFF).toByte()
            rgba[i * 4 + 3] = (p shr 24 and 0xFF).toByte()
        }
        return RasterizedText(bitmapWidth, bitmapHeight, rgba)
    }
}

private const val CACHE_SIZE = 12

