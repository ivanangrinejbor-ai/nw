/*
 * Paintroid: An image manipulation application for Android.
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.paintroid.ui.tools

import android.content.Context
import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.Checkable
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.paintroid.dialog.ProjectPickerDialog
import org.catrobat.catroid.paintroid.colorpicker.ColorPickerDialog
import org.catrobat.catroid.paintroid.colorpicker.OnColorPickedListener
import org.catrobat.catroid.paintroid.tools.FontEntry
import org.catrobat.catroid.paintroid.tools.FontType
import org.catrobat.catroid.paintroid.tools.ImportedFontRegistry
import org.catrobat.catroid.paintroid.tools.TextToolEffects
import org.catrobat.catroid.paintroid.tools.options.TextToolOptionsView

private const val DEFAULT_TEXTSIZE = "20"
private const val MAX_TEXTSIZE = "300"
private const val MIN_FONT_SIZE = 1
private const val MAX_FONT_SIZE = 300

class DefaultTextToolOptionsView(rootView: ViewGroup) : TextToolOptionsView {
    private val context: Context = rootView.context
    private var callback: TextToolOptionsView.Callback? = null
    private val textEditText: EditText
    private val fontSizeText: EditText
    private val fontList: RecyclerView
    private val addFontButton: View
    private val addFontDeviceButton: View
    private val fxButton: com.google.android.material.button.MaterialButton
    private var fontDevicePicker: androidx.activity.result.ActivityResultLauncher<String>? = null
    private val underlinedToggleButton: MaterialButton
    private val italicToggleButton: MaterialButton
    private val boldToggleButton: MaterialButton
    private var fontListAdapter: FontListAdapter? = null
    private val topLayout: View
    private val bottomLayout: View
    private val textToolOptionsViewShapeSizeChip: Chip
    private val changeSizeShapeSizeChip: Chip

    init {
        val inflater = LayoutInflater.from(context)
        val textToolView = inflater.inflate(R.layout.dialog_pocketpaint_text_tool, rootView)
        topLayout = textToolView.findViewById(R.id.pocketpaint_text_top_layout)
        bottomLayout = textToolView.findViewById(R.id.pocketpaint_text_bottom_layout)
        textEditText = textToolView.findViewById(R.id.pocketpaint_text_tool_dialog_input_text)
        fontList = textToolView.findViewById(R.id.pocketpaint_text_tool_dialog_list_font)
        addFontButton = textToolView.findViewById(R.id.addFontButton)
        addFontDeviceButton = textToolView.findViewById(R.id.addFontDeviceButton)
        fxButton = textToolView.findViewById(R.id.pocketpaint_text_fx_button)
        underlinedToggleButton =
            textToolView.findViewById(R.id.pocketpaint_text_tool_dialog_toggle_underlined)
        italicToggleButton =
            textToolView.findViewById(R.id.pocketpaint_text_tool_dialog_toggle_italic)
        boldToggleButton = textToolView.findViewById(R.id.pocketpaint_text_tool_dialog_toggle_bold)
        fontSizeText = textToolView.findViewById(R.id.pocketpaint_font_size_text)
        fontSizeText.setText(DEFAULT_TEXTSIZE)
        underlinedToggleButton.paintFlags =
            underlinedToggleButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        initializeListeners()
        textEditText.requestFocus()
        val viewShapeSizeLayout =
            textToolView.findViewById<LinearLayout>(R.id.pocketpaint_layout_text_tool_options_view_shape_size)
        textToolOptionsViewShapeSizeChip = viewShapeSizeLayout.findViewById(R.id.pocketpaint_fill_shape_size_text)
        val changeShapeSizeLayout =
            textToolView.findViewById<LinearLayout>(R.id.pocketpaint_layout_text_tool_change_size_shape_size)
        changeSizeShapeSizeChip = changeShapeSizeLayout.findViewById(R.id.pocketpaint_fill_shape_size_text)
        toggleShapeSizeVisibility(false)
    }

    private fun initializeListeners() {
        textEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(editable: Editable) {
                notifyTextChanged(editable.toString())
            }
        })
        textEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideKeyboard()
            }
        }

        val entries = buildEntries()
        fontListAdapter = FontListAdapter(
            context,
            entries,
            onFontClicked = { entry ->
                notifyFontChanged(entry)
                hideKeyboard()
            },
            onFontLongClicked = { entry -> confirmRemoveImportedFont(entry) }
        )
        fontList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        fontList.adapter = fontListAdapter

        addFontButton.setOnClickListener {
            val project = ProjectManager.getInstance().getCurrentProject()
            val filesDir = project?.getFilesDir()
            if (project != null && filesDir != null && filesDir.isDirectory) {
                val imported = ImportedFontRegistry.importFromProject(context, project.directory)
                if (imported.isNotEmpty()) {
                    rebuildFontList()
                    return@setOnClickListener
                }
            }
            val fm = (context as? FragmentActivity)?.supportFragmentManager ?: return@setOnClickListener
            ProjectPickerDialog.newInstance { rebuildFontList() }.show(fm, "ProjectPickerDialog")
        }

        fxButton.setOnClickListener { showEffectsDialog() }

        fontDevicePicker = (context as? androidx.activity.ComponentActivity)
            ?.registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) {
                    return@registerForActivityResult
                }
                val imported = ImportedFontRegistry.importFromFile(context, uri)
                if (imported == null) {
                    android.widget.Toast.makeText(
                        context,
                        R.string.add_font_invalid,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@registerForActivityResult
                }
                try {
                    val project = ProjectManager.getInstance().getCurrentProject()
                    if (project != null) {
                        ImportedFontRegistry.copyToProject(context, imported, project.filesDir)
                    }
                } catch (_: Exception) {
                }
                rebuildFontList()
                selectFontEntry(FontEntry.Imported(imported))
            }
        addFontDeviceButton.setOnClickListener {
            hideKeyboard()
            fontDevicePicker?.launch("*/*")
        }

        underlinedToggleButton.setOnClickListener { v ->
            val underlined = (v as Checkable).isChecked
            notifyUnderlinedChanged(underlined)
            hideKeyboard()
        }
        italicToggleButton.setOnClickListener { v ->
            val italic = (v as Checkable).isChecked
            notifyItalicChanged(italic)
            hideKeyboard()
        }
        boldToggleButton.setOnClickListener { v ->
            val bold = (v as Checkable).isChecked
            notifyBoldChanged(bold)
            hideKeyboard()
        }
        fontSizeText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) = Unit

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) = Unit

            override fun afterTextChanged(editable: Editable) {
                val sizeText = fontSizeText.text.toString()
                var sizeTextInt: Int
                sizeTextInt = try {
                    sizeText.toInt()
                } catch (exp: NumberFormatException) {
                    MIN_FONT_SIZE
                }
                if (sizeTextInt > MAX_FONT_SIZE) {
                    sizeTextInt = MAX_FONT_SIZE
                    fontSizeText.setText(MAX_TEXTSIZE)
                    fontSizeText.setSelection(MAX_TEXTSIZE.length)
                }
                notifyTextSizeChanged(sizeTextInt)
            }
        })
    }

    private fun buildEntries(): List<FontEntry> =
        FontType.values().filter { it != FontType.PROJECT_FONT }.map { FontEntry.BuiltIn(it) } +
            ImportedFontRegistry.getAll(context).map { FontEntry.Imported(it) }

    private fun rebuildFontList() {
        fontListAdapter?.updateEntries(buildEntries())
    }

    private fun selectFontEntry(entry: FontEntry) {
        val entries = fontListAdapter?.fontEntries ?: return
        val index = entries.indexOfFirst { matches(it, entry) }
        if (index >= 0) {
            fontListAdapter?.setSelectedFontIndex(index)
            notifyFontChanged(entries[index])
        }
    }

    private fun confirmRemoveImportedFont(entry: FontEntry) {
        if (entry !is FontEntry.Imported) {
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.remove_font_title))
            .setMessage(context.getString(R.string.remove_font_message, entry.font.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ImportedFontRegistry.remove(context, entry.font.name)
                rebuildFontList()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun notifyFontChanged(entry: FontEntry) {
        when (entry) {
            is FontEntry.BuiltIn -> callback?.setFont(entry, null)
            is FontEntry.Imported -> callback?.setFont(entry, ImportedFontRegistry.getTypeface(context, entry.font.name))
        }
    }

    private fun notifyUnderlinedChanged(underlined: Boolean) {
        callback?.setUnderlined(underlined)
    }

    private fun notifyItalicChanged(italic: Boolean) {
        callback?.setItalic(italic)
    }

    private fun notifyBoldChanged(bold: Boolean) {
        callback?.setBold(bold)
    }

    private fun notifyTextSizeChanged(textSize: Int) {
        callback?.setTextSize(textSize)
    }

    private fun notifyTextChanged(text: String) {
        callback?.setText(text)
    }

    private fun showEffectsDialog() {
        val effects = callback?.getToolEffects() ?: TextToolEffects()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pocketpaint_text_effects, null)

        val chkStroke = dialogView.findViewById<CheckBox>(R.id.chk_fx_stroke)
        val btnStroke = dialogView.findViewById<Button>(R.id.btn_fx_stroke_color)
        val chkGlow = dialogView.findViewById<CheckBox>(R.id.chk_fx_glow)
        val seekGlow = dialogView.findViewById<SeekBar>(R.id.seek_fx_glow)
        val btnGlow = dialogView.findViewById<Button>(R.id.btn_fx_glow_color)
        val chkShadow = dialogView.findViewById<CheckBox>(R.id.chk_fx_shadow)
        val btnShadow = dialogView.findViewById<Button>(R.id.btn_fx_shadow_color)
        val chkGradient = dialogView.findViewById<CheckBox>(R.id.chk_fx_gradient)
        val btnGradTop = dialogView.findViewById<Button>(R.id.btn_fx_grad_top)
        val btnGradBottom = dialogView.findViewById<Button>(R.id.btn_fx_grad_bottom)
        val chkPixel = dialogView.findViewById<CheckBox>(R.id.chk_fx_pixel)
        val chkDim = dialogView.findViewById<CheckBox>(R.id.chk_fx_dim)

        fun refreshEnabled() {
            btnStroke.isEnabled = chkStroke.isChecked
            seekGlow.isEnabled = chkGlow.isChecked
            btnGlow.isEnabled = chkGlow.isChecked
            btnShadow.isEnabled = chkShadow.isChecked
            btnGradTop.isEnabled = chkGradient.isChecked
            btnGradBottom.isEnabled = chkGradient.isChecked
        }
        fun refreshColors() {
            btnStroke.setBackgroundColor(effects.strokeColor)
            btnGlow.setBackgroundColor(effects.glowColor)
            btnShadow.setBackgroundColor(effects.shadowColor)
            btnGradTop.setBackgroundColor(effects.gradientTopColor)
            btnGradBottom.setBackgroundColor(effects.gradientBottomColor)
        }

        chkStroke.isChecked = effects.strokeWidth > 0f
        chkGlow.isChecked = effects.glowIntensity > 0
        seekGlow.progress = effects.glowIntensity.coerceIn(0, 9)
        chkShadow.isChecked = effects.shadowEnabled
        chkGradient.isChecked = effects.useGradient
        chkPixel.isChecked = effects.pixelCrisp
        chkDim.isChecked = effects.autoDimBackground
        refreshColors()
        refreshEnabled()

        fun colorPicker(initial: Int, onPicked: (Int) -> Unit) {
            val fm = (context as? FragmentActivity)?.supportFragmentManager ?: return
            val picker = ColorPickerDialog.newInstance(initial, catroidFlag = false)
            picker.addOnColorPickedListener(object : OnColorPickedListener {
                override fun colorChanged(color: Int) {
                    onPicked(color)
                    refreshColors()
                }
            })
            picker.show(fm, "textFxColorPicker")
        }
        btnStroke.setOnClickListener { if (chkStroke.isChecked) colorPicker(effects.strokeColor) { effects.strokeColor = it } }
        btnGlow.setOnClickListener { if (chkGlow.isChecked) colorPicker(effects.glowColor) { effects.glowColor = it } }
        btnShadow.setOnClickListener { if (chkShadow.isChecked) colorPicker(effects.shadowColor) { effects.shadowColor = it } }
        btnGradTop.setOnClickListener { if (chkGradient.isChecked) colorPicker(effects.gradientTopColor) { effects.gradientTopColor = it } }
        btnGradBottom.setOnClickListener {
            if (chkGradient.isChecked) colorPicker(effects.gradientBottomColor) { effects.gradientBottomColor = it }
        }
        seekGlow.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                effects.glowIntensity = progress + 1
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        for (chk in listOf(chkStroke, chkGlow, chkShadow, chkGradient)) {
            chk.setOnCheckedChangeListener { _, _ -> refreshEnabled() }
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.text_fx_title))
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (!chkStroke.isChecked) effects.strokeWidth = 0f else if (effects.strokeWidth <= 0f) effects.strokeWidth = 3f
                effects.glowIntensity = if (chkGlow.isChecked) seekGlow.progress.coerceAtLeast(1) else 0
                effects.shadowEnabled = chkShadow.isChecked
                effects.useGradient = chkGradient.isChecked
                effects.pixelCrisp = chkPixel.isChecked
                effects.autoDimBackground = chkDim.isChecked
                callback?.setToolEffects(effects)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun setState(
        bold: Boolean,
        italic: Boolean,
        underlined: Boolean,
        text: String,
        textSize: Int,
        fontEntry: FontEntry
    ) {
        boldToggleButton.isChecked = bold
        italicToggleButton.isChecked = italic
        underlinedToggleButton.isChecked = underlined
        textEditText.setText(text)
        val index = fontListAdapter?.fontEntries?.indexOfFirst { matches(it, fontEntry) } ?: -1
        if (index >= 0) {
            fontListAdapter?.setSelectedFontIndex(index)
        }
        notifyFontChanged(fontEntry)
        fontSizeText.setText(textSize.toString())
    }

    private fun matches(entry: FontEntry, target: FontEntry): Boolean = when {
        entry is FontEntry.BuiltIn && target is FontEntry.BuiltIn -> entry.fontType == target.fontType
        entry is FontEntry.Imported && target is FontEntry.Imported -> entry.font.name == target.font.name
        else -> false
    }

    override fun setCallback(listener: TextToolOptionsView.Callback) {
        callback = listener
    }

    override fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(textEditText.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInputFromWindow(textEditText.windowToken, InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun getTopLayout(): View = topLayout

    override fun getBottomLayout(): View = bottomLayout
    override fun setShapeSizeText(shapeSize: String) {
        textToolOptionsViewShapeSizeChip.setText(shapeSize)
        changeSizeShapeSizeChip.setText(shapeSize)
    }

    override fun toggleShapeSizeVisibility(isVisible: Boolean) {
        changeSizeShapeSizeChip.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
