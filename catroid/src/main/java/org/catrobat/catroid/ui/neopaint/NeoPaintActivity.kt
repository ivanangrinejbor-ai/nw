package org.catrobat.catroid.ui.neopaint

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants.EXTRA_PICTURE_PATH_POCKET_PAINT
import java.io.File

class NeoPaintActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var colorSwatch: Button
    private lateinit var layersPanel: LinearLayout
    private lateinit var layerList: LinearLayout
    private lateinit var seekLayerOpacity: SeekBar

    private var picturePath: String? = null
    private var currentColor = Color.BLACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_neopaint)

        drawingView = findViewById(R.id.drawing_view)
        colorSwatch = findViewById(R.id.btn_color_swatch)
        layersPanel = findViewById(R.id.layout_layers)
        layerList = findViewById(R.id.list_layers)
        seekLayerOpacity = findViewById(R.id.seek_layer_opacity)

        picturePath = intent.getStringExtra(EXTRA_PICTURE_PATH_POCKET_PAINT)

        val bitmap = if (picturePath != null) {
            BitmapFactory.decodeFile(picturePath) ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        drawingView.initializeWithBitmap(bitmap)

        setupToolbars()
        setupPropertyBar()
        setupLayerPanel()
        refreshLayers()
    }

    private fun setupToolbars() {
        val tools = mapOf(
            R.id.tool_brush to ToolType.BRUSH,
            R.id.tool_eraser to ToolType.ERASER,
            R.id.tool_fill to ToolType.FILL,
            R.id.tool_line to ToolType.LINE,
            R.id.tool_rect to ToolType.RECTANGLE,
            R.id.tool_oval to ToolType.OVAL,
            R.id.tool_text to ToolType.TEXT,
            R.id.tool_picker to ToolType.EYEDROPPER,
            R.id.tool_smudge to ToolType.SMUDGE
        )
        for ((id, type) in tools) {
            findViewById<Button>(id).setOnClickListener { drawingView.setTool(type) }
        }

        findViewById<Button>(R.id.btn_undo).setOnClickListener { drawingView.undo() }
        findViewById<Button>(R.id.btn_redo).setOnClickListener { drawingView.redo() }
        findViewById<Button>(R.id.btn_clear).setOnClickListener { drawingView.clearCurrentLayer() }

        findViewById<Button>(R.id.btn_layers).setOnClickListener {
            layersPanel.visibility = if (layersPanel.visibility == LinearLayout.VISIBLE) LinearLayout.GONE else LinearLayout.VISIBLE
        }
        findViewById<Button>(R.id.btn_flip_h).setOnClickListener { drawingView.flipHorizontal() }
        findViewById<Button>(R.id.btn_flip_v).setOnClickListener { drawingView.flipVertical() }
        findViewById<Button>(R.id.btn_rotate).setOnClickListener { drawingView.rotate90Cw() }

        drawingView.onColorPickedListener = { color ->
            currentColor = color
            colorSwatch.setBackgroundColor(color)
            drawingView.setColor(color)
        }
        drawingView.onRequestTextListener = { x, y -> showTextDialog(x, y) }
        drawingView.onChangeListener = { refreshLayers() }
    }

    private fun setupPropertyBar() {
        colorSwatch.setBackgroundColor(currentColor)
        colorSwatch.setOnClickListener {
            ColorPickerDialog(this, currentColor) { color ->
                currentColor = color
                colorSwatch.setBackgroundColor(color)
                drawingView.setColor(color)
            }.show()
        }

        findViewById<SeekBar>(R.id.seek_brush_size).setOnSeekBarChangeListener(
            simpleSeek { progress, _ -> drawingView.setStrokeWidth((progress + 1).toFloat()) })
        findViewById<SeekBar>(R.id.seek_opacity).setOnSeekBarChangeListener(
            simpleSeek { progress, _ -> drawingView.setOpacity(progress / 100f) })
    }

    private fun setupLayerPanel() {
        findViewById<Button>(R.id.btn_new_layer).setOnClickListener {
            drawingView.addLayer()
            refreshLayers()
        }
        findViewById<Button>(R.id.btn_dup_layer).setOnClickListener {
            drawingView.duplicateLayer(drawingView.currentIndex())
            refreshLayers()
        }
        findViewById<Button>(R.id.btn_del_layer).setOnClickListener {
            drawingView.removeLayer(drawingView.currentIndex())
            refreshLayers()
        }
        seekLayerOpacity.setOnSeekBarChangeListener(
            simpleSeek { progress, _ -> drawingView.setLayerOpacity(drawingView.currentIndex(), progress / 100f) })
    }

    private fun refreshLayers() {
        layerList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val count = drawingView.layerCount()
        for (i in 0 until count) {
            val layer = drawingView.getLayer(i) ?: continue
            val row = inflater.inflate(R.layout.item_layer, layerList, false)
            val nameView = row.findViewById<TextView>(R.id.layer_name)
            nameView.text = "Layer ${i + 1}"
            val visibilityToggle = row.findViewById<Button>(R.id.layer_visibility_toggle)
            visibilityToggle.text = if (layer.visible) "Show" else "Hide"
            visibilityToggle.setOnClickListener {
                drawingView.toggleVisibility(i)
                refreshLayers()
            }
            row.findViewById<Button>(R.id.layer_delete).setOnClickListener {
                drawingView.removeLayer(i)
                refreshLayers()
            }
            row.setOnClickListener { drawingView.selectLayer(i); refreshLayers() }
            if (i == drawingView.currentIndex()) {
                row.setBackgroundColor(0xFF555555.toInt())
            }
            layerList.addView(row)
        }
        if (count > 0) {
            seekLayerOpacity.progress = (drawingView.getLayer(drawingView.currentIndex())?.opacity?.times(100f))?.toInt() ?: 100
        }
    }

    private fun showTextDialog(x: Float, y: Float) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_neopaint_text, null)
        val input = dialogView.findViewById<TextView>(R.id.edit_text_input)
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create().apply {
                dialogView.findViewById<Button>(R.id.btn_text_ok).setOnClickListener {
                    drawingView.drawTextOnCurrentLayer(input.text.toString(), x, y)
                    dismiss()
                }
                dialogView.findViewById<Button>(R.id.btn_text_cancel).setOnClickListener { dismiss() }
                show()
            }
    }

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
            val file = File(path)
            val out = java.io.FileOutputStream(file)
            drawingView.getCompositeBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
            out.close()
            val result = Intent()
            result.putExtra(EXTRA_PICTURE_PATH_POCKET_PAINT, path)
            setResult(Activity.RESULT_OK, result)
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun onBackPressed() {
        saveAndReturn()
    }
}
