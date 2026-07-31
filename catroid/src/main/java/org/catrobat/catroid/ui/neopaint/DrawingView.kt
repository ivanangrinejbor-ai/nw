package org.catrobat.catroid.ui.neopaint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var bitmapWidth = 1
        private set
    var bitmapHeight = 1
        private set

    var onRequestTextListener: ((x: Float, y: Float) -> Unit)? = null
    var onColorPickedListener: ((Int) -> Unit)? = null
    var onChangeListener: (() -> Unit)? = null
    var onShowConfirmButtons: ((show: Boolean) -> Unit)? = null

    private val layers = ArrayList<PaintLayer>()
    private var currentLayerIndex = 0

    private var toolType = ToolType.BRUSH
    private var paintColor = Color.BLACK
    private var strokeWidth = 8f
    private var toolOpacity = 1f
    private var shapeFillAmount = 100
    private var textContent = ""
    private var textFont: Typeface? = null
    private var textOutlineWidth = 0f
    private var textOutlineColor = Color.BLACK
    private var textGlowRadius = 0f
    private var textGlowColor = Color.parseColor("#80FFFFFF")
    private var textGradientStart = Color.TRANSPARENT
    private var textGradientEnd = Color.TRANSPARENT

    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val layerPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33000000.toInt()
    }
    private val overlayStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFFFFFFFF.toInt()
    }
    private val starPath = Path()
    private val heartPath = Path()

    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var startY = 0f
    private var shapeSnapshot: Bitmap? = null
    private var smudgeSrc: Bitmap? = null
    private var smudgeScratchBitmap: Bitmap? = null
    private var smudgeScratchCanvas: Canvas? = null

    private var fitScale = 1f
    private var fitOffsetX = 0f
    private var fitOffsetY = 0f
    private var userZoom = 1f
    private var userPanX = 0f
    private var userPanY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastPinchDist = 0f
    private var isPinching = false
    private var pinchCenterBx = 0f
    private var pinchCenterBy = 0f
    private var pinchCenterSx = 0f
    private var pinchCenterSy = 0f

    private var previewX = -1f
    private var previewY = -1f
    private var showPreview = false
    private val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var overlay: OverlayShape? = null
    private var overlayStartX = 0f
    private var overlayStartY = 0f
    private var overlayInitCenterX = 0f
    private var overlayInitCenterY = 0f
    private var overlayInitW = 0f
    private var overlayInitH = 0f
    private var overlayInitZoom = 1f
    private var overlayTouchMode = OverlayTouchMode.NONE
    private var activeHandle = ResizeHandle.NONE
    private var prePinchOverlayW = 0f
    private var prePinchOverlayH = 0f
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val idleHandler = Handler(Looper.getMainLooper())
    private var idlePending = false

    

    private val undoStack = ArrayDeque<Pair<Int, Bitmap>>()
    private val redoStack = ArrayDeque<Pair<Int, Bitmap>>()

    

    fun initializeWithBitmap(bitmap: Bitmap) {
        val mutable = ensureMutable(bitmap)
        bitmapWidth = mutable.width
        bitmapHeight = mutable.height
        for (l in layers) l.bitmap.recycle()
        layers.clear()
        layers.add(PaintLayer(mutable, "Layer 1"))
        currentLayerIndex = 0
        clearUndoStack()
        clearRedoStack()
        userZoom = 1f
        userPanX = 0f
        userPanY = 0f
        overlay = null
        computeFitTransform()
        invalidate()
        onChangeListener?.invoke()
    }

    private fun clearUndoStack() {
        for (pair in undoStack) {
            pair.second.recycle()
        }
        undoStack.clear()
    }

    private fun clearRedoStack() {
        for (pair in redoStack) {
            pair.second.recycle()
        }
        redoStack.clear()
    }

    

    

    fun getCurrentLayer(): PaintLayer? = if (layers.isEmpty()) null else layers[currentLayerIndex]
    fun getLayer(index: Int): PaintLayer? = if (index in layers.indices) layers[index] else null
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
        layers[index].bitmap.recycle()
        layers.removeAt(index)
        if (currentLayerIndex >= layers.size) currentLayerIndex = layers.size - 1
        onChangeListener?.invoke()
    }

    fun duplicateLayer(index: Int) {
        if (index < 0 || index >= layers.size) return
        val src = layers[index]
        val copy = src.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        layers.add(index + 1, PaintLayer(copy, "${src.name} copy", src.visible, src.opacity))
        currentLayerIndex = index + 1
        onChangeListener?.invoke()
    }

    fun selectLayer(index: Int) {
        if (index in layers.indices) { currentLayerIndex = index; onChangeListener?.invoke() }
    }

    fun toggleVisibility(index: Int) {
        if (index in layers.indices) { layers[index].visible = !layers[index].visible; onChangeListener?.invoke() }
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        if (index in layers.indices) { layers[index].opacity = opacity; invalidate() }
    }

    fun setTool(type: ToolType) { toolType = type; cancelOverlay() }
    fun setColor(color: Int) { paintColor = color }
    fun setStrokeWidth(width: Float) { strokeWidth = width }
    fun setOpacity(opacity: Float) { toolOpacity = opacity }
    fun setShapeFill(amount: Int) { shapeFillAmount = amount.coerceIn(0, 100) }
    fun setTextContent(text: String) { textContent = text }
    fun setTextFont(font: Typeface?) { textFont = font }
    fun setTextOutline(width: Float, color: Int) { textOutlineWidth = width; textOutlineColor = color }
    fun setTextGlow(radius: Float, color: Int) { textGlowRadius = radius; textGlowColor = color }
    fun setTextGradient(start: Int, end: Int) { textGradientStart = start; textGradientEnd = end }

    fun setOverlayRotation(deg: Float) {
        overlay?.rotation = deg
        invalidate()
    }
    fun setOverlayFlipX(flip: Boolean) {
        overlay?.flipX = flip
        invalidate()
    }
    fun setOverlayFlipY(flip: Boolean) {
        overlay?.flipY = flip
        invalidate()
    }
    fun getOverlayRotation(): Float = overlay?.rotation ?: 0f
    fun getOverlayFlipX(): Boolean = overlay?.flipX ?: false
    fun getOverlayFlipY(): Boolean = overlay?.flipY ?: false

    fun resetZoom() { userZoom = 1f; userPanX = 0f; userPanY = 0f; invalidate() }

    fun clearCurrentLayer() {
        val layer = getCurrentLayer() ?: return
        pushUndo(layer)
        val c = Canvas(layer.bitmap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            c.drawColor(Color.TRANSPARENT, BlendMode.CLEAR)
        } else {
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }
        invalidate(); onChangeListener?.invoke()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (undoStack.isEmpty()) return
        val (index, snapshot) = undoStack.removeLast()
        val layer = layers.getOrNull(index) ?: run { snapshot.recycle(); return }
        val currentBmp = ensureMutable(layer.bitmap)
        redoStack.add(Pair(index, currentBmp))
        while (redoStack.size > MAX_UNDO_STEPS) {
            val removed = redoStack.removeFirst()
            removed.second.recycle()
        }
        layer.bitmap = ensureMutable(snapshot)
        invalidate(); onChangeListener?.invoke()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val (index, snapshot) = redoStack.removeLast()
        val layer = layers.getOrNull(index) ?: run { snapshot.recycle(); return }
        val currentBmp = ensureMutable(layer.bitmap)
        undoStack.add(Pair(index, currentBmp))
        while (undoStack.size > MAX_UNDO_STEPS) {
            val removed = undoStack.removeFirst()
            removed.second.recycle()
        }
        layer.bitmap = ensureMutable(snapshot)
        invalidate(); onChangeListener?.invoke()
    }

    fun flipHorizontal() = transform { it.flipHorizontal() }
    fun flipVertical() = transform { it.flipVertical() }
    fun rotate90Cw() = transform { it.rotate90Cw() }

    private fun transform(operation: (Bitmap) -> Bitmap) {
        val layer = getCurrentLayer() ?: return
        pushUndo(layer)
        val oldBmp = layer.bitmap
        layer.bitmap = operation(ensureMutable(oldBmp))
        if (oldBmp !== layer.bitmap) {
            oldBmp.recycle()
        }
        invalidate(); onChangeListener?.invoke()
    }

    fun getCompositeBitmap(): Bitmap {
        val result = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val c = Canvas(result)
        for (layer in layers) {
            if (!layer.visible) continue
            layerPaint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            c.drawBitmap(layer.bitmap, 0f, 0f, layerPaint)
        }
        return result
    }

    fun copyCurrentLayerBitmap(): Bitmap? = getCurrentLayer()?.let { it.bitmap.copy(Bitmap.Config.ARGB_8888, true) }

    fun pasteBitmapAsNewLayer(bmp: Bitmap) {
        val scaled = Bitmap.createScaledBitmap(bmp, bitmapWidth, bitmapHeight, true)
        layers.add(PaintLayer(scaled, "Pasted"))
        currentLayerIndex = layers.size - 1
        onChangeListener?.invoke(); invalidate()
    }

    fun resizeCanvas(newW: Int, newH: Int) {
        val w = newW.coerceAtLeast(64); val h = newH.coerceAtLeast(64)
        if (w == bitmapWidth && h == bitmapHeight) return
        for (layer in layers) {
            val old = layer.bitmap
            val newBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(newBmp).drawBitmap(old, 0f, 0f, null)
            layer.bitmap = newBmp
            if (old !== newBmp) old.recycle()
        }
        bitmapWidth = w; bitmapHeight = h
        computeFitTransform(); invalidate(); onChangeListener?.invoke()
    }

    fun commitOverlay() {
        val o = overlay ?: return
        val layer = getCurrentLayer() ?: return
        pushUndo(layer)
        val c = Canvas(layer.bitmap)
        val hw = o.w / 2f
        val hh = o.h / 2f
        c.save()
        c.translate(o.cx, o.cy)
        if (o.rotation != 0f) c.rotate(o.rotation)
        if (o.flipX) c.scale(-1f, 1f)
        if (o.flipY) c.scale(1f, -1f)
        applyPaint(false)
        val l2 = -hw; val t2 = -hh; val r2 = hw; val b2 = hh
        when (o.type) {
            ToolType.RECTANGLE -> drawRectOrFill(c, l2, t2, r2, b2)
            ToolType.OVAL -> drawOvalOrFill(c, l2, t2, r2, b2)
            ToolType.STAR -> drawStarOrFill(c, l2, t2, r2, b2)
            ToolType.HEART -> drawHeartOrFill(c, l2, t2, r2, b2)
            ToolType.TEXT -> drawTextOverlay(c, 0f, 0f)
            else -> {}
        }
        c.restore()
        overlay = null
        onShowConfirmButtons?.invoke(false)
        invalidate(); onChangeListener?.invoke()
    }

    fun cancelOverlay() {
        overlay = null
        onShowConfirmButtons?.invoke(false)
        invalidate()
    }

    private fun computeFitTransform() {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || width == 0 || height == 0) return
        fitScale = min(width.toFloat() / bitmapWidth, height.toFloat() / bitmapHeight)
        fitOffsetX = (width - bitmapWidth * fitScale) / 2f
        fitOffsetY = (height - bitmapHeight * fitScale) / 2f
    }

    private fun toScreenX(bx: Float): Float = bx * fitScale * userZoom + fitOffsetX + userPanX
    private fun toScreenY(by: Float): Float = by * fitScale * userZoom + fitOffsetY + userPanY
    private fun toBitmapX(sx: Float): Float = (sx - fitOffsetX - userPanX) / (fitScale * userZoom)
    private fun toBitmapY(sy: Float): Float = (sy - fitOffsetY - userPanY) / (fitScale * userZoom)
    private fun scaleToScreen(len: Float): Float = len * fitScale * userZoom
    private fun scaleToBitmap(len: Float): Float = len / (fitScale * userZoom)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeFitTransform()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0 || bitmapWidth <= 0 || bitmapHeight <= 0) return
        canvas.drawColor(0xFF2B2B2B.toInt())
        canvas.save()
        canvas.translate(fitOffsetX + userPanX, fitOffsetY + userPanY)
        canvas.scale(fitScale * userZoom, fitScale * userZoom)

        for (layer in layers) {
            if (!layer.visible) continue
            layerPaint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            canvas.drawBitmap(layer.bitmap, 0f, 0f, layerPaint)
        }

        val o = overlay
        if (o != null) {
            val hw = o.w / 2f
            val hh = o.h / 2f
            val l = o.cx - hw
            val t = o.cy - hh
            val r = o.cx + hw
            val b = o.cy + hh

            canvas.save()
            canvas.translate(o.cx, o.cy)
            if (o.rotation != 0f) canvas.rotate(o.rotation)
            if (o.flipX) canvas.scale(-1f, 1f)
            if (o.flipY) canvas.scale(1f, -1f)

            canvas.drawRect(-hw, -hh, hw, hh, overlayFill)
            canvas.drawRect(-hw, -hh, hw, hh, overlayStroke)

            applyPaint(false)
            when (o.type) {
                ToolType.RECTANGLE -> canvas.drawRect(-hw, -hh, hw, hh, if (shapeFillAmount > 0) fillPaint else drawPaint)
                ToolType.OVAL -> canvas.drawOval(-hw, -hh, hw, hh, if (shapeFillAmount > 0) fillPaint else drawPaint)
                ToolType.STAR -> drawStarPath(canvas, -hw, -hh, hw, hh)
                ToolType.HEART -> drawHeartPath(canvas, -hw, -hh, hw, hh)
                ToolType.TEXT -> drawTextOverlay(canvas, 0f, 0f)
                else -> {}
            }


            val handleR = 8f / (fitScale * userZoom)
            val handlePositions = listOf(
                0f to -hh,
                0f to hh,
                -hw to 0f,
                hw to 0f,
                -hw to -hh,
                hw to -hh,
                -hw to hh,
                hw to hh
            )
            for ((hx, hy) in handlePositions) {
                handlePaint.style = Paint.Style.FILL
                handlePaint.color = Color.WHITE
                canvas.drawCircle(hx, hy, handleR, handlePaint)
                handlePaint.style = Paint.Style.STROKE
                handlePaint.strokeWidth = 2f / (fitScale * userZoom)
                handlePaint.color = 0xFF333333.toInt()
                canvas.drawCircle(hx, hy, handleR, handlePaint)
            }

            canvas.restore()
        }

        canvas.restore()


        if (showPreview && previewX >= 0 && previewY >= 0 &&
            (toolType == ToolType.BRUSH || toolType == ToolType.ERASER || toolType == ToolType.SMUDGE)) {
            val sx = toScreenX(previewX)
            val sy = toScreenY(previewY)
            val sr = strokeWidth * fitScale * userZoom / 2f
            previewPaint.color = if (toolType == ToolType.ERASER) 0xFFFFFFFF.toInt() else (paintColor and 0x00FFFFFF or 0x88000000.toInt())
            canvas.drawCircle(sx, sy, sr.coerceAtLeast(4f), previewPaint)
        }
    }





    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (layers.isEmpty()) return false


        if (event.pointerCount >= 2) {
            cancelActiveTouchGesture()
            handlePinch(event)
            return true
        }

        val bx = toBitmapX(event.x)
        val by = toBitmapY(event.y)


        if (event.action == MotionEvent.ACTION_CANCEL) {
            cancelActiveTouchGesture()
            showPreview = false
            invalidate()
            return true
        }


        if (toolType == ToolType.BRUSH || toolType == ToolType.ERASER || toolType == ToolType.SMUDGE) {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    showPreview = true; previewX = bx; previewY = by; invalidate()
                }
                MotionEvent.ACTION_UP -> { showPreview = false; invalidate() }
            }
        }


        if (overlay != null && (toolType in SHAPE_TOOLS || toolType == ToolType.TEXT)) {
            handleOverlayTouch(event, bx, by)
            return true
        }

        when (toolType) {
            ToolType.BRUSH -> handleDraw(event, bx, by, false)
            ToolType.ERASER -> handleDraw(event, bx, by, true)
            ToolType.SMUDGE -> handleSmudge(event, bx, by)
            ToolType.FILL -> if (event.action == MotionEvent.ACTION_DOWN) {
                getCurrentLayer()?.let {
                    pushUndo(it)
                    floodFill(it.bitmap, bx.toInt(), by.toInt(), paintColor)
                }
                invalidate(); onChangeListener?.invoke()
            }
            ToolType.EYEDROPPER -> if (event.action == MotionEvent.ACTION_DOWN) {
                onColorPickedListener?.invoke(pickColor(bx.toInt(), by.toInt()))
            }
            ToolType.LINE -> handleDrawLine(event, bx, by)
            ToolType.RECTANGLE, ToolType.OVAL, ToolType.STAR, ToolType.HEART -> if (event.action == MotionEvent.ACTION_DOWN) {
                startShapeOverlay(toolType, bx, by)
            }
            ToolType.TEXT -> if (event.action == MotionEvent.ACTION_DOWN) {
                startTextOverlay(bx, by)
            }
            ToolType.SPRAY_CAN -> handleSpray(event, bx, by)
            ToolType.CLIPBOARD, ToolType.ZOOM -> {}
        }
        return true
    }






    private fun cancelActiveTouchGesture() {
        shapeSnapshot?.recycle()
        shapeSnapshot = null
        smudgeSrc = null
        showPreview = false
        overlayTouchMode = OverlayTouchMode.NONE
        activeHandle = ResizeHandle.NONE
    }





    private fun handlePinch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                isPinching = true
                lastPinchDist = pinchDistance(event)
                pinchCenterSx = (event.getX(0) + event.getX(1)) / 2f
                pinchCenterSy = (event.getY(0) + event.getY(1)) / 2f
                pinchCenterBx = toBitmapX(pinchCenterSx)
                pinchCenterBy = toBitmapY(pinchCenterSy)
                if (overlay != null) {
                    prePinchOverlayW = overlay!!.w
                    prePinchOverlayH = overlay!!.h
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isPinching) return
                val dist = pinchDistance(event)
                if (dist > 10f && lastPinchDist > 10f) {
                    val factor = dist / lastPinchDist
                    if (overlay != null) {
                        overlay!!.w = (prePinchOverlayW * factor).coerceAtLeast(10f)
                        overlay!!.h = (prePinchOverlayH * factor).coerceAtLeast(10f)
                    } else {
                        val oldZoom = userZoom
                        userZoom = (userZoom * factor).coerceIn(0.3f, 20f)

                        userPanX = pinchCenterSx - fitOffsetX - pinchCenterBx * fitScale * userZoom
                        userPanY = pinchCenterSy - fitOffsetY - pinchCenterBy * fitScale * userZoom
                    }
                    lastPinchDist = dist
                    invalidate()
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                isPinching = false
            }
        }
    }

    private fun pinchDistance(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return sqrt(dx * dx + dy * dy)
    }





    private fun startShapeOverlay(type: ToolType, bx: Float, by: Float) {
        val initW = 100f / (fitScale * userZoom).coerceAtLeast(0.1f)
        val initH = 100f / (fitScale * userZoom).coerceAtLeast(0.1f)
        overlay = OverlayShape(type, bx, by, initW, initH, paintColor, shapeFillAmount)
        overlayTouchMode = OverlayTouchMode.MOVE
        overlayInitCenterX = bx
        overlayInitCenterY = by
        overlayInitW = initW
        overlayInitH = initH
        overlayInitZoom = userZoom
        invalidate()
        scheduleIdle()
    }

    private fun startTextOverlay(bx: Float, by: Float) {
        val initW = 200f / (fitScale * userZoom).coerceAtLeast(0.1f)
        val initH = 60f / (fitScale * userZoom).coerceAtLeast(0.1f)
        overlay = OverlayShape(ToolType.TEXT, bx, by, initW, initH, paintColor, shapeFillAmount)
        overlayTouchMode = OverlayTouchMode.MOVE
        overlayInitCenterX = bx
        overlayInitCenterY = by
        overlayInitW = initW
        overlayInitH = initH
        overlayInitZoom = userZoom
        onRequestTextListener?.invoke(bx, by)
        invalidate()
        scheduleIdle()
    }


    private fun toLocalCoords(bx: Float, by: Float, o: OverlayShape): Pair<Float, Float> {
        val dx = bx - o.cx
        val dy = by - o.cy
        val rotRad = Math.toRadians(o.rotation.toDouble())
        val cosR = cos(rotRad).toFloat()
        val sinR = sin(rotRad).toFloat()

        var lx = cosR * dx + sinR * dy
        var ly = -sinR * dx + cosR * dy

        if (o.flipX) lx = -lx
        if (o.flipY) ly = -ly
        return lx to ly
    }

    private fun detectHandle(o: OverlayShape, lx: Float, ly: Float): ResizeHandle {
        val hw = o.w / 2f
        val hh = o.h / 2f
        val hitR = 18f / (fitScale * userZoom)
        val handles = listOf(
            ResizeHandle.TOP to Pair(0f, -hh),
            ResizeHandle.BOTTOM to Pair(0f, hh),
            ResizeHandle.LEFT to Pair(-hw, 0f),
            ResizeHandle.RIGHT to Pair(hw, 0f),
            ResizeHandle.TOP_LEFT to Pair(-hw, -hh),
            ResizeHandle.TOP_RIGHT to Pair(hw, -hh),
            ResizeHandle.BOTTOM_LEFT to Pair(-hw, hh),
            ResizeHandle.BOTTOM_RIGHT to Pair(hw, hh)
        )
        var best = ResizeHandle.NONE
        var bestDist = hitR
        for ((handle, pos) in handles) {
            val d = sqrt((lx - pos.first) * (lx - pos.first) + (ly - pos.second) * (ly - pos.second))
            if (d < bestDist) {
                bestDist = d
                best = handle
            }
        }
        return best
    }

    private fun handleOverlayTouch(event: MotionEvent, bx: Float, by: Float) {
        cancelIdle()
        val o = overlay ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                val (lx, ly) = toLocalCoords(bx, by, o)
                val hit = detectHandle(o, lx, ly)
                if (hit != ResizeHandle.NONE) {
                    overlayTouchMode = OverlayTouchMode.RESIZE
                    activeHandle = hit
                } else {
                    overlayTouchMode = OverlayTouchMode.MOVE
                }
                overlayStartX = bx
                overlayStartY = by
                overlayInitCenterX = o.cx
                overlayInitCenterY = o.cy
                overlayInitW = o.w
                overlayInitH = o.h
                overlayInitZoom = userZoom
            }
            MotionEvent.ACTION_MOVE -> {
                val dbx = bx - overlayStartX
                val dby = by - overlayStartY

                val rotRad = Math.toRadians(o.rotation.toDouble())
                val cosR = cos(rotRad).toFloat()
                val sinR = sin(rotRad).toFloat()
                var ldx = cosR * dbx + sinR * dby
                var ldy = -sinR * dbx + cosR * dby
                if (o.flipX) ldx = -ldx
                if (o.flipY) ldy = -ldy

                if (overlayTouchMode == OverlayTouchMode.RESIZE) {
                    val minSz = 10f
                    val dh = when (activeHandle) {
                        ResizeHandle.TOP, ResizeHandle.TOP_LEFT, ResizeHandle.TOP_RIGHT -> -ldy
                        ResizeHandle.BOTTOM, ResizeHandle.BOTTOM_LEFT, ResizeHandle.BOTTOM_RIGHT -> ldy
                        else -> 0f
                    }
                    val dw = when (activeHandle) {
                        ResizeHandle.LEFT, ResizeHandle.TOP_LEFT, ResizeHandle.BOTTOM_LEFT -> -ldx
                        ResizeHandle.RIGHT, ResizeHandle.TOP_RIGHT, ResizeHandle.BOTTOM_RIGHT -> ldx
                        else -> 0f
                    }
                    val newW = (overlayInitW + dw).coerceAtLeast(minSz)
                    val newH = (overlayInitH + dh).coerceAtLeast(minSz)
                    o.w = newW
                    o.h = newH


                    val clx = ldx / 2f
                    val cly = ldy / 2f
                    o.cx = overlayInitCenterX + cosR * clx - sinR * cly
                    o.cy = overlayInitCenterY + sinR * clx + cosR * cly
                } else {

                    var newCx = overlayInitCenterX + dbx
                    var newCy = overlayInitCenterY + dby
                    newCx = newCx.coerceIn(o.w / 2f, bitmapWidth - o.w / 2f)
                    newCy = newCy.coerceIn(o.h / 2f, bitmapHeight - o.h / 2f)
                    o.cx = newCx
                    o.cy = newCy
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                overlayTouchMode = OverlayTouchMode.NONE
                activeHandle = ResizeHandle.NONE
                scheduleIdle()
            }
        }
    }

    private fun scheduleIdle() {
        cancelIdle()
        idlePending = true
        idleHandler.postDelayed({
            idlePending = false
            if (overlay != null) {
                onShowConfirmButtons?.invoke(true)
            }
        }, 600)
    }

    private fun cancelIdle() {
        idlePending = false
        idleHandler.removeCallbacksAndMessages(null)
    }





    private fun pushUndo(layer: PaintLayer) {
        val copy = try {
            layer.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: OutOfMemoryError) {

            null
        }
        if (copy != null) {
            undoStack.add(Pair(currentLayerIndex, copy))
            while (undoStack.size > MAX_UNDO_STEPS) {
                val removed = undoStack.removeFirst()
                removed.second.recycle()
            }
        }
        clearRedoStack()
    }

    private fun handleDraw(event: MotionEvent, bx: Float, by: Float, erase: Boolean) {
        val layer = getCurrentLayer() ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pushUndo(layer); lastX = bx; lastY = by
                applyPaint(erase)
                Canvas(layer.bitmap).drawPoint(bx, by, drawPaint)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                applyPaint(erase)
                Canvas(layer.bitmap).drawLine(lastX, lastY, bx, by, drawPaint)
                lastX = bx; lastY = by
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                onChangeListener?.invoke()
            }
        }
    }

    private fun handleDrawLine(event: MotionEvent, bx: Float, by: Float) {
        val layer = getCurrentLayer() ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pushUndo(layer)
                shapeSnapshot = layer.bitmap.copy(Bitmap.Config.ARGB_8888, true)
                startX = bx; startY = by; lastX = bx; lastY = by
            }
            MotionEvent.ACTION_MOVE -> {
                restoreFrom(shapeSnapshot)
                applyPaint(false); Canvas(layer.bitmap).drawLine(startX, startY, bx, by, drawPaint)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                restoreFrom(shapeSnapshot)
                applyPaint(false); Canvas(layer.bitmap).drawLine(startX, startY, bx, by, drawPaint)
                shapeSnapshot?.recycle()
                shapeSnapshot = null; invalidate(); onChangeListener?.invoke()
            }
        }
    }

    private fun handleSmudge(event: MotionEvent, bx: Float, by: Float) {
        val layer = getCurrentLayer() ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pushUndo(layer)
                smudgeSrc = ensureMutable(layer.bitmap)
                lastX = bx; lastY = by
            }
            MotionEvent.ACTION_MOVE -> {
                val src = smudgeSrc ?: return
                val r = (strokeWidth / 2 + 6).toInt().coerceAtLeast(3)
                if (bitmapWidth < 2 * r || bitmapHeight < 2 * r) return
                val sx = (lastX - r).toInt().coerceIn(0, bitmapWidth - 2 * r)
                val sy = (lastY - r).toInt().coerceIn(0, bitmapHeight - 2 * r)
                val sw = 2 * r; val sh = 2 * r
                if (smudgeScratchBitmap == null || smudgeScratchBitmap!!.width != sw || smudgeScratchBitmap!!.height != sh) {
                    smudgeScratchBitmap?.recycle()
                    smudgeScratchBitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                    smudgeScratchCanvas = Canvas(smudgeScratchBitmap!!)
                }
                smudgeScratchCanvas!!.drawBitmap(src, -sx.toFloat(), -sy.toFloat(), null)
                val dx = (bx - r).coerceIn(0f, (bitmapWidth - 2 * r).toFloat())
                val dy = (by - r).coerceIn(0f, (bitmapHeight - 2 * r).toFloat())
                val p = Paint().apply { alpha = (toolOpacity * 160).toInt().coerceIn(0, 255) }
                Canvas(layer.bitmap).drawBitmap(smudgeScratchBitmap!!, dx, dy, p)
                lastX = bx; lastY = by; invalidate()
            }
            MotionEvent.ACTION_UP -> { smudgeSrc = null; onChangeListener?.invoke() }
        }
    }

    private fun handleSpray(event: MotionEvent, bx: Float, by: Float) {
        val layer = getCurrentLayer() ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pushUndo(layer); sprayAt(layer, bx, by, 30); lastX = bx; lastY = by; invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                sprayAt(layer, bx, by, 15); lastX = bx; lastY = by; invalidate()
            }
            MotionEvent.ACTION_UP -> onChangeListener?.invoke()
        }
    }

    private fun sprayAt(layer: PaintLayer, cx: Float, cy: Float, density: Int) {
        val radius = (strokeWidth * 2).coerceAtLeast(8f)
        applyPaint(false)
        drawPaint.style = Paint.Style.FILL
        val c = Canvas(layer.bitmap)
        for (i in 0 until density) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val dist = Random.nextFloat() * radius
            val x = cx + cos(angle) * dist
            val y = cy + sin(angle) * dist
            c.drawCircle(x, y, (Random.nextFloat() * 3 + 1).coerceAtMost(strokeWidth / 2), drawPaint)
        }
        drawPaint.style = Paint.Style.STROKE
    }

    private fun restoreFrom(snapshot: Bitmap?) {
        val layer = getCurrentLayer() ?: return
        if (snapshot == null) return
        val oldBmp = layer.bitmap
        layer.bitmap = snapshot.copy(Bitmap.Config.ARGB_8888, true)
        if (oldBmp !== layer.bitmap) {
            oldBmp.recycle()
        }
    }





    private fun drawRectOrFill(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        if (shapeFillAmount > 0) {
            fillPaint.color = drawPaint.color; fillPaint.alpha = drawPaint.alpha * shapeFillAmount / 100
            c.drawRect(l, t, r, b, fillPaint)
        }
        if (shapeFillAmount < 100) c.drawRect(l, t, r, b, drawPaint)
    }

    private fun drawOvalOrFill(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        if (shapeFillAmount > 0) {
            fillPaint.color = drawPaint.color; fillPaint.alpha = drawPaint.alpha * shapeFillAmount / 100
            c.drawOval(l, t, r, b, fillPaint)
        }
        if (shapeFillAmount < 100) c.drawOval(l, t, r, b, drawPaint)
    }

    private fun drawStarOrFill(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        drawStarPath(c, l, t, r, b)
        if (shapeFillAmount < 100) {
            applyPaint(false)
            drawPaint.style = Paint.Style.STROKE
            c.drawPath(starPath, drawPaint)
            drawPaint.style = Paint.Style.STROKE
        }
    }

    private fun drawHeartOrFill(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        drawHeartPath(c, l, t, r, b)
        if (shapeFillAmount < 100) {
            applyPaint(false)
            drawPaint.style = Paint.Style.STROKE
            c.drawPath(heartPath, drawPaint)
            drawPaint.style = Paint.Style.STROKE
        }
    }

    private fun drawStarPath(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val cx = (l + r) / 2f; val cy = (t + b) / 2f
        val outerR = max(r - l, b - t) / 2f; val innerR = outerR * 0.4f
        starPath.reset()
        for (i in 0 until 10) {
            val angle = (i * 36 - 90).toDouble() * Math.PI / 180.0
            val radius = if (i % 2 == 0) outerR else innerR
            val x = cx + (cos(angle) * radius).toFloat(); val y = cy + (sin(angle) * radius).toFloat()
            if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
        }
        starPath.close()
        if (shapeFillAmount > 0) {
            fillPaint.color = paintColor; fillPaint.alpha = (toolOpacity * 255).toInt() * shapeFillAmount / 100
            c.drawPath(starPath, fillPaint)
        }
    }

    private fun drawHeartPath(c: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val cx = (l + r) / 2f; val cy = (t + b) / 2f
        val hw = (r - l) / 2f; val hh = (b - t) / 2f
        heartPath.reset()
        heartPath.moveTo(cx, (cy + hh * 0.3f))
        heartPath.cubicTo(cx - hw * 1.2f, cy - hh * 0.6f, cx - hw * 0.3f, cy - hh * 1.1f, cx, cy - hh * 0.4f)
        heartPath.cubicTo(cx + hw * 0.3f, cy - hh * 1.1f, cx + hw * 1.2f, cy - hh * 0.6f, cx, (cy + hh * 0.3f))
        heartPath.close()
        if (shapeFillAmount > 0) {
            fillPaint.color = paintColor; fillPaint.alpha = (toolOpacity * 255).toInt() * shapeFillAmount / 100
            c.drawPath(heartPath, fillPaint)
        }
    }

    private fun drawTextOverlay(c: Canvas, cx: Float, cy: Float) {
        val text = textContent.ifBlank { "Text" }
        val size = (strokeWidth * 4).coerceAtLeast(24f) * (overlay?.w?.div(200f) ?: 1f)
        textPaint.color = paintColor
        textPaint.alpha = (toolOpacity * 255).toInt().coerceIn(0, 255)
        textPaint.textSize = size
        if (textFont != null) textPaint.typeface = textFont


        if (textGradientStart != Color.TRANSPARENT && textGradientEnd != Color.TRANSPARENT) {
            textPaint.shader = android.graphics.LinearGradient(
                0f, 0f, (overlay?.w ?: size * text.length).coerceAtLeast(1f), 0f,
                textGradientStart, textGradientEnd, Shader.TileMode.CLAMP
            )
        } else {
            textPaint.shader = null
        }


        if (textGlowRadius > 0f) {
            textPaint.setShadowLayer(textGlowRadius, 0f, 0f, textGlowColor)
        } else {
            textPaint.setShadowLayer(0f, 0f, 0f, 0)
        }

        val tw = textPaint.measureText(text)


        if (textOutlineWidth > 0f) {
            textOutlinePaint.set(textPaint)
            textOutlinePaint.style = Paint.Style.STROKE
            textOutlinePaint.strokeWidth = textOutlineWidth
            textOutlinePaint.color = textOutlineColor
            textOutlinePaint.shader = null
            textOutlinePaint.clearShadowLayer()
            c.drawText(text, cx - tw / 2f, cy + size / 3f, textOutlinePaint)
        }


        c.drawText(text, cx - tw / 2f, cy + size / 3f, textPaint)


        textPaint.clearShadowLayer()
    }

    private fun Paint.clearShadowLayer() {
        setShadowLayer(0f, 0f, 0f, 0)
    }





    private fun applyPaint(erase: Boolean) {
        drawPaint.strokeWidth = strokeWidth
        if (erase) {
            drawPaint.color = Color.WHITE
            drawPaint.alpha = (toolOpacity * 255).toInt().coerceIn(0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                drawPaint.xfermode = null
                drawPaint.blendMode = BlendMode.DST_OUT
                drawPaint.colorFilter = null
            } else {
                drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                drawPaint.colorFilter = null
            }
        } else {
            drawPaint.color = paintColor
            drawPaint.alpha = (toolOpacity * 255).toInt().coerceIn(0, 255)
            drawPaint.xfermode = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                drawPaint.blendMode = null
            }
            drawPaint.colorFilter = null
        }
    }





    private fun pickColor(x: Int, y: Int): Int {
        val px = x.coerceIn(0, bitmapWidth - 1); val py = y.coerceIn(0, bitmapHeight - 1)
        for (layer in layers.reversed()) {
            if (!layer.visible) continue
            val c = layer.bitmap.getPixel(px, py)
            if (c != Color.TRANSPARENT) return c
        }
        return Color.TRANSPARENT
    }





    private fun floodFill(bmp: Bitmap, x: Int, y: Int, newColor: Int) {
        val w = bmp.width; val h = bmp.height
        if (x < 0 || y < 0 || x >= w || y >= h) return

        if (w.toLong() * h > MAX_FLOOD_FILL_DIM.toLong() * MAX_FLOOD_FILL_DIM) return
        val oldColor = bmp.getPixel(x, y)
        if (oldColor == newColor) return
        val pixels = try {
            IntArray(w * h)
        } catch (e: OutOfMemoryError) {
            return
        }
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val stack = ArrayDeque<Int>(); stack.add(y * w + x)
        val t = 40
        val oA = (oldColor shr 24) and 0xff; val oR = (oldColor shr 16) and 0xff
        val oG = (oldColor shr 8) and 0xff; val oB = oldColor and 0xff
        
        fun tolMatch(c: Int): Boolean =
            abs(((c shr 24) and 0xff) - oA) <= t && abs(((c shr 16) and 0xff) - oR) <= t &&
            abs(((c shr 8) and 0xff) - oG) <= t && abs((c and 0xff) - oB) <= t
        

        pixels[y * w + x] = newColor
        
        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            val px = idx % w; val py = idx / w
            if (px > 0) { val ni = idx - 1; val nc = pixels[ni]
                if (nc != newColor && tolMatch(nc)) { pixels[ni] = newColor; stack.add(ni) } }
            if (px < w - 1) { val ni = idx + 1; val nc = pixels[ni]
                if (nc != newColor && tolMatch(nc)) { pixels[ni] = newColor; stack.add(ni) } }
            if (py > 0) { val ni = idx - w; val nc = pixels[ni]
                if (nc != newColor && tolMatch(nc)) { pixels[ni] = newColor; stack.add(ni) } }
            if (py < h - 1) { val ni = idx + w; val nc = pixels[ni]
                if (nc != newColor && tolMatch(nc)) { pixels[ni] = newColor; stack.add(ni) } }
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





    data class OverlayShape(
        val type: ToolType,
        var cx: Float, var cy: Float,
        var w: Float, var h: Float,
        var color: Int,
        var fillAmount: Int,
        var rotation: Float = 0f,
        var flipX: Boolean = false,
        var flipY: Boolean = false
    )

    private enum class OverlayTouchMode { NONE, MOVE, RESIZE }
    private enum class ResizeHandle {
        NONE, TOP, BOTTOM, LEFT, RIGHT,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    companion object {
        private val SHAPE_TOOLS = setOf(
            ToolType.RECTANGLE, ToolType.OVAL, ToolType.STAR, ToolType.HEART
        )

        private const val MAX_UNDO_STEPS = 15

        private const val MAX_FLOOD_FILL_DIM = 2048
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelIdle()
        smudgeScratchBitmap?.recycle()
        smudgeScratchBitmap = null
        smudgeScratchCanvas = null
        smudgeSrc?.recycle()
        smudgeSrc = null
        clearUndoStack()
        clearRedoStack()
        for (l in layers) {
            l.bitmap.recycle()
        }
        layers.clear()
    }
}
