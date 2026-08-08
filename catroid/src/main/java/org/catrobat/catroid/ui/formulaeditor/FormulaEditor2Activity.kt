package org.catrobat.catroid.ui.formulaeditor

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.FormulaBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.Functions
import org.catrobat.catroid.formulaeditor.InternFormulaParser
import org.catrobat.catroid.formulaeditor.Sensors
import org.catrobat.catroid.ui.neopaint.ColorPickerDialog
import org.catrobat.catroid.ui.recyclerview.fragment.CategoryListFragment
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.UserVariable

class FormulaEditor2Activity : AppCompatActivity() {

    companion object {
        const val EXTRA_FORMULA_STRING = "extra_formula_string"
        const val EXTRA_RESULT_FORMULA_STRING = "extra_result_formula_string"
        const val EXTRA_BRICK_NAME = "extra_brick_name"
        private const val SAVE_STATE_FORMULA = "save_state_formula"
        private const val SAVE_STATE_CURSOR = "save_state_cursor"
        private const val SAVE_STATE_FIELD_INDEX = "save_state_field_index"
        private const val MAX_UNDO_STACK = 50
        private const val DEBOUNCE_MS = 400L
        private const val HIGHLIGHT_ERROR_COLOR = 0x55F87171.toInt()
        private const val COLOR_FUNCTION = 0xFF00D4FF.toInt()
        private const val COLOR_NUMBER = 0xFF4ADE80.toInt()
        private const val COLOR_STRING = 0xFFC084FC.toInt()
        private const val COLOR_OPERATOR = 0xFFFBBF24.toInt()
        private const val COLOR_SENSOR = 0xFF38BDF8.toInt()
        private const val COLOR_BRACKET_MATCH = 0xFF22D3EE.toInt()
        private const val COLOR_BRACKET = 0xFF94A3B8.toInt()
        private const val REQUEST_FILE_PICKER = 9999
    }

    private lateinit var formulaInput: EditText
    private lateinit var errorText: TextView

    private val undoStack = mutableListOf<FormulaState>()
    private val redoStack = mutableListOf<FormulaState>()
    private var hasUnsavedChanges = false
    private var suppressHistoryPush = false
    private val handler = Handler(Looper.getMainLooper())
    private var validationRunnable: Runnable? = null

    private var formulaFields: MutableList<String> = mutableListOf()
    private var currentFieldIndex = 0

    private val clipboardHistory = mutableListOf<String>()
    private val clipboardHistoryMax = 10

    private var formulaBrick: FormulaBrick? = null
    private var currentFormulaField: Brick.FormulaField? = null

    private data class FormulaState(
        val text: String,
        val cursor: Int,
        val fieldIndex: Int
    )

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (hasUnsavedChanges) {
                AlertDialog.Builder(this@FormulaEditor2Activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(R.string.formula_editor_discard_changes_dialog_title)
                    .setMessage(R.string.formula_editor_discard_changes_dialog_message)
                    .setPositiveButton(R.string.save) { _, _ ->
                        hasUnsavedChanges = false
                        applyFormula()
                    }
                    .setNegativeButton(R.string.discard) { _, _ ->
                        hasUnsavedChanges = false
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                    .setNeutralButton(android.R.string.cancel, null)
                    .show()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = getString(R.string.formula_editor_2_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setContentView(R.layout.activity_formula_editor_2)

        formulaInput = findViewById(R.id.formula_editor_edit_field)
        errorText = TextView(this).apply {
            setPadding(16, 4, 16, 4)
            setTextColor(0xFFF87171.toInt())
            textSize = 12f
            visibility = View.GONE
        }
        val container = findViewById<android.widget.FrameLayout>(R.id.formula_editor_field_container)
        container?.addView(errorText)

        onBackPressedDispatcher.addCallback(this, backCallback)

        if (savedInstanceState != null) {
            formulaInput.setText(savedInstanceState.getString(SAVE_STATE_FORMULA, ""))
            currentFieldIndex = savedInstanceState.getInt(SAVE_STATE_FIELD_INDEX, 0)
            val cursor = savedInstanceState.getInt(SAVE_STATE_CURSOR, formulaInput.length())
            formulaInput.setSelection(cursor.coerceAtMost(formulaInput.length()))
        } else {
            val intentFormula = intent.getStringExtra(EXTRA_FORMULA_STRING) ?: ""
            formulaFields.clear()
            formulaFields.add(intentFormula)
            formulaInput.setText(intentFormula)
            formulaInput.setSelection(formulaInput.length())
        }

        val initialFormula = formulaInput.text.toString()
        val initialCursor = formulaInput.selectionStart
        pushState(FormulaState(initialFormula, initialCursor, currentFieldIndex), isInitial = true)
        hasUnsavedChanges = false

        formulaInput.addTextChangedListener(object : TextWatcher {
            private var beforeText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeText = s?.toString() ?: ""
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressHistoryPush) return
                val newText = s?.toString() ?: ""
                if (newText != beforeText) {
                    pushState(FormulaState(newText, formulaInput.selectionStart, currentFieldIndex))
                    hasUnsavedChanges = true
                    if (currentFieldIndex < formulaFields.size) {
                        formulaFields[currentFieldIndex] = newText
                    }
                }
                handler.removeCallbacks(validationRunnable ?: return)
                validationRunnable = Runnable { validateFormula(newText) }
                handler.postDelayed(validationRunnable!!, DEBOUNCE_MS)
                applySyntaxHighlighting()
            }
        })

        wireKeyboard()
        setupPhysicalKeyboard()
        applyUi2Colors()
        applySyntaxHighlighting()
    }

    override fun onResume() {
        super.onResume()
        backCallback.isEnabled = true
    }

    override fun onPause() {
        super.onPause()
        backCallback.isEnabled = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVE_STATE_FORMULA, formulaInput.text.toString())
        outState.putInt(SAVE_STATE_CURSOR, formulaInput.selectionStart)
        outState.putInt(SAVE_STATE_FIELD_INDEX, currentFieldIndex)
    }

    private fun pushState(state: FormulaState, isInitial: Boolean = false) {
        if (!isInitial) {
            undoStack.add(state)
            if (undoStack.size > MAX_UNDO_STACK) undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoButtons()
    }

    private fun undo() {
        if (undoStack.size <= 1) return
        val current = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(current)
        restoreState(undoStack.last())
        updateUndoRedoButtons()
    }

    private fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(next)
        restoreState(next)
        updateUndoRedoButtons()
    }

    private fun restoreState(state: FormulaState) {
        suppressHistoryPush = true
        formulaInput.setText(state.text)
        currentFieldIndex = state.fieldIndex
        formulaInput.setSelection(state.cursor.coerceAtMost(formulaInput.length()))
        suppressHistoryPush = false
        applySyntaxHighlighting()
        validateFormula(state.text)
    }

    private fun updateUndoRedoButtons() {
        val undoBtn = findViewById<View>(R.id.formula_editor_keyboard_undo)
        val redoBtn = findViewById<View>(R.id.formula_editor_keyboard_redo)
        undoBtn?.alpha = if (undoStack.size > 1) 1.0f else 0.3f
        undoBtn?.isEnabled = undoStack.size > 1
        redoBtn?.alpha = if (redoStack.isNotEmpty()) 1.0f else 0.3f
        redoBtn?.isEnabled = redoStack.isNotEmpty()
    }


    private fun setupPhysicalKeyboard() {
        formulaInput.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            val ctrl = event.isCtrlPressed || event.isMetaPressed

            when {
                ctrl && keyCode == KeyEvent.KEYCODE_Z -> { undo(); true }
                ctrl && keyCode == KeyEvent.KEYCODE_Y -> { redo(); true }
                ctrl && keyCode == KeyEvent.KEYCODE_C -> { copyFormula(); true }
                ctrl && keyCode == KeyEvent.KEYCODE_X -> { copyFormula(); deleteSelection(); true }
                ctrl && keyCode == KeyEvent.KEYCODE_V -> { pasteFormula(); true }
                ctrl && keyCode == KeyEvent.KEYCODE_A -> {
                    formulaInput.selectAll(); true
                }
                keyCode == KeyEvent.KEYCODE_ENTER -> {
                    applyFormula(); true
                }
                keyCode == KeyEvent.KEYCODE_DEL -> {
                    tokenAwareDelete(); true
                }
                else -> false
            }
        }
    }


    private fun tokenAwareDelete() {
        val text = formulaInput.text
        val cursor = formulaInput.selectionStart
        if (cursor <= 0 || text.isEmpty()) return

        val selStart = formulaInput.selectionStart
        val selEnd = formulaInput.selectionEnd
        if (selStart != selEnd) {
            text.delete(selStart, selEnd)
            formulaInput.setSelection(selStart)
            return
        }

        val str = text.toString()
        val beforeCursor = str.substring(0, cursor)

        val funcPattern = Regex("""([a-zA-Z_]\w*)\([^)]*$""")
        val funcMatch = funcPattern.find(beforeCursor)
        if (funcMatch != null) {
            val funcStart = cursor - funcMatch.value.length
            val afterCursor = str.substring(cursor)
            val closeIdx = afterCursor.indexOf(')')
            if (closeIdx >= 0) {
                text.delete(funcStart, cursor + closeIdx + 1)
                formulaInput.setSelection(funcStart)
                return
            }
        }

        val bracketPattern = Regex("""\([^)]*$""")
        val bracketMatch = bracketPattern.find(beforeCursor)
        if (bracketMatch != null && beforeCursor.endsWith("(")) {
            val afterCursor = str.substring(cursor)
            val closeIdx = afterCursor.indexOf(')')
            if (closeIdx >= 0) {
                val openIdx = cursor - 1
                text.delete(openIdx, cursor + closeIdx + 1)
                formulaInput.setSelection(openIdx)
                return
            }
        }

        if (cursor >= 2) {
            val twoBefore = str.substring(cursor - 2, cursor)
            if (twoBefore == "()") {
                text.delete(cursor - 2, cursor)
                formulaInput.setSelection(cursor - 2)
                return
            }
        }

        val start = cursor - 1
        val char = str[start]
        var end = cursor
        if (char.isLetter() || char == '_') {
            while (end < str.length && (str[end].isLetter() || str[end].isDigit() || str[end] == '_')) end++
        } else if (char.isDigit()) {
            while (end < str.length && (str[end].isDigit() || str[end] == '.')) end++
            if (end < str.length && str[end] == '(') end++
        }
        text.delete(start, end)
        formulaInput.setSelection(start)
    }


    private fun applySyntaxHighlighting() {
        val text = formulaInput.text.toString()
        if (text.isEmpty()) return

        val savedCursor = formulaInput.selectionStart
        val savedSelEnd = formulaInput.selectionEnd

        val ssb = SpannableStringBuilder(text)

        val functionNames = mutableSetOf<String>()
        Functions.values().forEach { functionNames.add(it.name.lowercase()) }
        Sensors.values().forEach { functionNames.add(it.name.lowercase()) }

        val keywords = setOf("true", "false", "pi", "and", "or", "not")

        var i = 0
        while (i < text.length) {
            val c = text[i]

            if (c == '"') {
                val end = text.indexOf('"', i + 1)
                if (end >= 0) {
                    ssb.setSpan(ForegroundColorSpan(COLOR_STRING), i, end + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end + 1
                } else {
                    ssb.setSpan(ForegroundColorSpan(COLOR_STRING), i, text.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = text.length
                }
            } else if (c.isLetter() || c == '_') {
                val start = i
                while (i < text.length && (text[i].isLetter() || text[i].isDigit() || text[i] == '_')) i++
                val word = text.substring(start, i).lowercase()
                val color = when {
                    word in keywords -> COLOR_OPERATOR
                    word in functionNames -> COLOR_FUNCTION
                    else -> COLOR_SENSOR
                }
                ssb.setSpan(ForegroundColorSpan(color), start, i,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (c.isDigit()) {
                val start = i
                while (i < text.length && (text[i].isDigit() || text[i] == '.')) i++
                ssb.setSpan(ForegroundColorSpan(COLOR_NUMBER), start, i,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (c in "+-*/%=<>!") {
                val start = i
                while (i < text.length && text[i] in "+-*/%=<>!") i++
                ssb.setSpan(ForegroundColorSpan(COLOR_OPERATOR), start, i,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                i++
            }
        }

        highlightBrackets(ssb, text)

        suppressHistoryPush = true
        formulaInput.setText(ssb)
        val safeCursor = savedCursor.coerceAtMost(formulaInput.length())
        val safeSelEnd = savedSelEnd.coerceAtMost(formulaInput.length())
        formulaInput.setSelection(safeCursor, safeSelEnd)
        suppressHistoryPush = false
    }

    private fun highlightBrackets(ssb: SpannableStringBuilder, text: String) {
        val stack = mutableListOf<Int>()
        for ((i, c) in text.withIndex()) {
            if (c == '(') {
                stack.add(i)
            } else if (c == ')') {
                if (stack.isNotEmpty()) {
                    val openIdx = stack.removeAt(stack.lastIndex)
                    val color = if (stack.size % 2 == 0) COLOR_BRACKET else COLOR_BRACKET_MATCH
                    ssb.setSpan(ForegroundColorSpan(color), openIdx, openIdx + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(ForegroundColorSpan(color), i, i + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }


    private fun validateFormula(text: String) {
        if (text.isBlank()) {
            errorText.visibility = View.GONE
            clearErrorHighlight()
            return
        }
        if (!isFormulaShapeValid(text)) {
            errorText.text = getString(R.string.formula_editor_2_check_brackets)
            errorText.visibility = View.VISIBLE
            highlightError()
            return
        }
        try {
            Formula(text)
            errorText.visibility = View.GONE
            clearErrorHighlight()
        } catch (e: RuntimeException) {
            val msg = e.message ?: getString(R.string.formula_editor_2_invalid_formula)
            errorText.text = msg.take(120)
            errorText.visibility = View.VISIBLE
            highlightErrorAtPosition(text)
        }
    }

    private fun highlightError() {
        val text = formulaInput.text
        text.setSpan(
            BackgroundColorSpan(HIGHLIGHT_ERROR_COLOR),
            0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun highlightErrorAtPosition(text: String) {
        val errorView = formulaInput
        errorView.post {
            val editable = errorView.text
            val spans = editable.getSpans(0, editable.length, BackgroundColorSpan::class.java)
            spans.forEach { editable.removeSpan(it) }
            val unmatchedBracket = findUnmatchedBracket(text)
            if (unmatchedBracket >= 0) {
                editable.setSpan(
                    BackgroundColorSpan(HIGHLIGHT_ERROR_COLOR),
                    unmatchedBracket, (unmatchedBracket + 1).coerceAtMost(editable.length),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun findUnmatchedBracket(text: String): Int {
        var depth = 0
        for ((i, c) in text.withIndex()) {
            if (c == '(') depth++
            else if (c == ')') {
                depth--
                if (depth < 0) return i
            }
        }
        if (depth > 0) {
            for (i in text.lastIndex downTo 0) {
                if (text[i] == '(') return i
            }
        }
        return -1
    }

    private fun clearErrorHighlight() {
        val text = formulaInput.text
        text.getSpans(0, text.length, BackgroundColorSpan::class.java).forEach { text.removeSpan(it) }
    }


    private fun applyUi2Colors() {
        val card = runCatching { getDrawable(R.drawable.bg_object_card_cube) }.getOrNull()
        val thumb = runCatching { getDrawable(R.drawable.bg_object_thumb_cube) }.getOrNull()
        val slate = 0xFF94A3B8.toInt()
        val white = 0xFFFFFFFF.toInt()

        val numberIds = intArrayOf(
            R.id.formula_editor_keyboard_0, R.id.formula_editor_keyboard_1,
            R.id.formula_editor_keyboard_2, R.id.formula_editor_keyboard_3,
            R.id.formula_editor_keyboard_4, R.id.formula_editor_keyboard_5,
            R.id.formula_editor_keyboard_6, R.id.formula_editor_keyboard_7,
            R.id.formula_editor_keyboard_8, R.id.formula_editor_keyboard_9,
            R.id.formula_editor_keyboard_plus, R.id.formula_editor_keyboard_minus,
            R.id.formula_editor_keyboard_mult, R.id.formula_editor_keyboard_divide,
            R.id.formula_editor_keyboard_decimal_mark,
            R.id.formula_editor_keyboard_bracket_open, R.id.formula_editor_keyboard_bracket_close
        )
        for (id in numberIds) {
            findViewById<View>(id)?.setUi2(background = thumb, textColor = white)
        }

        val categoryIds = intArrayOf(
            R.id.formula_editor_keyboard_function, R.id.formula_editor_keyboard_logic,
            R.id.formula_editor_keyboard_object, R.id.formula_editor_keyboard_sensors,
            R.id.formula_editor_keyboard_data, R.id.formula_editor_keyboard_string,
            R.id.formula_editor_keyboard_functional_button_toggle
        )
        for (id in categoryIds) {
            findViewById<View>(id)?.setUi2(background = card, textColor = slate)
        }

        val cardNeon = runCatching { getDrawable(R.drawable.bg_object_card_cube_neon) }.getOrNull()
        findViewById<View>(R.id.formula_editor_keyboard_color_picker)?.apply { background = card }
        findViewById<View>(R.id.formula_editor_keyboard_copy)?.apply { background = card }
        findViewById<View>(R.id.formula_editor_keyboard_paste)?.apply { background = card }
        findViewById<View>(R.id.formula_editor_keyboard_delete)?.setUi2(
            background = thumb, textColor = 0xFFF87171.toInt()
        )
        findViewById<View>(R.id.formula_editor_keyboard_compute)?.setUi2(
            background = cardNeon, textColor = white
        )

        val undoBtn = findViewById<View>(R.id.formula_editor_keyboard_undo)
        val redoBtn = findViewById<View>(R.id.formula_editor_keyboard_redo)
        undoBtn?.apply { setUi2(background = card, textColor = slate) }
        redoBtn?.apply { setUi2(background = card, textColor = slate) }
        updateUndoRedoButtons()

        formulaInput.background = runCatching { getDrawable(R.drawable.bg_object_card_cube) }.getOrNull()
        formulaInput.setTextColor(white)
        formulaInput.setHintTextColor(slate)
    }

    private fun View.setUi2(background: android.graphics.drawable.Drawable?, textColor: Int) {
        this.background = background
        when (this) {
            is TextView -> setTextColor(textColor)
            else -> Unit
        }
    }

    private fun wireKeyboard() {
        val digits = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        val digitIds = intArrayOf(
            R.id.formula_editor_keyboard_0, R.id.formula_editor_keyboard_1,
            R.id.formula_editor_keyboard_2, R.id.formula_editor_keyboard_3,
            R.id.formula_editor_keyboard_4, R.id.formula_editor_keyboard_5,
            R.id.formula_editor_keyboard_6, R.id.formula_editor_keyboard_7,
            R.id.formula_editor_keyboard_8, R.id.formula_editor_keyboard_9
        )
        for (i in digitIds.indices) {
            findViewById<View>(digitIds[i])?.setOnClickListener { insertAtCursor(digits[i]) }
        }

        findViewById<View>(R.id.formula_editor_keyboard_decimal_mark)?.setOnClickListener { insertAtCursor(".") }
        findViewById<View>(R.id.formula_editor_keyboard_plus)?.setOnClickListener { insertAtCursor("+") }
        findViewById<View>(R.id.formula_editor_keyboard_minus)?.setOnClickListener { insertAtCursor("-") }
        findViewById<View>(R.id.formula_editor_keyboard_mult)?.setOnClickListener { insertAtCursor("*") }
        findViewById<View>(R.id.formula_editor_keyboard_divide)?.setOnClickListener { insertAtCursor("/") }
        findViewById<View>(R.id.formula_editor_keyboard_bracket_open)?.setOnClickListener { insertAtCursor("(") }
        findViewById<View>(R.id.formula_editor_keyboard_bracket_close)?.setOnClickListener { insertAtCursor(")") }
        val deleteBtn = findViewById<View>(R.id.formula_editor_keyboard_delete)
        val deleteHandler = Handler(Looper.getMainLooper())
        var deleteRunning = false
        val deleteRunnable = object : Runnable {
            override fun run() {
                if (deleteRunning) {
                    tokenAwareDelete()
                    deleteHandler.postDelayed(this, 80)
                }
            }
        }
        deleteBtn?.setOnClickListener { tokenAwareDelete() }
        deleteBtn?.setOnLongClickListener {
            deleteRunning = true
            deleteHandler.postDelayed(deleteRunnable, 400)
            true
        }
        deleteBtn?.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP ||
                event.action == android.view.MotionEvent.ACTION_CANCEL) {
                deleteRunning = false
                deleteHandler.removeCallbacks(deleteRunnable)
            }
            false
        }

        findViewById<View>(R.id.formula_editor_keyboard_copy)?.setOnClickListener { copyFormula() }
        val pasteBtn = findViewById<View>(R.id.formula_editor_keyboard_paste)
        pasteBtn?.setOnClickListener { pasteFormula() }
        pasteBtn?.setOnLongClickListener { showClipboardHistoryDialog(); true }

        findViewById<View>(R.id.formula_editor_keyboard_functional_button_toggle)?.setOnClickListener { toggleFunctionalRows() }

        findViewById<View>(R.id.formula_editor_keyboard_undo)?.setOnClickListener { undo() }
        findViewById<View>(R.id.formula_editor_keyboard_redo)?.setOnClickListener { redo() }

        findViewById<View>(R.id.formula_editor_keyboard_function)?.setOnClickListener { showFunctionsBrowser() }
        findViewById<View>(R.id.formula_editor_keyboard_logic)?.setOnClickListener { showLogicBrowser() }
        findViewById<View>(R.id.formula_editor_keyboard_object)?.setOnClickListener { showObjectBrowser() }
        findViewById<View>(R.id.formula_editor_keyboard_sensors)?.setOnClickListener { showSensorsBrowser() }
        findViewById<View>(R.id.formula_editor_keyboard_data)?.setOnClickListener { showDataDialog() }
        findViewById<View>(R.id.formula_editor_keyboard_string)?.setOnClickListener { showStringDialog() }
        findViewById<View>(R.id.formula_editor_keyboard_color_picker)?.setOnClickListener { showColorDialog() }
        findViewById<View>(R.id.formula_editor_keyboard_compute)?.setOnClickListener { showComputeDialog() }
    }

    private fun toggleFunctionalRows() {
        val row1 = findViewById<View>(R.id.tableRow11)
        val row2 = findViewById<View>(R.id.tableRow12)
        val toggleBtn = findViewById<android.widget.ImageButton>(R.id.formula_editor_keyboard_functional_button_toggle)
        if (row1 == null || row2 == null) return
        val makeVisible = row1.visibility == View.GONE
        row1.visibility = if (makeVisible) View.VISIBLE else View.GONE
        row2.visibility = if (makeVisible) View.VISIBLE else View.GONE
        toggleBtn?.setImageResource(
            if (makeVisible) R.drawable.ic_keyboard_toggle_caret_up
            else R.drawable.ic_keyboard_toggle_caret_down
        )
    }

    private fun showFunctionsBrowser() {
        val categories = FormulaEditor2Menus.categoriesFor(this, CategoryListFragment.FUNCTION_TAG)
        if (categories.isEmpty()) {
            Toast.makeText(this, R.string.formula_editor_2_no_values, Toast.LENGTH_SHORT).show()
            return
        }
        CategoryBrowser.show(this, getString(R.string.formula_editor_functions), categories) { name, _ ->
            insertFromBrowser(name)
        }
    }

    private fun showLogicBrowser() {
        val categories = FormulaEditor2Menus.categoriesFor(this, CategoryListFragment.LOGIC_TAG)
        if (categories.isEmpty()) {
            Toast.makeText(this, R.string.formula_editor_2_no_values, Toast.LENGTH_SHORT).show()
            return
        }
        CategoryBrowser.show(this, getString(R.string.formula_editor_logic), categories) { name, _ ->
            insertFromBrowser(name)
        }
    }

    private fun showObjectBrowser() {
        val categories = FormulaEditor2Menus.categoriesFor(this, CategoryListFragment.OBJECT_TAG)
        if (categories.isEmpty()) {
            Toast.makeText(this, R.string.formula_editor_2_no_values, Toast.LENGTH_SHORT).show()
            return
        }
        CategoryBrowser.show(this, getString(R.string.formula_editor_object), categories) { name, _ ->
            insertFromBrowser(name)
        }
    }

    private fun showSensorsBrowser() {
        val categories = FormulaEditor2Menus.categoriesFor(this, CategoryListFragment.SENSOR_TAG)
        if (categories.isEmpty()) {
            Toast.makeText(this, R.string.formula_editor_2_no_values, Toast.LENGTH_SHORT).show()
            return
        }
        CategoryBrowser.show(this, getString(R.string.formula_editor_device), categories) { name, _ ->
            insertFromBrowser(name)
        }
    }

    private fun insertFromBrowser(resIdString: String) {
        val resId = resIdString.toIntOrNull()
        if (resId == null || resId <= 0) return
        val tokenText = FormulaEditor2Menus.insertionText(this, resId)
        if (tokenText != null) {
            insertFormulaTokenText(tokenText)
        } else {
            insertAtCursor(getString(resId))
        }
    }

    private fun insertFormulaTokenText(tokenText: String) {
        val paramStart = tokenText.indexOf('(')
        if (tokenText.endsWith(")") && paramStart >= 0) {
            val cursor = formulaInput.selectionStart.coerceAtMost(formulaInput.length())
            formulaInput.text.insert(cursor, tokenText)
            formulaInput.setSelection(cursor + paramStart + 1)
        } else {
            insertAtCursor(tokenText)
        }
    }

    private fun showDataDialog() {
        val options = mutableListOf(
            getString(R.string.formula_editor_2_data_formulas),
            getString(R.string.formula_editor_2_variables),
            getString(R.string.formula_editor_2_lists)
        )
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.formula_editor_data)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> showFunctionsBrowser()
                    1 -> {
                        val vars = variableNameTokens()
                        if (vars.isEmpty()) {
                            Toast.makeText(this, R.string.formula_editor_2_no_variables, Toast.LENGTH_SHORT).show()
                        } else {
                            showTokenListDialog(getString(R.string.formula_editor_2_variables), vars)
                        }
                    }
                    else -> {
                        val lists = listNameTokens()
                        if (lists.isEmpty()) {
                            Toast.makeText(this, R.string.formula_editor_2_no_lists, Toast.LENGTH_SHORT).show()
                        } else {
                            showTokenListDialog(getString(R.string.formula_editor_2_lists), lists)
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTokenListDialog(title: String, tokens: List<String>) {
        if (tokens.isEmpty()) {
            Toast.makeText(this, R.string.formula_editor_2_no_values, Toast.LENGTH_SHORT).show()
            return
        }
        val categories = listOf("" to tokens.map {
            CategoryBrowser.CategoryItem(it, it)
        })
        CategoryBrowser.show(this, title, categories) { name, _ ->
            insertAtCursor(name)
        }
    }

    private fun showStringDialog() {
        val input = EditText(this)
        val p = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(p, p, p, p)
        input.hint = getString(R.string.formula_editor_string)

        val warningText = TextView(this).apply {
            text = getString(R.string.formula_editor_2_string_warning)
            setTextColor(0xFFFBBF24.toInt())
            setPadding(p, p, p, 0)
            visibility = View.GONE
        }

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                val hasFormula = text.contains(Regex("""[a-zA-Z_]\w*\(""")) ||
                    text.contains(Regex("""\d+\s*[+\-*/%]\s*\d+"""))
                warningText.visibility = if (hasFormula && text.length > 3) View.VISIBLE else View.GONE
            }
        })

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(input)
            addView(warningText)
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.formula_editor_string)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                insertAtCursor("\"" + input.text.toString() + "\"")
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showColorDialog() {
        val currentColor = try {
            val text = formulaInput.text.toString()
            val hexMatch = Regex("""#([0-9A-Fa-f]{6,8})""").find(text)
            if (hexMatch != null) {
                Color.parseColor(hexMatch.value)
            } else {
                Color.WHITE
            }
        } catch (_: Exception) { Color.WHITE }

        ColorPickerDialog(this, currentColor) { color ->
            val hex = String.format("#%08X", color)
            insertAtCursor(hex)
        }.show()
    }

    private fun copyFormula() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val selStart = formulaInput.selectionStart
        val selEnd = formulaInput.selectionEnd
        val text = if (selStart != selEnd) {
            formulaInput.text.substring(selStart, selEnd)
        } else {
            formulaInput.text.toString()
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("formula", text))
        addToClipboardHistory(text)
        Toast.makeText(this, R.string.formula_editor_2_copied, Toast.LENGTH_SHORT).show()
    }

    private fun pasteFormula() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            insertAtCursor(clip.getItemAt(0).coerceToText(this).toString())
        }
    }

    private fun deleteSelection() {
        val selStart = formulaInput.selectionStart
        val selEnd = formulaInput.selectionEnd
        if (selStart != selEnd) {
            formulaInput.text.delete(selStart, selEnd)
            formulaInput.setSelection(selStart)
        }
    }

    private fun insertAtCursor(text: String) {
        val cursor = formulaInput.selectionStart.coerceAtLeast(0)
        formulaInput.text.insert(cursor, text)
        formulaInput.setSelection((cursor + text.length).coerceAtMost(formulaInput.length()))
    }

    private fun addToClipboardHistory(text: String) {
        clipboardHistory.remove(text)
        clipboardHistory.add(0, text)
        if (clipboardHistory.size > clipboardHistoryMax) {
            clipboardHistory.removeAt(clipboardHistory.lastIndex)
        }
    }

    private fun showClipboardHistoryDialog() {
        val allItems = mutableListOf<String>()
        val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
        if (clip != null && clip.itemCount > 0) {
            val current = clip.getItemAt(0).coerceToText(this).toString()
            if (current.isNotEmpty()) allItems.add(current)
        }
        for (item in clipboardHistory) {
            if (item !in allItems) allItems.add(item)
        }
        if (allItems.isEmpty()) {
            Toast.makeText(this, R.string.formula_editor_2_no_clipboard, Toast.LENGTH_SHORT).show()
            return
        }
        val displayItems = allItems.map { if (it.length > 60) it.take(57) + "..." else it }.toTypedArray()
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.formula_editor_2_clipboard_history)
            .setItems(displayItems) { _, which -> insertAtCursor(allItems[which]) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun isFormulaShapeValid(value: String): Boolean {
        val text = value.trim()
        if (text.isEmpty()) return false
        var depth = 0
        for (char in text) {
            when (char) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth < 0) return false
                }
            }
        }
        return depth == 0
    }

    private fun applyFormula() {
        val result = formulaInput.text.toString()
        if (!isFormulaShapeValid(result)) {
            Toast.makeText(this, R.string.formula_editor_2_check_brackets, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Formula(result)
        } catch (_: RuntimeException) {
            Toast.makeText(this, R.string.formula_editor_2_invalid_formula, Toast.LENGTH_SHORT).show()
            return
        }
        hasUnsavedChanges = false
        val returnIntent = Intent().apply {
            putExtra(EXTRA_RESULT_FORMULA_STRING, result)
        }
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        backCallback.handleOnBackPressed()
        return true
    }

    private fun showVariableManager() {
        val project = ProjectManager.getInstance().currentProject ?: return
        val sprite = ProjectManager.getInstance().currentSprite
        val allVars = mutableListOf<Pair<String, UserVariable>>()
        sprite?.getUserVariables()?.forEach { allVars.add("${sprite.name}: ${it.name}" to it) }
        project.getUserVariables()?.forEach { allVars.add("Global: ${it.name}" to it) }

        val items = mutableListOf(
            getString(R.string.formula_editor_2_create_variable),
            getString(R.string.formula_editor_2_create_global_variable)
        )
        allVars.forEach { items.add(it.first) }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.formula_editor_2_manage_variables)
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> showCreateVariableDialog(isGlobal = false)
                    1 -> showCreateVariableDialog(isGlobal = true)
                    else -> {
                        val (_, variable) = allVars[which - 2]
                        showVariableOptionsDialog(variable)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCreateVariableDialog(isGlobal: Boolean) {
        val input = EditText(this).apply {
            hint = getString(R.string.formula_editor_2_variable_name_hint)
            val p = (16 * resources.displayMetrics.density).toInt(); setPadding(p, p, p, p)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(if (isGlobal) R.string.formula_editor_2_create_global_variable else R.string.formula_editor_2_create_variable)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val variable = UserVariable(name)
                if (isGlobal) ProjectManager.getInstance().currentProject?.addUserVariable(variable)
                else ProjectManager.getInstance().currentSprite?.addUserVariable(variable)
                saveProjectToDisk()
                Toast.makeText(this, getString(R.string.formula_editor_2_variable_created, name), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showVariableOptionsDialog(variable: UserVariable) {
        val options = arrayOf(
            getString(R.string.formula_editor_2_insert_variable),
            getString(R.string.formula_editor_2_rename_variable),
            getString(R.string.formula_editor_2_delete_variable)
        )
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(variable.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> insertAtCursor(variable.name)
                    1 -> showRenameVariableDialog(variable)
                    2 -> showDeleteVariableDialog(variable)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showRenameVariableDialog(variable: UserVariable) {
        val input = EditText(this).apply {
            setText(variable.name)
            val p = (16 * resources.displayMetrics.density).toInt(); setPadding(p, p, p, p)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.formula_editor_2_rename_variable)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty() || newName == variable.name) return@setPositiveButton
                val oldName = variable.name
                variable.name = newName
                renameVariableInFormulas(oldName, newName)
                saveProjectToDisk()
                Toast.makeText(this, getString(R.string.formula_editor_2_variable_renamed, newName), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteVariableDialog(variable: UserVariable) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.formula_editor_2_delete_variable)
            .setMessage(getString(R.string.formula_editor_2_delete_variable_confirm, variable.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ProjectManager.getInstance().currentProject?.getUserVariables()?.remove(variable)
                ProjectManager.getInstance().currentSprite?.getUserVariables()?.remove(variable)
                saveProjectToDisk()
                Toast.makeText(this, getString(R.string.formula_editor_2_variable_deleted, variable.name), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun renameVariableInFormulas(oldName: String, newName: String) {
        try {
            val project = ProjectManager.getInstance().currentProject ?: return
            val sprites = project.spriteListWithClones ?: return
            for (sprite in sprites) {
                for (script in sprite.scriptList) {
                    for (brick in script.brickList) {
                        if (brick is FormulaBrick) {
                            val fields = brick.allFormulaFieldsWithFormulas
                            for (entry in fields.entries) {
                                try {
                                    entry.value.updateVariableName(oldName, newName)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_FILE_PICKER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE_PICKER && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val path = uri.path ?: uri.toString()
                insertAtCursor("\"$path\"")
            }
        }
    }

    private fun saveProjectToDisk() {
        try {
            val project = ProjectManager.getInstance().currentProject ?: return
            org.catrobat.catroid.io.XstreamSerializer.getInstance().saveProject(project)
        } catch (_: Exception) {}
    }

    private fun showComputeDialog() {
        val text = formulaInput.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, R.string.formula_editor_2_no_formula, Toast.LENGTH_SHORT).show()
            return
        }
        if (!isFormulaShapeValid(text)) {
            Toast.makeText(this, R.string.formula_editor_2_check_brackets, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val formula = Formula(text)
            val project = ProjectManager.getInstance().currentProject
            val sprite = ProjectManager.getInstance().currentSprite
            val scope = Scope(project!!, sprite!!, null)
            val result = formula.getUserFriendlyString(null, scope)
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.formula_editor_2_compute)
                .setMessage("= $result")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.formula_editor_2_invalid_formula, Toast.LENGTH_SHORT).show()
        }
    }

    private fun variableNameTokens(): List<String> {
        val projectVars = ProjectManager.getInstance().currentProject?.getUserVariables()
            ?.map { it.name } ?: emptyList()
        val spriteVars = ProjectManager.getInstance().currentSprite?.getUserVariables()
            ?.map { it.name } ?: emptyList()
        return (spriteVars + projectVars).distinct()
    }

    private fun listNameTokens(): List<String> {
        val projectLists = ProjectManager.getInstance().currentProject?.getUserLists()
            ?.map { it.name } ?: emptyList()
        val spriteLists = ProjectManager.getInstance().currentSprite?.getUserLists()
            ?.map { it.name } ?: emptyList()
        return (spriteLists + projectLists).distinct()
    }
}

