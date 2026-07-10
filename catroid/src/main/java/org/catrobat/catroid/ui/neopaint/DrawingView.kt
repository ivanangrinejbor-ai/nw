package org.catrobat.catroid.ui.neopaint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val layers = ArrayList<PaintLayer>()
    private var currentLayerIndex = 0

    private var bitmapWidth = 1
    private var bitmapHeight = 1

    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private var toolType = ToolType.BRUSH
    private var paintColor = Color.BLACK
    private var strokeWidth = 8f
    private var toolOpacity = 1f

    private val drawPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var startY = 0f
    private var shapeSnapshot: Bitmap? = null
    private var smudgeSrc: Bitmap? = null

    private val undoStack = ArrayDeque<Pair<Int, Bitmap>>()
    private val redoStack = ArrayDeque<Pair<Int, Bitmap>>()

    var onRequestTextListener: ((x: Float, y: Float) -> Unit)? = null
    var onColorPickedListener: ((Int) -> Unit)? = null
    var onChangeListener: (() -> Unit)? = null

    fun initializeWithBitmap(bitmap: Bitmap) {
        val mutable = ensureMutable(bitmap)
        bitmapWidth = mutable.width
        bitmapHeight = mutable.height
        layers.clear()
        layers.add(PaintLayer(mutable, "Layer 1"))
        currentLayerIndex = 0
        undoStack.clear()
        redoStack.clear()
        computeTransform()
        invalidate()
        onChangeListener?.invoke()
    }

    fun getCurrentLayer(): PaintLayer? =
        if (layers.isEmpty()) null else layers[currentLayerIndex]

    fun getLayer(index: Int): PaintLayer? =
        if (index in layers.indices) layers[index] else null

    fun layerCount(): Int = layers.size
    fun currentIndex(): Int = currentLayerIndex

    fun addLayer(): Int {
        val bmp = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        layers.add(PaintLayer(bmp, "Layer ${layers.size + 1}"))
        currentLayerIndex = layers.size - 1
        onChangeListener?.invoke()
        return currentLayerIndex
    }

    fun removeLayer(index: Int) {
        if (layers.size <= 1 || index < 0 || index >= layers.size) return
        layers.removeAt(index)
        if (currentLayerIndex >= layers.size) currentLayerIndex = layers.size - 1
        onChangeListener?.invoke()
    }

    fun duplicateLayer(index: Int) {
        if (index < 0 || index >= layers.size) return
        val src = layers[index]
        val copy = ensureMutable(src.bitmap)
        layers.add(index + 1, PaintLayer(copy, "${src.name} copy", src.visible, src.opacity))
        currentLayerIndex = index + 1
        onChangeListener?.invoke()
    }

    fun selectLayer(index: Int) {
        if (index in layers.indices) {
            currentLayerIndex = index
            onChangeListener?.invoke()
        }
    }

    fun toggleVisibility(index: Int) {
        if (index in layers.indices) {
            layers[index].visible = !layers[index].visible
            onChangeListener?.invoke()
        }
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        if (index in layers.indices) {
            layers[index].opacity = opacity
            invalidate()
        }
    }

    fun setTool(type: ToolType) {
        toolType = type
    }

    fun setColor(color: Int) {
        paintColor = color
    }

    fun setStrokeWidth(width: Float) {
        strokeWidth = width
    }

    fun setOpacity(opacity: Float) {
        toolOpacity = opacity
    }

    fun clearCurrentLayer() {
        val layer = getCurrentLayer() ?: return
        pushUndo(layer)
        val c = Canvas(layer.bitmap)
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
        onChangeListener?.invoke()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (undoStack.isEmpty()) return
        val (index, snapshot) = undoStack.removeLast()
        val layer = layers.getOrNull(index) ?: return
        redoStack.add(Pair(index, ensureMutable(layer.bitmap)))
        layer.bitmap = ensureMutable(snapshot)
        invalidate()
        onChangeListener?.invoke()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val (index, snapshot) = redoStack.removeLast()
        val layer = layers.getOrNull(index) ?: return
        undoStack.add(Pair(index, ensureMutable(layer.bitmap)))
        layer.bitmap = ensureMutable(snapshot)
        invalidate()
        onChangeListener?.invoke()
    }

    fun flipHorizontal() = transform { it.flipHorizontal() }
    fun flipVertical() = transform { it.flipVertical() }
    fun rotate90Cw() = transform { it.rotate90Cw() }

    private fun transform(operation: (Bitmap) -> Bitmap) {
        val layer = getCurrentLayer() ?: return
        pushUndo(layer)
        layer.bitmap = operation(ensureMutable(layer.bitmap))
        invalidate()
        onChangeListener?.invoke()
    }

    fun drawTextOnCurrentLayer(text: String, x: Float, y: Float) {
        val layer = getCurrentLayer() ?: return
        if (text.isBlank()) return
        pushUndo(layer)
        val c = Canvas(layer.bitmap)
        val p = Paint().apply {
            isAntiAlias = true
            color = paintColor
            textSize = (strokeWidth * 4).coerceAtLeast(24f)
            style = Paint.Style.FILL
        }
        c.drawText(text, toBitmapX(x), toBitmapY(y), p)
        invalidate()
        onChangeListener?.invoke()
    }

    fun getCompositeBitmap(): Bitmap {
        val result = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val c = Canvas(result)
        for (layer in layers) {
            if (!layer.visible) continue
            val alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            c.save()
            c.alpha = alpha
            c.drawBitmap(layer.bitmap, 0f, 0f, null)
            c.restore()
        }
        return result
    }

    private fun pushUndo(layer: PaintLayer) {
        undoStack.add(Pair(currentLayerIndex, ensureMutable(layer.bitmap)))
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeTransform()
    }

    private fun computeTransform() {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || width == 0 || height == 0) return
        scale = min(width.toFloat() / bitmapWidth, height.toFloat() / bitmapHeight)
        offsetX = (width - bitmapWidth * scale) / 2f
        offsetY = (height - bitmapHeight * scale) / 2f
    }

    private fun toBitmapX(x: Float): Float = (x - offsetX) / scale
    private fun toBitmapY(y: Float): Float = (y - offsetY) / scale

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFF2B2B2B.toInt())
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        for (layer in layers) {
            if (!layer.visible) continue
            val alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            canvas.save()
            canvas.alpha = alpha
            canvas.drawBitmap(layer.bitmap, 0f, 0f, null)
            canvas.restore()
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (layers.isEmpty()) return false
        val bx = toBitmapX(event.x)
        val by = toBitmapY(event.y)
        when (toolType) {
            ToolType.BRUSH -> handleDraw(event, bx, by, false)
            ToolType.ERASER -> handleDraw(event, bx, by, true)
            ToolType.SMUDGE -> handleSmudge(event, bx, by)
            ToolType.FILL -> if (event.action == MotionEvent.ACTION_DOWN) {
                floodFill(getCurrentLayer()!!.bitmap, bx.toInt(), by.toInt(), paintColor)
                invalidate()
                onChangeListener?.invoke()
            }
            ToolType.EYEDROPPER -> if (event.action == MotionEvent.ACTION_DOWN) {
                val color = pickColor(bx.toInt(), by.toInt())
                onColorPickedListener?.invoke(color)
            }
            ToolType.LINE, ToolType.RECTANGLE, ToolType.OVAL -> handleShape(event, bx, by)
            ToolType.TEXT -> if (event.action == MotionEvent.ACTION_DOWN) {
                onRequestTextListener?.invoke(event.x, event.y)
            }
        }
        return true
    }

    private fun handleDraw(event: MotionEvent, bx: Float, by: Float, erase: Boolean) {
        val layer = getCurrentLayer() ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pushUndo(layer)
                lastX = bx
                lastY = by
                applyPaint(erase)
                Canvas(layer.bitmap).drawPoint(bx, by, drawPaint)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                applyPaint(erase)
                Canvas(layer.bitmap).drawLine(lastX, lastY, bx, by, drawPaint)
                lastX = bx
                lastY = by
                invalidate()
            }
            MotionEvent.ACTION_UP -> onChangeListener?.invoke()
        }
    }

    private fun handleSmudge(event: MotionEvent, bx: Float, by: Float) {
        val layer = getCurrentLayer() ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pushUndo(layer)
                smudgeSrc = ensureMutable(layer.bitmap)
                lastX = bx
                lastY = by
            }
            MotionEvent.ACTION_MOVE -> {
                val src = smudgeSrc ?: return
                val r = (strokeWidth / 2 + 6).toInt().coerceAtLeast(3)
                val sx = (lastX - r).toInt().coerceIn(0, (bitmapWidth - 2 * r).coerceAtLeast(1))
                val sy = (lastY - r).toInt().coerceIn(0, (bitmapHeight - 2 * r).coerceAtLeast(1))
                val stamp = Bitmap.createBitmap(src, sx, sy, 2 * r, 2 * r)
                val dx = (bx - r).coerceIn(0f, (bitmapWidth - 2 * r).toFloat())
                val dy = (by - r).coerceIn(0f, (bitmapHeight - 2 * r).toFloat())
                val p = Paint().apply { alpha = (toolOpacity * 160).toInt().coerceIn(0, 255) }
                Canvas(layer.bitmap).drawBitmap(stamp, dx, dy, p)
                stamp.recycle()
                lastX = bx
                lastY = by
                invalidate()
            }
            MotionEvent.ACTION_UP -> onChangeListener?.invoke()
        }
    }

    private fun handleShape(event: MotionEvent, bx: Float, by: Float) {
        val layer = getCurrentLayer() ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pushUndo(layer)
                shapeSnapshot = ensureMutable(layer.bitmap)
                startX = bx
                startY = by
                lastX = bx
                lastY = by
            }
            MotionEvent.ACTION_MOVE -> {
                restoreFrom(shapeSnapshot)
                drawShape(layer.bitmap, startX, startY, bx, by)
                lastX = bx
                lastY = by
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                restoreFrom(shapeSnapshot)
                drawShape(layer.bitmap, startX, startY, bx, by)
                shapeSnapshot = null
                invalidate()
                onChangeListener?.invoke()
            }
        }
    }

    private fun restoreFrom(snapshot: Bitmap?) {
        val layer = getCurrentLayer() ?: return
        if (snapshot == null) return
        layer.bitmap = ensureMutable(snapshot)
    }

    private fun drawShape(target: Bitmap, x0: Float, y0: Float, x1: Float, y1: Float) {
        applyPaint(false)
        val c = Canvas(target)
        when (toolType) {
            ToolType.LINE -> c.drawLine(x0, y0, x1, y1, drawPaint)
            ToolType.RECTANGLE -> c.drawRect(
                min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), drawPaint)
            ToolType.OVAL -> c.drawOval(
                min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), drawPaint)
            else -> {}
        }
    }

    private fun applyPaint(erase: Boolean) {
        drawPaint.color = if (erase) Color.TRANSPARENT else paintColor
        drawPaint.alpha = (toolOpacity * 255).toInt().coerceIn(0, 255)
        drawPaint.strokeWidth = strokeWidth
        drawPaint.xfermode = if (erase) PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
    }

    private fun pickColor(x: Int, y: Int): Int {
        val px = x.coerceIn(0, bitmapWidth - 1)
        val py = y.coerceIn(0, bitmapHeight - 1)
        var color = Color.TRANSPARENT
        for (layer in layers.reversed()) {
            if (!layer.visible) continue
            val c = layer.bitmap.getPixel(px, py)
            if (c != Color.TRANSPARENT) {
                color = c
                break
            }
        }
        return color
    }

    private fun floodFill(bmp: Bitmap, x: Int, y: Int, newColor: Int) {
        val w = bmp.width
        val h = bmp.height
        if (x < 0 || y < 0 || x >= w || y >= h) return
        val oldColor = bmp.getPixel(x, y)
        if (oldColor == newColor) return
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val stack = ArrayDeque<Int>()
        stack.add(y * w + x)
        val tolerance = 40
        val oldA = (oldColor shr 24) and 0xff
        val oldR = (oldColor shr 16) and 0xff
        val oldG = (oldColor shr 8) and 0xff
        val oldB = oldColor and 0xff
        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            val c = pixels[idx]
            val ca = (c shr 24) and 0xff
            val cr = (c shr 16) and 0xff
            val cg = (c shr 8) and 0xff
            val cb = c and 0xff
            if (abs(ca - oldA) <= tolerance && abs(cr - oldR) <= tolerance &&
                abs(cg - oldG) <= tolerance && abs(cb - oldB) <= tolerance) {
                pixels[idx] = newColor
                val px = idx % w
                val py = idx / w
                if (px > 0) stack.add(idx - 1)
                if (px < w - 1) stack.add(idx + 1)
                if (py > 0) stack.add(idx - w)
                if (py < h - 1) stack.add(idx + w)
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun ensureMutable(bitmap: Bitmap): Bitmap {
        if (bitmap.isMutable) return bitmap
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun Bitmap.flipHorizontal(): Bitmap {
        val m = android.graphics.Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
    }

    private fun Bitmap.flipVertical(): Bitmap {
        val m = android.graphics.Matrix().apply { preScale(1f, -1f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
    }

    private fun Bitmap.rotate90Cw(): Bitmap {
        val m = android.graphics.Matrix().apply { preRotate(90f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
    }

    private fun max(a: Float, b: Float) = if (a > b) a else b
    private fun min(a: Float, b: Float) = if (a < b) a else b
}
