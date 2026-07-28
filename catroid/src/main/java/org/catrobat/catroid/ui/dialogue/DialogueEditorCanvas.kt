package org.catrobat.catroid.ui.dialogue

import android.content.Context
import android.graphics.*
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import org.catrobat.catroid.dialogue.DialogueTree
import org.catrobat.catroid.dialogue.DialogueNode
import java.util.*
import kotlin.math.sqrt

class DialogueEditorCanvas(context: Context) : View(context) {

    lateinit var editorActivity: DialogueEditorActivity
    private var dialogueTree: DialogueTree = DialogueTree()

    private var offsetX = 0f
    private var offsetY = 0f
    private var scale = 1f
    private val minScale = 0.15f
    private val maxScale = 3f

    private val nodeWidth = 180f
    private val nodeHeight = 80f
    private val portRadius = 10f
    private val cornerRadius = 12f

    private var draggedNodeId: String? = null
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var nodeStartX = 0f
    private var nodeStartY = 0f
    private var isDragging = false

    var selectedNodeId: String? = null
        private set

    var connectingFromId: String? = null
        private set
    private var connectingFromPort: Int = 0

    private val visibleNodes = mutableListOf<DialogueNode>()
    private val visibleConnections = mutableListOf<Triple<DialogueNode, DialogueNode, Int>>()

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 28f; isFakeBoldText = true }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 22f }
    private val tinyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 16f }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = 3f; style = Paint.Style.STROKE }
    private val connectionLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN; strokeWidth = 3f; style = Paint.Style.STROKE }
    private val portPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN; style = Paint.Style.FILL }
    private val bgPaint = Paint().apply { color = Color.rgb(30, 30, 35) }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(45, 45, 50); strokeWidth = 1f; style = Paint.Style.STROKE }
    private val minimapBgPaint = Paint().apply { color = Color.argb(180, 20, 20, 25) }
    private val minimapNodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val minimapViewportPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1f }
    private val searchHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 255, 215, 0); style = Paint.Style.FILL }

    private val nodeColors = mapOf(
        "Start" to Color.rgb(34, 139, 34), "Dialogue" to Color.rgb(70, 70, 180),
        "Choice" to Color.rgb(180, 130, 30), "Condition" to Color.rgb(180, 60, 60),
        "Action" to Color.rgb(60, 150, 150), "End" to Color.rgb(120, 30, 30),
        "Comment" to Color.rgb(100, 100, 100)
    )
    private val minimapColors = nodeColors

    var searchTerm: String = ""
        set(value) { field = value; invalidate() }

    private var miniMapRect = RectF()

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var focusX = 0f; private var focusY = 0f
        override fun onScaleBegin(d: ScaleGestureDetector): Boolean { focusX = d.focusX; focusY = d.focusY; return true }
        override fun onScale(d: ScaleGestureDetector): Boolean {
            val old = scale; scale = (scale * d.scaleFactor).coerceIn(minScale, maxScale)
            val r = scale / old; offsetX = (offsetX - focusX / old) * r + focusX / scale
            offsetY = (offsetY - focusY / old) * r + focusY / scale; invalidate(); return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (miniMapRect.contains(e.x, e.y)) { jumpToMinimapPosition(e.x, e.y); return true }
            val wx = (e.x / scale - offsetX); val wy = (e.y / scale - offsetY)
            val hitNode = findNodeAt(wx, wy)
            val hitPort = findPortAt(wx, wy)
            if (hitPort != null) {
                if (connectingFromId != null) {
                    editorActivity.connectNodes(connectingFromId!!, hitPort.first, hitPort.second)
                    connectingFromId = null; invalidate()
                } else editorActivity.showEditNodeDialog(dialogueTree.getNode(hitPort.first)!!)
            } else if (hitNode != null) { selectedNodeId = hitNode.id; invalidate() }
            else { selectedNodeId = null; invalidate() }
            return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val wx = (e.x / scale - offsetX); val wy = (e.y / scale - offsetY)
            findNodeAt(wx, wy)?.let { editorActivity.showEditNodeDialog(it) }; return true
        }
        override fun onLongPress(e: MotionEvent) {
            val wx = (e.x / scale - offsetX); val wy = (e.y / scale - offsetY)
            findNodeAt(wx, wy)?.let { node ->
                val popup = android.widget.PopupMenu(context, this@DialogueEditorCanvas)
                popup.menu.add("Edit"); popup.menu.add("Delete"); popup.menu.add("Duplicate")
                popup.menu.add("Connect from here")
                popup.menu.add("Cancel connection").isEnabled = connectingFromId != null
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Edit" -> editorActivity.showEditNodeDialog(node)
                        "Delete" -> editorActivity.deleteNode(node.id)
                        "Duplicate" -> duplicateNode(node)
                        "Connect from here" -> { connectingFromId = node.id; invalidate() }
                        "Cancel connection" -> { connectingFromId = null; invalidate() }
                    }; true
                }; popup.show()
            }
        }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (draggedNodeId != null) {
                val n = dialogueTree.getNode(draggedNodeId!!) ?: return false
                updateNodePosition(draggedNodeId!!, n.x - dx / scale, n.y - dy / scale)
                isDragging = true; return true
            }
            offsetX -= dx / scale; offsetY -= dy / scale; invalidate(); return true
        }
    })

    fun setDialogueTree(tree: DialogueTree) { dialogueTree = tree; recalcVisible(); invalidate() }

    fun addNodeAtCenter(type: String) {
        val cx = (width / 2f / scale - offsetX); val cy = (height / 2f / scale - offsetY)
        val id = UUID.randomUUID().toString()
        val node: DialogueNode = when (type) {
            "Start" -> DialogueNode.StartNode(id, cx, cy)
            "Dialogue" -> DialogueNode.DialogueLine(id, cx, cy)
            "Choice" -> DialogueNode.ChoiceNode(id, cx, cy, mutableListOf(DialogueNode.Choice("Option A"), DialogueNode.Choice("Option B")))
            "Condition" -> DialogueNode.ConditionNode(id, cx, cy, "1 == 1")
            "Action" -> DialogueNode.ActionNode(id, cx, cy, mutableListOf(DialogueNode.ActionEntry("setVariable", "var", "0")))
            "End" -> DialogueNode.EndNode(id, cx, cy)
            "Comment" -> DialogueNode.CommentNode(id, cx, cy, "Comment")
            else -> DialogueNode.DialogueLine(id, cx, cy)
        }
        dialogueTree.nodes.add(node); selectedNodeId = id; recalcVisible(); invalidate()
    }

    private fun duplicateNode(node: DialogueNode) {
        editorActivity.pushUndo()
        val newId = UUID.randomUUID().toString()
        val newNode: DialogueNode = when (node) {
            is DialogueNode.StartNode -> node.copy(id = newId, x = node.x + 30f, y = node.y + 30f)
            is DialogueNode.DialogueLine -> node.copy(id = newId, x = node.x + 30f, y = node.y + 30f)
            is DialogueNode.ChoiceNode -> node.copy(id = newId, x = node.x + 30f, y = node.y + 30f, choices = node.choices.toMutableList())
            is DialogueNode.ConditionNode -> node.copy(id = newId, x = node.x + 30f, y = node.y + 30f)
            is DialogueNode.ActionNode -> node.copy(id = newId, x = node.x + 30f, y = node.y + 30f, actions = node.actions.toMutableList())
            is DialogueNode.EndNode -> node.copy(id = newId, x = node.x + 30f, y = node.y + 30f)
            is DialogueNode.CommentNode -> node.copy(id = newId, x = node.x + 30f, y = node.y + 30f)
        }
        dialogueTree.nodes.add(newNode); selectedNodeId = newId; recalcVisible(); invalidate()
    }

    fun updateNodePosition(id: String, x: Float, y: Float) {
        val idx = dialogueTree.nodes.indexOfFirst { it.id == id }; if (idx < 0) return
        dialogueTree.nodes[idx] = when (val n = dialogueTree.nodes[idx]) {
            is DialogueNode.StartNode -> n.copy(x = x, y = y)
            is DialogueNode.DialogueLine -> n.copy(x = x, y = y)
            is DialogueNode.ChoiceNode -> n.copy(x = x, y = y)
            is DialogueNode.ConditionNode -> n.copy(x = x, y = y)
            is DialogueNode.ActionNode -> n.copy(x = x, y = y)
            is DialogueNode.EndNode -> n.copy(x = x, y = y)
            is DialogueNode.CommentNode -> n.copy(x = x, y = y)
        }; recalcVisible(); invalidate()
    }

    fun centerOnNode(nodeId: String) {
        dialogueTree.getNode(nodeId)?.let {
            offsetX = -it.x + (width / 2f / scale) - nodeWidth / 2f
            offsetY = -it.y + (height / 2f / scale) - nodeHeight / 2f
            selectedNodeId = nodeId; recalcVisible(); invalidate()
        }
    }

    private fun findNodeAt(wx: Float, wy: Float): DialogueNode? = visibleNodes.findLast { n ->
        wx >= n.x && wx <= n.x + nodeWidth && wy >= n.y && wy <= n.y + nodeHeight
    }

    private fun findPortAt(wx: Float, wy: Float): Pair<String, Int>? {
        for (node in visibleNodes) {
            val oc = getOutputCount(node); val tw = oc * 30f; val sx = node.x + nodeWidth / 2f - tw / 2f
            for (i in 0 until oc) {
                val px = sx + i * 30f + 15f; val py = node.y + nodeHeight
                val dx = (wx - px).toDouble(); val dy = (wy - py).toDouble()
                if (sqrt(dx * dx + dy * dy) < 20.0) return Pair(node.id, i)
            }
            if (node !is DialogueNode.StartNode) {
                val px = node.x + nodeWidth / 2f; val py = node.y
                val dx = (wx - px).toDouble(); val dy = (wy - py).toDouble()
                if (sqrt(dx * dx + dy * dy) < 20.0) return Pair(node.id, -1)
            }
        }
        return null
    }

    private fun getOutputCount(node: DialogueNode): Int = when (node) {
        is DialogueNode.StartNode -> 1; is DialogueNode.DialogueLine -> 1; is DialogueNode.ChoiceNode -> node.choices.size
        is DialogueNode.ConditionNode -> 2; is DialogueNode.ActionNode -> 1; else -> 0
    }

    private fun recalcVisible() {
        val vpL = -offsetX; val vpT = -offsetY; val vpR = width / scale - offsetX; val vpB = height / scale - offsetY
        val margin = nodeWidth * 2f; visibleNodes.clear(); visibleConnections.clear()
        for (node in dialogueTree.nodes) {
            if (node.x + nodeWidth >= vpL - margin && node.x <= vpR + margin &&
                node.y + nodeHeight >= vpT - margin && node.y <= vpB + margin) visibleNodes.add(node)
        }
        for (node in dialogueTree.nodes) {
            for ((tid, pi) in getNodeOutputs(node)) {
                dialogueTree.getNode(tid)?.let { t ->
                    if (node in visibleNodes || t in visibleNodes) visibleConnections.add(Triple(node, t, pi))
                }
            }
        }
    }

    private fun getNodeOutputs(node: DialogueNode): List<Pair<String, Int>> {
        val r = mutableListOf<Pair<String, Int>>()
        when (node) {
            is DialogueNode.StartNode -> node.next?.let { r.add(Pair(it, 0)) }
            is DialogueNode.DialogueLine -> node.next?.let { r.add(Pair(it, 0)) }
            is DialogueNode.ActionNode -> node.next?.let { r.add(Pair(it, 0)) }
            is DialogueNode.ConditionNode -> { node.trueNext?.let { r.add(Pair(it, 0)) }; node.falseNext?.let { r.add(Pair(it, 1)) } }
            is DialogueNode.ChoiceNode -> node.choices.forEachIndexed { i, c -> c.next?.let { r.add(Pair(it, i)) } }
            else -> {}
        }; return r
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        canvas.save(); canvas.scale(scale, scale); canvas.translate(offsetX, offsetY)
        drawGrid(canvas); drawConnections(canvas)
        for (node in visibleNodes) drawNode(canvas, node)
        drawSearchHighlights(canvas)
        if (connectingFromId != null) drawConnectingLine(canvas)
        canvas.restore(); drawMinimap(canvas)
    }

    private fun drawConnectingLine(canvas: Canvas) {
        connectingFromId?.let { fid ->
            dialogueTree.getNode(fid)?.let { fn ->
                val oc = getOutputCount(fn); val tw = oc * 30f; val sx = fn.x + nodeWidth / 2f - tw / 2f
                val px = sx + connectingFromPort * 30f + 15f; val py = fn.y + nodeHeight
                val endX = (width / 2f / scale - offsetX); val endY = (height / 2f / scale - offsetY)
                val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.YELLOW; strokeWidth = 2f; style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                }
                val p = Path(); p.moveTo(px, py)
                val mid = (py + endY) / 2f; p.cubicTo(px, mid, endX, mid, endX, endY); canvas.drawPath(p, dp)
            }
        }
    }

    private fun drawSearchHighlights(canvas: Canvas) {
        if (searchTerm.isBlank()) return
        for (node in visibleNodes) {
            val t = when (node) {
                is DialogueNode.DialogueLine -> node.text; is DialogueNode.ChoiceNode -> node.choices.joinToString(" ") { it.text }
                is DialogueNode.ConditionNode -> node.expression; is DialogueNode.CommentNode -> node.text; else -> ""
            }
            if (t.contains(searchTerm, ignoreCase = true))
                canvas.drawRoundRect(node.x - 4f, node.y - 4f, node.x + nodeWidth + 4f, node.y + nodeHeight + 4f, cornerRadius + 4f, cornerRadius + 4f, searchHighlightPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val gs = 50f * (if (scale < 0.5f) 4f else if (scale < 0.8f) 2f else 1f)
        val sx = (-offsetX - offsetX % gs); val sy = (-offsetY - offsetY % gs)
        var x = sx; while (x < width / scale - offsetX + gs) { canvas.drawLine(x, -offsetY, x, height / scale - offsetY, gridPaint); x += gs }
        var y = sy; while (y < height / scale - offsetY + gs) { canvas.drawLine(-offsetX, y, width / scale - offsetX, y, gridPaint); y += gs }
    }

    private fun drawConnections(canvas: Canvas) {
        for ((from, to, pi) in visibleConnections) {
            val oc = getOutputCount(from); val tw = oc * 30f; val sx = from.x + nodeWidth / 2f - tw / 2f
            val startX = sx + pi * 30f + 15f; val startY = from.y + nodeHeight
            val endX = to.x + nodeWidth / 2f; val endY = to.y
            val hl = from.id == selectedNodeId || to.id == selectedNodeId
            val paint = if (hl) connectionLinePaint else linePaint
            val p = Path(); p.moveTo(startX, startY); val midY = (startY + endY) / 2f
            p.cubicTo(startX, midY, endX, midY, endX, endY); canvas.drawPath(p, paint)
            val a = 8f; val ap = Path()
            ap.moveTo(endX, endY); ap.lineTo(endX - a, endY - a); ap.lineTo(endX + a, endY - a); ap.close()
            canvas.drawPath(ap, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = paint.color; style = Paint.Style.FILL })
        }
    }

    private fun drawNode(canvas: Canvas, node: DialogueNode) {
        val base = nodeColors[node.typeName()] ?: Color.GRAY
        val sel = node.id == selectedNodeId
        val color = if (sel) Color.rgb(minOf(255, Color.red(base) + 40), minOf(255, Color.green(base) + 40), minOf(255, Color.blue(base) + 40)) else base
        nodePaint.color = color; nodePaint.style = Paint.Style.FILL
        canvas.drawRoundRect(node.x, node.y, node.x + nodeWidth, node.y + nodeHeight, cornerRadius, cornerRadius, nodePaint)
        if (sel) {
            selectedPaint.color = Color.WHITE; selectedPaint.style = Paint.Style.STROKE; selectedPaint.strokeWidth = 3f
            canvas.drawRoundRect(node.x, node.y, node.x + nodeWidth, node.y + nodeHeight, cornerRadius, cornerRadius, selectedPaint)
        }
        val label = when (node) {
            is DialogueNode.StartNode -> "▶ Start"; is DialogueNode.DialogueLine -> (if (node.textId.isNotEmpty()) "[${node.textId}] " else "") + (node.speaker.ifEmpty { "Dialogue" })
            is DialogueNode.ChoiceNode -> "Choice (${node.choices.size})"; is DialogueNode.ConditionNode -> "If..."
            is DialogueNode.ActionNode -> "Action (${node.actions.size})"; is DialogueNode.EndNode -> "■ End"; is DialogueNode.CommentNode -> "Comment"
        }
        canvas.drawText(label, node.x + 10f, node.y + 32f, textPaint)
        val sub = when (node) {
            is DialogueNode.DialogueLine -> node.text.take(28) + if (node.text.length > 28) "…" else ""
            is DialogueNode.ConditionNode -> node.expression.take(22); is DialogueNode.CommentNode -> node.text.take(22); else -> ""
        }
        if (sub.isNotEmpty()) canvas.drawText(sub, node.x + 10f, node.y + 62f, smallTextPaint)
        if (node !is DialogueNode.StartNode) {
            val ppx = node.x + nodeWidth / 2f; val ppy = node.y
            canvas.drawCircle(ppx, ppy, portRadius, portPaint)
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            ringPaint.color = Color.DKGRAY
            ringPaint.style = Paint.Style.STROKE
            ringPaint.strokeWidth = 2f
            canvas.drawCircle(ppx, ppy, portRadius, ringPaint)
        }
        val oc = getOutputCount(node); val tw = oc * 30f; val sx = node.x + nodeWidth / 2f - tw / 2f
        for (i in 0 until oc) {
            val qpx = sx + i * 30f + 15f; val qpy = node.y + nodeHeight
            canvas.drawCircle(qpx, qpy, portRadius, portPaint)
            val rp = Paint(Paint.ANTI_ALIAS_FLAG)
            rp.color = Color.DKGRAY
            rp.style = Paint.Style.STROKE
            rp.strokeWidth = 2f
            canvas.drawCircle(qpx, qpy, portRadius, rp)
            if (node is DialogueNode.ConditionNode) canvas.drawText(if (i == 0) "T" else "F", qpx - 8f, qpy + 24f, tinyTextPaint)
            else if (node is DialogueNode.ChoiceNode && i < node.choices.size) canvas.drawText(node.choices[i].text.take(3), qpx - 8f, qpy + 24f, tinyTextPaint)
        }
    }

    private fun drawMinimap(canvas: Canvas) {
        if (dialogueTree.nodes.isEmpty()) return
        val mmW = 180f; val mmH = 120f; val pad = 12f
        val mmX = width - mmW - pad; val mmY = height - mmH - pad - 60f
        miniMapRect = RectF(mmX, mmY, mmX + mmW, mmY + mmH)
        canvas.drawRoundRect(mmX - 2f, mmY - 2f, mmX + mmW + 2f, mmY + mmH + 2f, 8f, 8f, minimapBgPaint)
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        for (n in dialogueTree.nodes) {
            if (n.x < minX) minX = n.x; if (n.y < minY) minY = n.y
            if (n.x + nodeWidth > maxX) maxX = n.x + nodeWidth; if (n.y + nodeHeight > maxY) maxY = n.y + nodeHeight
        }
        if (maxX <= minX) { maxX = minX + 100f; maxY = minY + 100f }
        val pad2 = 20f; val rX = (maxX - minX) + pad2 * 2; val rY = (maxY - minY) + pad2 * 2
        val mmS = minOf(mmW / rX, mmH / rY)
        val dOX = mmX + (mmW - rX * mmS) / 2f; val dOY = mmY + (mmH - rY * mmS) / 2f
        canvas.save(); canvas.clipRect(mmX - 2f, mmY - 2f, mmX + mmW + 2f, mmY + mmH + 2f)
        for (node in dialogueTree.nodes) {
            minimapNodePaint.color = minimapColors[node.typeName()] ?: Color.GRAY; minimapNodePaint.style = Paint.Style.FILL
            val nx = dOX + (node.x - minX + pad2) * mmS; val ny = dOY + (node.y - minY + pad2) * mmS
            canvas.drawRect(nx, ny, nx + maxOf(2f, nodeWidth * mmS * 0.7f), ny + maxOf(2f, nodeHeight * mmS * 0.7f), minimapNodePaint)
        }
        canvas.drawRect(
            dOX + (-offsetX - minX + pad2) * mmS, dOY + (-offsetY - minY + pad2) * mmS,
            dOX + (-offsetX - minX + pad2 + width / scale) * mmS, dOY + (-offsetY - minY + pad2 + height / scale) * mmS,
            minimapViewportPaint
        )
        canvas.restore()
    }

    private fun jumpToMinimapPosition(tx: Float, ty: Float) {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        for (n in dialogueTree.nodes) {
            if (n.x < minX) minX = n.x; if (n.y < minY) minY = n.y
            if (n.x + nodeWidth > maxX) maxX = n.x + nodeWidth; if (n.y + nodeHeight > maxY) maxY = n.y + nodeHeight
        }
        if (maxX <= minX) return
        val pad2 = 20f; val rX = (maxX - minX) + pad2 * 2; val rY = (maxY - minY) + pad2 * 2
        val mmS = minOf(180f / rX, 120f / rY)
        val dOX = miniMapRect.left + (180f - rX * mmS) / 2f; val dOY = miniMapRect.top + (120f - rY * mmS) / 2f
        offsetX = -((tx - dOX) / mmS + minX - pad2) + (width / 2f / scale)
        offsetY = -((ty - dOY) / mmS + minY - pad2) + (height / 2f / scale)
        recalcVisible(); invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) gestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (miniMapRect.contains(event.x, event.y)) return true
                val wx = (event.x / scale - offsetX); val wy = (event.y / scale - offsetY)
                findNodeAt(wx, wy)?.let {
                    draggedNodeId = it.id; nodeStartX = it.x; nodeStartY = it.y
                    dragStartX = event.x; dragStartY = event.y; isDragging = false; selectedNodeId = it.id
                } ?: run { draggedNodeId = null }; invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging && draggedNodeId != null) editorActivity.pushUndo()
                draggedNodeId = null; isDragging = false; performClick()
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggedNodeId != null) {
                    val dx = (event.x - dragStartX) / scale; val dy = (event.y - dragStartY) / scale
                    updateNodePosition(draggedNodeId!!, nodeStartX + dx, nodeStartY + dy)
                    if (Math.abs(event.x - dragStartX) > 5 || Math.abs(event.y - dragStartY) > 5) isDragging = true
                }
            }
        }; return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }
}
