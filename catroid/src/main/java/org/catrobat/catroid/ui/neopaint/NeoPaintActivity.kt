package org.catrobat.catroid.ui.neopaint

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants.EXTRA_PICTURE_PATH_POCKET_PAINT
import java.io.File

class NeoPaintActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var colorSwatch: Button
    private lateinit var txtShapeFill: TextView

    private var picturePath: String? = null
    private var currentColor = Color.BLACK
    private var currentTool = ToolType.BRUSH
    private var currentBrushSize = 8f
    private var currentOpacityValue = 1f
    private var shapeFillAmount = 100 // 0 = outline, 100 = solid fill

    // Clipboard: stores a bitmap copy of a layer
    private var clipboardBitmap: Bitmap? = null

    // More-tools dialog hold reference to dismiss it after tool selection
    private var moreToolsDialog: AlertDialog? = null

    // Transform controls (shown when overlay active)
    private lateinit var transformGroup: List<View>
    private lateinit var normalControls: List<View>
    private lateinit var seekRotation: SeekBar
    private lateinit var lblRotation: TextView
    private lateinit var btnFlipX: ImageButton
    private lateinit var btnFlipY: ImageButton
    private lateinit var btnOverlayConfirm: ImageButton
    private lateinit var btnOverlayCancel: ImageButton
    private var currentRotation = 0f
    private var currentFlipX = false
    private var currentFlipY = false
    private lateinit var brushButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_neopaint)

        drawingView = findViewById(R.id.drawing_view)
        colorSwatch = findViewById(R.id.btn_color_swatch)
        txtShapeFill = findViewById(R.id.lbl_shape_fill)

        picturePath = intent.getStringExtra(EXTRA_PICTURE_PATH_POCKET_PAINT)

        var bitmap: Bitmap? = null

        if (savedInstanceState != null) {
            val tempPath = savedInstanceState.getString("drawingTempPath")
            if (tempPath != null) {
                bitmap = BitmapFactory.decodeFile(tempPath)
            }
        }

        if (bitmap == null) {
            bitmap = if (picturePath != null) {
                BitmapFactory.decodeFile(picturePath) ?: Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            } else {
                Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            }
        }
        drawingView.initializeWithBitmap(bitmap)

        if (savedInstanceState != null) {
            currentTool = ToolType.values()[savedInstanceState.getInt("currentToolOrdinal", 0)]
            currentBrushSize = savedInstanceState.getFloat("currentBrushSize", 8f)
            currentOpacityValue = savedInstanceState.getFloat("currentOpacityValue", 1f)
            shapeFillAmount = savedInstanceState.getInt("shapeFillAmount", 100)
            drawingView.setTool(currentTool)
            drawingView.setStrokeWidth(currentBrushSize)
            drawingView.setOpacity(currentOpacityValue)
            drawingView.setShapeFill(shapeFillAmount)
            updateShapeFillLabel()
        }

        setupToolbars()
        setupPropertyBar()
    }

    private fun setupToolbars() {
        // Brush — always on main palette
        brushButton = findViewById(R.id.tool_brush)
        brushButton.setOnClickListener {
            selectTool(ToolType.BRUSH)
        }

        // More tools button
        findViewById<TextView>(R.id.btn_more_tools).setOnClickListener {
            showMoreToolsDialog()
        }

        // Action bar buttons
        findViewById<ImageButton>(R.id.btn_undo).setOnClickListener { drawingView.undo() }
        findViewById<ImageButton>(R.id.btn_redo).setOnClickListener { drawingView.redo() }
        findViewById<ImageButton>(R.id.btn_clear).setOnClickListener { drawingView.clearCurrentLayer() }

        findViewById<ImageButton>(R.id.btn_flip_h).setOnClickListener { drawingView.flipHorizontal() }
        findViewById<ImageButton>(R.id.btn_flip_v).setOnClickListener { drawingView.flipVertical() }
        findViewById<ImageButton>(R.id.btn_rotate).setOnClickListener { drawingView.rotate90Cw() }

        findViewById<Button>(R.id.btn_save).setOnClickListener { saveAndReturn() }

        drawingView.onColorPickedListener = { color ->
            currentColor = color
            colorSwatch.setBackgroundColor(color)
            drawingView.setColor(color)
        }
        drawingView.onRequestTextListener = { x, y -> showTextDialog(x, y) }

        // Transform controls
        seekRotation = findViewById(R.id.seek_rotation)
        lblRotation = findViewById(R.id.lbl_rotation)
        btnFlipX = findViewById(R.id.btn_flip_x)
        btnFlipY = findViewById(R.id.btn_flip_y)
        btnOverlayConfirm = findViewById(R.id.btn_overlay_confirm)
        btnOverlayCancel = findViewById(R.id.btn_overlay_cancel)

        transformGroup = listOf(lblRotation, seekRotation, btnFlipX, btnFlipY, btnOverlayConfirm, btnOverlayCancel)
        normalControls = listOf(
            findViewById(R.id.lbl_brush_size), findViewById(R.id.seek_brush_size),
            findViewById(R.id.lbl_opacity), findViewById(R.id.seek_opacity)
        )

        seekRotation.setOnSeekBarChangeListener(simpleSeek { progress, _ ->
            currentRotation = progress.toFloat()
            lblRotation.text = "${progress}°"
            drawingView.setOverlayRotation(currentRotation)
        })

        btnFlipX.setOnClickListener {
            currentFlipX = !currentFlipX
            btnFlipX.isSelected = currentFlipX
            drawingView.setOverlayFlipX(currentFlipX)
        }
        btnFlipY.setOnClickListener {
            currentFlipY = !currentFlipY
            btnFlipY.isSelected = currentFlipY
            drawingView.setOverlayFlipY(currentFlipY)
        }

        btnOverlayConfirm.setOnClickListener {
            drawingView.commitOverlay()
            hideTransformControls()
        }
        btnOverlayCancel.setOnClickListener {
            drawingView.cancelOverlay()
            hideTransformControls()
        }

        drawingView.onShowConfirmButtons = { show ->
            runOnUiThread {
                if (show) showTransformControls() else hideTransformControls()
            }
        }

        updateToolSelection(currentTool)
    }

    private fun selectTool(tool: ToolType) {
        currentTool = tool
        drawingView.setTool(tool)
        updateToolSelection(tool)
        updatePropertyBarForTool(tool)
        moreToolsDialog?.dismiss()
    }

    private fun updateToolSelection(active: ToolType) {
        val brushBtn = findViewById<ImageButton>(R.id.tool_brush)
        brushBtn.isSelected = (active == ToolType.BRUSH)
        // Remaining tools are in the dialog — no selection highlight needed there
    }

    private fun updatePropertyBarForTool(tool: ToolType) {
        // Show/hide shape fill slider based on tool
        txtShapeFill.visibility = if (tool in SHAPE_TOOLS) View.VISIBLE else View.GONE
    }

    private fun setupPropertyBar() {
        colorSwatch.setBackgroundColor(currentColor)
        updateBrushTint()
        colorSwatch.setOnClickListener {
            ColorPickerDialog(this, currentColor) { color ->
                currentColor = color
                colorSwatch.setBackgroundColor(color)
                drawingView.setColor(color)
                updateBrushTint()
            }.show()
        }

        val lblBrushSize = findViewById<TextView>(R.id.lbl_brush_size)
        val lblOpacity = findViewById<TextView>(R.id.lbl_opacity)
        val brushSeek = findViewById<SeekBar>(R.id.seek_brush_size)
        brushSeek.progress = (currentBrushSize - 1).toInt()
        brushSeek.setOnSeekBarChangeListener(
            simpleSeek { progress, _ ->
                currentBrushSize = (progress + 1).toFloat()
                drawingView.setStrokeWidth(currentBrushSize)
                lblBrushSize.text = "${progress + 1}"
            })
        val opacitySeek = findViewById<SeekBar>(R.id.seek_opacity)
        opacitySeek.progress = (currentOpacityValue * 100).toInt()
        opacitySeek.setOnSeekBarChangeListener(
            simpleSeek { progress, _ ->
                currentOpacityValue = progress / 100f
                drawingView.setOpacity(currentOpacityValue)
                lblOpacity.text = "$progress%"
            })

        // Shape fill slider (initially hidden)
        txtShapeFill.visibility = View.GONE
        val seekFill = findViewById<SeekBar>(R.id.seek_shape_fill)
        seekFill.progress = shapeFillAmount
        seekFill.setOnSeekBarChangeListener(
            simpleSeek { progress, _ ->
                shapeFillAmount = progress
                drawingView.setShapeFill(progress)
                updateShapeFillLabel()
            })
    }

    private fun updateShapeFillLabel() {
        txtShapeFill.text = if (shapeFillAmount >= 100) "Fill" else if (shapeFillAmount <= 0) "Line" else "Fill $shapeFillAmount%"
    }

    private fun showTransformControls() {
        currentRotation = drawingView.getOverlayRotation()
        currentFlipX = drawingView.getOverlayFlipX()
        currentFlipY = drawingView.getOverlayFlipY()
        seekRotation.progress = currentRotation.roundToInt()
        lblRotation.text = "${currentRotation.roundToInt()}°"
        btnFlipX.isSelected = currentFlipX
        btnFlipY.isSelected = currentFlipY
        for (v in transformGroup) v.visibility = View.VISIBLE
        for (v in normalControls) v.visibility = View.GONE
        txtShapeFill.visibility = View.GONE
        findViewById<SeekBar>(R.id.seek_shape_fill).visibility = View.GONE
    }

    private fun hideTransformControls() {
        for (v in transformGroup) v.visibility = View.GONE
        for (v in normalControls) v.visibility = View.VISIBLE
        // Shape fill visibility is managed by updatePropertyBarForTool
        updatePropertyBarForTool(currentTool)
    }

    private fun updateBrushTint() {
        brushButton.drawable?.setTint(currentColor)
    }

    // ── More Tools Dialog ──────────────────────────────────────

    private fun showMoreToolsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_neopaint_more_tools, null)

        // Drawing tools
        view.findViewById<ImageButton>(R.id.dlg_tool_eraser).setOnClickListener { selectTool(ToolType.ERASER) }
        view.findViewById<ImageButton>(R.id.dlg_tool_fill).setOnClickListener { selectTool(ToolType.FILL) }
        view.findViewById<ImageButton>(R.id.dlg_tool_line).setOnClickListener { selectTool(ToolType.LINE) }
        view.findViewById<ImageButton>(R.id.dlg_tool_smudge).setOnClickListener { selectTool(ToolType.SMUDGE) }
        view.findViewById<ImageButton>(R.id.dlg_tool_spray).setOnClickListener { selectTool(ToolType.SPRAY_CAN) }

        // Shapes
        view.findViewById<ImageButton>(R.id.dlg_tool_rect).setOnClickListener { selectTool(ToolType.RECTANGLE) }
        view.findViewById<ImageButton>(R.id.dlg_tool_oval).setOnClickListener { selectTool(ToolType.OVAL) }
        view.findViewById<ImageButton>(R.id.dlg_tool_star).setOnClickListener { selectTool(ToolType.STAR) }
        view.findViewById<ImageButton>(R.id.dlg_tool_heart).setOnClickListener { selectTool(ToolType.HEART) }

        // Tools
        view.findViewById<ImageButton>(R.id.dlg_tool_text).setOnClickListener { selectTool(ToolType.TEXT) }
        view.findViewById<ImageButton>(R.id.dlg_tool_picker).setOnClickListener { selectTool(ToolType.EYEDROPPER) }
        view.findViewById<ImageButton>(R.id.dlg_tool_zoom).setOnClickListener {
            moreToolsDialog?.dismiss()
            showResolutionDialog()
        }
        view.findViewById<ImageButton>(R.id.dlg_tool_clipboard).setOnClickListener {
            moreToolsDialog?.dismiss()
            showClipboardMenu()
        }

        moreToolsDialog = AlertDialog.Builder(this)
            .setTitle("Tools")
            .setView(view)
            .setNegativeButton("Close", null)
            .create()
            .also { it.show() }
    }

    // ── Resolution Dialog ──────────────────────────────────────

    private fun showResolutionDialog() {
        val w = drawingView.bitmapWidth
        val h = drawingView.bitmapHeight
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_neopaint_resize, null) ?: run {
            // Fallback if layout not yet created — use inline
            val ll = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16) }
            val lbl = TextView(this).apply { text = "Current: ${w}x${h}" }
            ll.addView(lbl)
            AlertDialog.Builder(this)
                .setTitle("Canvas Size")
                .setView(ll)
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val lblInfo = view.findViewById<TextView>(R.id.resize_lbl_info)
        lblInfo?.text = "Current: ${w}x${h}"

        val seekW = view.findViewById<SeekBar>(R.id.seek_resize_w)
        val seekH = view.findViewById<SeekBar>(R.id.seek_resize_h)
        val txtW = view.findViewById<TextView>(R.id.resize_txt_w)
        val txtH = view.findViewById<TextView>(R.id.resize_txt_h)

        if (seekW != null) {
            seekW.max = 4096
            seekW.progress = w.coerceIn(64, 4096)
        }
        if (seekH != null) {
            seekH.max = 4096
            seekH.progress = h.coerceIn(64, 4096)
        }

        var newW = w
        var newH = h

        if (seekW != null && txtW != null) {
            txtW.text = "$newW"
            seekW.setOnSeekBarChangeListener(simpleSeek { p, _ -> newW = p.coerceAtLeast(64); txtW.text = "$newW" })
        }
        if (seekH != null && txtH != null) {
            txtH.text = "$newH"
            seekH.setOnSeekBarChangeListener(simpleSeek { p, _ -> newH = p.coerceAtLeast(64); txtH.text = "$newH" })
        }

        val editW = view.findViewById<EditText>(R.id.resize_edit_w)
        val editH = view.findViewById<EditText>(R.id.resize_edit_h)
        editW?.setText("$w")
        editH?.setText("$h")

        AlertDialog.Builder(this)
            .setTitle("Canvas Size")
            .setView(view)
            .setPositiveButton("Resize") { _, _ ->
                val finalW = editW?.text?.toString()?.toIntOrNull()?.coerceIn(64, 4096) ?: newW
                val finalH = editH?.text?.toString()?.toIntOrNull()?.coerceIn(64, 4096) ?: newH
                drawingView.resizeCanvas(finalW, finalH)
                selectTool(ToolType.BRUSH)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Clipboard Menu ─────────────────────────────────────────

    private fun showClipboardMenu() {
        val items = arrayOf("Copy layer", "Paste layer", "Clear clipboard")
        AlertDialog.Builder(this)
            .setTitle("Clipboard")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> { // Copy
                        clipboardBitmap?.recycle()
                        clipboardBitmap = drawingView.copyCurrentLayerBitmap()
                    }
                    1 -> { // Paste
                        val bmp = clipboardBitmap
                        if (bmp != null) {
                            drawingView.pasteBitmapAsNewLayer(bmp)
                        }
                    }
                    2 -> { // Clear
                        clipboardBitmap?.recycle()
                        clipboardBitmap = null
                    }
                }
            }
            .show()
    }

    // ── Text Dialog ────────────────────────────────────────────

    private fun showTextDialog(x: Float, y: Float) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_neopaint_text, null)
        val input = dialogView.findViewById<EditText>(R.id.edit_text_input)
        val chkOutline = dialogView.findViewById<android.widget.CheckBox>(R.id.chk_outline)
        val btnOutlineColor = dialogView.findViewById<Button>(R.id.btn_outline_color)
        val chkGlow = dialogView.findViewById<android.widget.CheckBox>(R.id.chk_glow)
        val btnGlowColor = dialogView.findViewById<Button>(R.id.btn_glow_color)
        val glowRadiusRow = dialogView.findViewById<View>(R.id.glow_radius_row)
        val seekGlow = dialogView.findViewById<SeekBar>(R.id.seek_glow_radius)
        val chkGradient = dialogView.findViewById<android.widget.CheckBox>(R.id.chk_gradient)
        val btnGradStart = dialogView.findViewById<Button>(R.id.btn_grad_start)
        val btnGradEnd = dialogView.findViewById<Button>(R.id.btn_grad_end)
        val lblGradTo = dialogView.findViewById<TextView>(R.id.lbl_grad_to)

        var outlineColor = Color.BLACK
        var glowColor = Color.parseColor("#80FFFFFF")
        var gradStart = Color.RED
        var gradEnd = Color.BLUE

        // Toggle visibility
        chkGlow.setOnCheckedChangeListener { _, checked ->
            glowRadiusRow.visibility = if (checked) View.VISIBLE else View.GONE
        }

        chkGradient.setOnCheckedChangeListener { _, checked ->
            val v = if (checked) View.VISIBLE else View.GONE
            btnGradStart.visibility = v; lblGradTo.visibility = v; btnGradEnd.visibility = v
        }

        // Color pickers
        btnOutlineColor.setOnClickListener {
            ColorPickerDialog(this, outlineColor) { c -> outlineColor = c; btnOutlineColor.setBackgroundColor(c) }.show()
        }
        btnGlowColor.setOnClickListener {
            ColorPickerDialog(this, glowColor) { c -> glowColor = c; btnGlowColor.setBackgroundColor(c) }.show()
        }
        btnGradStart.setOnClickListener {
            ColorPickerDialog(this, gradStart) { c -> gradStart = c; btnGradStart.setBackgroundColor(c) }.show()
        }
        btnGradEnd.setOnClickListener {
            ColorPickerDialog(this, gradEnd) { c -> gradEnd = c; btnGradEnd.setBackgroundColor(c) }.show()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.setCancelable(true)

        dialogView.findViewById<Button>(R.id.btn_text_ok).setOnClickListener {
            drawingView.setTextContent(input.text.toString())
            if (chkOutline.isChecked) {
                drawingView.setTextOutline(3f, outlineColor)
            } else {
                drawingView.setTextOutline(0f, Color.BLACK)
            }
            if (chkGlow.isChecked) {
                drawingView.setTextGlow(seekGlow.progress.toFloat() + 1f, glowColor)
            } else {
                drawingView.setTextGlow(0f, glowColor)
            }
            if (chkGradient.isChecked) {
                drawingView.setTextGradient(gradStart, gradEnd)
            } else {
                drawingView.setTextGradient(Color.TRANSPARENT, Color.TRANSPARENT)
            }
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btn_text_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun simpleSeek(onChange: (Int, Boolean) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) = onChange(progress, fromUser)
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
    }

    private fun saveAndReturn() {
        val path = picturePath
        if (path != null) {
            saveBitmapToFile(File(path))
            val result = Intent()
            result.putExtra(EXTRA_PICTURE_PATH_POCKET_PAINT, path)
            setResult(Activity.RESULT_OK, result)
        } else {
            val tempFile = File(cacheDir, "neopaint_new_${System.currentTimeMillis()}.png")
            saveBitmapToFile(tempFile)
            val result = Intent()
            result.putExtra(EXTRA_PICTURE_PATH_POCKET_PAINT, tempFile.absolutePath)
            setResult(Activity.RESULT_OK, result)
        }
        finish()
    }

    private fun saveBitmapToFile(file: File) {
        java.io.FileOutputStream(file).use { out ->
            drawingView.getCompositeBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("picturePath", picturePath)
        outState.putInt("currentColor", currentColor)
        outState.putInt("currentToolOrdinal", currentTool.ordinal)
        outState.putFloat("currentBrushSize", currentBrushSize)
        outState.putFloat("currentOpacityValue", currentOpacityValue)
        outState.putInt("shapeFillAmount", shapeFillAmount)
        try {
            val tempFile = File(cacheDir, "neopaint_restore_temp.png")
            saveBitmapToFile(tempFile)
            outState.putString("drawingTempPath", tempFile.absolutePath)
        } catch (_: Exception) {}
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        picturePath = savedInstanceState.getString("picturePath")
        currentColor = savedInstanceState.getInt("currentColor", Color.BLACK)
        colorSwatch.setBackgroundColor(currentColor)
        currentTool = ToolType.values()[savedInstanceState.getInt("currentToolOrdinal", 0)]
        currentBrushSize = savedInstanceState.getFloat("currentBrushSize", 8f)
        currentOpacityValue = savedInstanceState.getFloat("currentOpacityValue", 1f)
        shapeFillAmount = savedInstanceState.getInt("shapeFillAmount", 100)
        drawingView.setShapeFill(shapeFillAmount)
        updateShapeFillLabel()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(R.string.neopaint_save_changes_title)
            .setMessage(R.string.neopaint_save_changes_message)
            .setPositiveButton(R.string.neopaint_save_exit) { _: DialogInterface, _: Int -> saveAndReturn() }
            .setNeutralButton(R.string.neopaint_discard) { _: DialogInterface, _: Int ->
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        private val SHAPE_TOOLS = setOf(
            ToolType.RECTANGLE, ToolType.OVAL, ToolType.STAR, ToolType.HEART
        )
    }
}
