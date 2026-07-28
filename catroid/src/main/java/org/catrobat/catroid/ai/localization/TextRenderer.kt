package org.catrobat.catroid.ai.localization

import android.graphics.*
import kotlin.math.abs
import kotlin.math.atan2

enum class QualityMode { FAST, STANDARD, MAXIMUM }

object TextRenderer {

    private val bgFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }

    fun replaceText(
        bitmap: Bitmap,
        region: TextRegion,
        customTypeface: Typeface? = null,
        quality: QualityMode = QualityMode.STANDARD,
        isPixelArt: Boolean = false
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val box = region.boundingBox
        val padding = 4

        fillBackground(canvas, bitmap, box, padding, region.backgroundColor)

        val angle = if (quality != QualityMode.FAST) region.rotationAngle else 0f
        val text = region.translatedText
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = region.textColor
            textSize = region.estimatedFontSize
            isFilterBitmap = !isPixelArt
            customTypeface?.let { typeface = it }
                ?: run { typeface = Typeface.DEFAULT_BOLD }
        }

        val lines = splitIntoLines(paint, text, box.width().toFloat())
        val lineCount = lines.size
        val fontSize = fitTextSize(paint, lines.maxByOrNull { it.length } ?: text,
            box.width().toFloat(), box.height().toFloat() / lineCount)
        paint.textSize = fontSize

        val isVertical = box.height() > box.width() * 2.5f && lines.size == 1
                && !text.contains(" ")

        if (isVertical) {
            drawTextVertical(canvas, text, paint, box)
        } else if (angle != 0f) {
            canvas.save()
            val cx = box.centerX().toFloat()
            val cy = box.centerY().toFloat()
            canvas.rotate(angle, cx, cy)
        }

        if (region.outlineColor != 0 && region.outlineColor != region.textColor) {
            drawTextWithOutline(canvas, lines, paint, box, region.outlineColor, region.outlineWidth)
        } else {
            drawTextLines(canvas, lines, paint, box)
        }

        if (angle != 0f) {
            canvas.restore()
        }

        return result
    }

    private fun fillBackground(
        canvas: Canvas,
        bitmap: Bitmap,
        box: Rect,
        padding: Int,
        fallbackColor: Int
    ) {
        val expandPad = padding + 2
        val left = maxOf(0, box.left - expandPad)
        val top = maxOf(0, box.top - expandPad)
        val right = minOf(bitmap.width - 1, box.right + expandPad)
        val bottom = minOf(bitmap.height - 1, box.bottom + expandPad)

        val colors = sampleEdgeColors(bitmap, left, top, right, bottom)
        if (colors.size >= 4) {
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val shader = RadialGradient(
                box.centerX().toFloat(), box.centerY().toFloat(),
                maxOf(box.width(), box.height()).toFloat() * 0.8f,
                IntArray(colors.size) { colors[it] },
                null, Shader.TileMode.CLAMP
            )
            gradPaint.shader = shader
            canvas.drawRect(
                (left - padding).toFloat(), (top - padding).toFloat(),
                (right + padding).toFloat(), (bottom + padding).toFloat(),
                gradPaint
            )
        } else {
            bgFillPaint.color = fallbackColor
            canvas.drawRect(
                (left - padding).toFloat(), (top - padding).toFloat(),
                (right + padding).toFloat(), (bottom + padding).toFloat(),
                bgFillPaint
            )
        }
    }

    private fun sampleEdgeColors(
        bitmap: Bitmap,
        left: Int, top: Int, right: Int, bottom: Int
    ): List<Int> {
        val colors = mutableListOf<Int>()
        val step = maxOf(2, (right - left) / 5)
        for (x in left..right step step) {
            if (top in 0 until bitmap.height) colors.add(bitmap.getPixel(x, top))
            if (bottom in 0 until bitmap.height) colors.add(bitmap.getPixel(x, bottom))
        }
        for (y in top..bottom step step) {
            if (left in 0 until bitmap.width) colors.add(bitmap.getPixel(left, y))
            if (right in 0 until bitmap.width) colors.add(bitmap.getPixel(right, y))
        }
        return if (colors.size > 4) {
            val clustered = clusterColors(colors, 4)
            clustered
        } else colors
    }

    private fun clusterColors(colors: List<Int>, k: Int): List<Int> {
        if (colors.size <= k) return colors
        val sorted = colors.sortedBy { brightness(it) }
        val step = sorted.size / k
        return (0 until k).map { i ->
            val start = i * step
            val end = minOf(start + step, sorted.size)
            if (start >= end) sorted.last()
            else medianColor(sorted.subList(start, end))
        }
    }

    private fun splitIntoLines(paint: Paint, text: String, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        val currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth * 1.2f) {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine.clear()
                }
                if (paint.measureText(word) > maxWidth * 1.2f) {
                    lines.addAll(splitLongWord(paint, word, maxWidth))
                } else {
                    currentLine.append(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return if (lines.isEmpty()) listOf(text) else lines
    }

    private fun splitLongWord(paint: Paint, word: String, maxWidth: Float): List<String> {
        val chars = mutableListOf<String>()
        val sb = StringBuilder()
        for (c in word) {
            val test = sb.toString() + c
            if (paint.measureText(test) > maxWidth * 1.15f && sb.isNotEmpty()) {
                chars.add(sb.toString())
                sb.clear()
            }
            sb.append(c)
        }
        if (sb.isNotEmpty()) chars.add(sb.toString())
        return chars
    }

    private fun drawTextLines(canvas: Canvas, lines: List<String>, paint: Paint, box: Rect) {
        val lineHeight = paint.fontMetrics.let { it.descent - it.ascent }
        val totalHeight = lineHeight * lines.size
        val startY = box.top.toFloat() + (box.height() - totalHeight) / 2f - paint.fontMetrics.ascent

        for ((i, line) in lines.withIndex()) {
            val lineWidth = paint.measureText(line)
            val x = box.left.toFloat() + (box.width() - lineWidth) / 2f
            val y = startY + i * lineHeight
            drawTextSafe(canvas, line, x, y, paint)
        }
    }

    private fun drawTextVertical(canvas: Canvas, text: String, paint: Paint, box: Rect) {
        val charHeight = paint.fontMetrics.let { it.descent - it.ascent }
        val totalHeight = charHeight * text.length
        val scaleY = minOf(1f, box.height().toFloat() / totalHeight)
        val scaledCharH = charHeight * scaleY
        val startX = box.centerX().toFloat()
        val startY = box.top.toFloat() + (box.height() - scaledCharH * text.length) / 2f - paint.fontMetrics.ascent * scaleY

        val charPaint = Paint(paint).apply { textSize = paint.textSize * scaleY }
        for ((i, c) in text.withIndex()) {
            val charStr = c.toString()
            val cw = charPaint.measureText(charStr)
            drawTextSafe(canvas, charStr, startX - cw / 2f, startY + i * scaledCharH, charPaint)
        }
    }

    private fun drawTextSafe(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        val missing = mutableListOf<Int>()
        text.codePoints().forEach { cp ->
            if (!paint.hasGlyph(cp.toString())) missing.add(cp)
        }
        if (missing.isEmpty()) {
            canvas.drawText(text, x, y, paint)
            return
        }
        val sysPaint = Paint(paint).apply { typeface = Typeface.DEFAULT }
        var cx = x
        val chars = text.codePoints().toArray()
        for (cp in chars) {
            val charStr = String(intArrayOf(cp), 0, 1)
            val usePaint = if (cp in missing) sysPaint else paint
            canvas.drawText(charStr, cx, y, usePaint)
            cx += usePaint.measureText(charStr)
        }
    }

    private fun drawTextWithOutline(
        canvas: Canvas,
        lines: List<String>,
        paint: Paint,
        box: Rect,
        outlineColor: Int,
        outlineWidth: Float
    ) {
        val lineHeight = paint.fontMetrics.let { it.descent - it.ascent }
        val totalHeight = lineHeight * lines.size
        val startY = box.top.toFloat() + (box.height() - totalHeight) / 2f - paint.fontMetrics.ascent

        val w = maxOf(1.5f, outlineWidth)
        strokePaint.apply { color = outlineColor; strokeWidth = w * 2f; style = Paint.Style.FILL_AND_STROKE }
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = w * 2f

        for ((i, line) in lines.withIndex()) {
            val lineWidth = paint.measureText(line)
            val x = box.left.toFloat() + (box.width() - lineWidth) / 2f
            val y = startY + i * lineHeight

            val prevColor = paint.color
            paint.color = outlineColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = w * 2.5f
            canvas.drawText(line, x, y, paint)

            paint.color = prevColor
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f
            drawTextSafe(canvas, line, x, y, paint)
        }
    }

    private fun fitTextSize(
        paint: Paint,
        text: String,
        maxWidth: Float,
        maxHeight: Float
    ): Float {
        var size = paint.textSize
        var iters = 0
        while (iters < 25) {
            paint.textSize = size
            val lines = splitIntoLines(paint, text, maxWidth)
            val fm = paint.fontMetrics
            val totalH = (fm.descent - fm.ascent) * lines.size
            val maxLineW = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
            if (maxLineW <= maxWidth * 1.12f && totalH <= maxHeight * 1.15f) break
            size *= 0.88f
            iters++
        }
        return size
    }

    fun detectBackgroundColor(bitmap: Bitmap, box: Rect): Int {
        val colors = sampleEdgeColors(
            bitmap,
            maxOf(0, box.left - 2), maxOf(0, box.top - 2),
            minOf(bitmap.width - 1, box.right + 2), minOf(bitmap.height - 1, box.bottom + 2)
        )
        return if (colors.isNotEmpty()) medianColor(colors) else 0x00000000
    }

    fun detectTextColor(bitmap: Bitmap, box: Rect): Int {
        val samples = mutableListOf<Int>()
        val step = maxOf(1, minOf(box.width(), box.height()) / 4)
        var y = box.top
        while (y < box.bottom) {
            var x = box.left
            while (x < box.right) {
                if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    if ((pixel shr 24 and 0xFF) > 40) samples.add(pixel)
                }
                x += step
            }
            y += step
        }
        return if (samples.isNotEmpty()) dominantColor(samples) else 0xFF000000.toInt()
    }

    fun detectOutlineColor(bitmap: Bitmap, box: Rect, textColor: Int): Int {
        val samples = mutableListOf<Int>()
        val step = maxOf(1, minOf(box.width(), box.height()) / 6)
        val textR = textColor shr 16 and 0xFF
        val textG = textColor shr 8 and 0xFF
        val textB = textColor and 0xFF

        var y = box.top
        while (y < box.bottom) {
            var x = box.left
            while (x < box.right) {
                if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                    val p = bitmap.getPixel(x, y)
                    val pr = p shr 16 and 0xFF; val pg = p shr 8 and 0xFF; val pb = p and 0xFF
                    if ((p shr 24 and 0xFF) > 60 &&
                        (abs(pr - textR) > 40 || abs(pg - textG) > 40 || abs(pb - textB) > 40)) {
                        samples.add(p)
                    }
                }
                x += step
            }
            y += step
        }
        return if (samples.size > step) dominantColor(samples) else 0
    }

    fun detectOutlineWidth(bitmap: Bitmap, box: Rect, textColor: Int): Float {
        var maxOutlinePx = 0
        val cx = box.centerX()
        val cy = box.centerY()
        val startX = box.left + box.width() / 4
        val startY = box.top + box.height() / 4
        val endX = box.right - box.width() / 4
        val endY = box.bottom - box.height() / 4

        val textR = textColor shr 16 and 0xFF
        val textG = textColor shr 8 and 0xFF
        val textB = textColor and 0xFF
        val tolerance = 35

        for (x in startX..endX) {
            var outlinePx = 0
            var inText = false
            for (y in box.top..box.bottom) {
                if (y !in 0 until bitmap.height) continue
                val p = bitmap.getPixel(x, y)
                val same = abs((p shr 16 and 0xFF) - textR) < tolerance &&
                        abs((p shr 8 and 0xFF) - textG) < tolerance &&
                        abs((p and 0xFF) - textB) < tolerance
                if (same && (p shr 24 and 0xFF) > 60) {
                    if (!inText) { outlinePx = 0 }
                    inText = true
                } else if (inText) {
                    outlinePx++
                    if (outlinePx > 3) break
                }
            }
            if (outlinePx in 1..3) maxOutlinePx = maxOf(maxOutlinePx, outlinePx)
        }
        return maxOf(0.5f, maxOutlinePx.toFloat())
    }

    fun detectRotationAngle(line: Any?, bitmap: Bitmap?): Float {
        return try {
            if (line == null) return 0f
            val m = line.javaClass.getMethod("getCornerPoints")
            val points = m.invoke(line) as? Array<android.graphics.Point> ?: return 0f
            if (points.size < 4) return 0f
            val dx = (points[1].x - points[0].x).toFloat()
            val dy = (points[1].y - points[0].y).toFloat()
            if (abs(dx) < 2f) return 0f
            Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        } catch (_: Exception) {
            0f
        }
    }

    fun detectFontSize(box: Rect): Float {
        return box.height() * 0.62f
    }

    private fun medianColor(colors: List<Int>): Int {
        if (colors.isEmpty()) return 0
        val sorted = colors.sortedBy { brightness(it) }
        return sorted[sorted.size / 2]
    }

    private fun dominantColor(colors: List<Int>): Int {
        if (colors.isEmpty()) return 0xFF000000.toInt()
        val grouped = colors.groupBy { it and 0x00FFFFFF }
        return grouped.maxByOrNull { it.value.size }?.key?.let { it or 0xFF000000.toInt() }
            ?: colors.first()
    }

    internal fun brightness(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
