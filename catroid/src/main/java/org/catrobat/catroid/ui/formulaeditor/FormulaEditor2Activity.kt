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
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.UserVariable
import java.util.Locale

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
        val categories = mutableListOf<Pair<String, List<CategoryBrowser.CategoryItem>>>()

        val mathFns = listOf("sin", "cos", "tan", "ln", "log", "sqrt", "abs", "round",
            "floor", "ceil", "exp", "pow", "mod", "max", "min", "sign", "lerp",
            "arcsin", "arccos", "arctan", "arctan2", "distan", "clamp", "map_range",
            "rgb", "hsv", "mix_color", "if_then_else", "to_hex", "to_dec")
        categories.add("Maths" to mathFns.map {
            CategoryBrowser.CategoryItem(it, it, getParamHint(it))
        })

        val strFns = listOf("length", "letter", "subtext", "upper", "lower", "reverse",
            "join", "join3", "joinnumber", "replace", "contains_str", "repeat",
            "random_str", "regex", "var", "varname", "varvalue")
        categories.add("Strings" to strFns.map {
            CategoryBrowser.CategoryItem(it, it, getParamHint(it))
        })

        val listFns = listOf("number_of_items", "list_item", "get_item", "contains",
            "index_of_item", "flatten", "connect", "find")
        categories.add("Lists" to listFns.map {
            CategoryBrowser.CategoryItem(it, it, getParamHint(it))
        })

        val fileFns = listOf("file", "files_path", "all_files", "file_size",
            "file_project_size", "file_size_in_dir", "file_size_at_path",
            "read_string", "file_exists", "file_project_exists")
        categories.add("File I/O" to fileFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        val deviceFns = listOf("device_name", "device_manufacturer", "android_version",
            "api_level", "system_language", "system_theme", "cpu_name", "cpu_architecture",
            "cpu_cores", "cpu_frequency", "total_ram", "free_ram", "total_storage",
            "free_storage", "battery_percent", "battery_charging", "battery_temp",
            "battery_voltage", "internet_connected", "screen_width", "screen_height",
            "screen_dpi", "screen_refresh", "is_pc", "is_mobile", "current_scene_name",
            "scene_time", "delta")
        categories.add("Device Info" to deviceFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        val spriteFns = listOf("sprite_exists", "sprite_x", "sprite_y", "sprite_size",
            "sprite_width", "sprite_height", "sprite_direction", "sprite_visible",
            "sprite_transparency", "sprite_layer", "sprite_name_get", "sprite_index_get",
            "sprite_uuid", "sprite_clone_count", "sprite_look_count", "sprite_distance",
            "sprite_touching", "sprite_angle_to")
        categories.add("Sprite Info" to spriteFns.map {
            CategoryBrowser.CategoryItem(it, it, getParamHint(it))
        })

        val jsonFns = listOf("json_get", "json_set", "json_is_valid")
        categories.add("JSON" to jsonFns.map {
            CategoryBrowser.CategoryItem(it, it, getParamHint(it))
        })

        CategoryBrowser.show(this, getString(R.string.formula_editor_functions), categories) { name, params ->
            insertWithParams(name)
        }
    }

    private fun showLogicBrowser() {
        val categories = listOf(
            "Boolean" to listOf("true", "false", "AND", "OR", "NOT").map {
                CategoryBrowser.CategoryItem(it, it)
            },
            "Comparison" to listOf("=", "!=", ">", "<", ">=", "<=").map {
                CategoryBrowser.CategoryItem(it, it)
            }
        )
        CategoryBrowser.show(this, getString(R.string.formula_editor_logic), categories) { name, _ ->
            insertAtCursor(name)
        }
    }

    private fun showObjectBrowser() {
        val categories = mutableListOf<Pair<String, List<CategoryBrowser.CategoryItem>>>()

        val lookFns = listOf("object_look_number", "object_number_of_looks",
            "object_look_width", "object_look_height", "object_look_name",
            "object_transparency", "object_brightness", "object_color",
            "object_layer", "color_touches_color")
        categories.add("Object Look" to lookFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        val moveFns = listOf("object_x", "object_y", "object_size", "object_width",
            "object_height", "object_direction", "object_x_velocity", "object_y_velocity",
            "object_angular_velocity", "object_distance_to", "collides_with_edge",
            "collides_with_finger", "touched", "object_touches_object")
        categories.add("Object Movement" to moveFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        CategoryBrowser.show(this, getString(R.string.formula_editor_object), categories) { name, _ ->
            insertAtCursor(name)
        }
    }

    private fun showSensorsBrowser() {
        val categories = mutableListOf<Pair<String, List<CategoryBrowser.CategoryItem>>>()

        val touchFns = listOf("finger_x", "finger_y", "finger_touched",
            "multi_finger_x", "multi_finger_y", "multi_finger_touched",
            "number_current_touches", "last_finger_index", "index_current_touch",
            "mouse_x", "mouse_y", "mouse_delta_x", "mouse_delta_y", "mouse_scroll")
        categories.add("Touch" to touchFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        val deviceSensors = listOf("x_acceleration", "y_acceleration", "z_acceleration",
            "compass_direction", "x_inclination", "y_inclination",
            "loudness", "latitude", "longitude", "location_accuracy", "altitude")
        categories.add("Device Sensors" to deviceSensors.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        val dateFns = listOf("timer", "date_year", "date_month", "date_day",
            "date_weekday", "time_hour", "time_minute", "time_second")
        categories.add("Date/Time" to dateFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        val legoFns = listOf("nxt_sensor_1", "nxt_sensor_2", "nxt_sensor_3", "nxt_sensor_4",
            "ev3_sensor_1", "ev3_sensor_2", "ev3_sensor_3", "ev3_sensor_4")
        categories.add("LEGO" to legoFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        val faceFns = listOf("face_detected", "face_size", "face_x", "face_y",
            "second_face_detected", "second_face_size", "second_face_x", "second_face_y")
        categories.add("Face Detection" to faceFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        val poseFns = listOf("nose_x", "nose_y", "left_eye_center_x", "left_eye_center_y",
            "right_eye_center_x", "right_eye_center_y", "left_ear_x", "left_ear_y",
            "right_ear_x", "right_ear_y", "mouth_left_corner_x", "mouth_left_corner_y",
            "left_shoulder_x", "left_shoulder_y", "right_shoulder_x", "right_shoulder_y",
            "left_wrist_x", "left_wrist_y", "right_wrist_x", "right_wrist_y")
        categories.add("Pose Detection" to poseFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        val stageFns = listOf("stage_width", "stage_height")
        categories.add("Stage" to stageFns.map {
            CategoryBrowser.CategoryItem(it, it, null)
        })

        CategoryBrowser.show(this, getString(R.string.formula_editor_device), categories) { name, _ ->
            insertAtCursor(name)
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

    private val NO_ARG_FUNCTIONS = setOf(
        "pi", "true", "false", "e", "nan", "infinity",
        "screen_width", "screen_height", "device_name", "current_language", "system_language",
        "last_finger_index", "finger_touched", "number_current_touches", "acceleration_x",
        "acceleration_y", "acceleration_z", "gps_latitude", "gps_longitude", "gps_altitude",
        "object_x", "object_y", "object_size", "object_direction", "object_look_number",
        "object_number_of_looks", "object_x_velocity", "object_y_velocity", "timer",
        "object_brightness", "object_transparency", "object_color", "object_volume",
        "object_rotation_style", "object_layer", "object_width", "object_height",
        "stage_width", "stage_height", "min_x", "max_x", "min_y", "max_y",
        "object_touching_finger", "object_touching_touch",
        "nxt_sensor_1", "nxt_sensor_2", "nxt_sensor_3", "nxt_sensor_4",
        "ev3_sensor_1", "ev3_sensor_2", "ev3_sensor_3", "ev3_sensor_4"
    )

    private fun getParamHint(name: String): String? {
        val lower = name.lowercase(Locale.US)
        if (lower in NO_ARG_FUNCTIONS) return null
        val hints = mapOf(
            "rand" to "min, max",
            "round" to "value",
            "floor" to "value",
            "ceil" to "value",
            "abs" to "value",
            "sin" to "degrees",
            "cos" to "degrees",
            "tan" to "degrees",
            "ln" to "value",
            "log" to "value",
            "sqrt" to "value",
            "exp" to "value",
            "pow" to "base, exponent",
            "mod" to "dividend, divisor",
            "max" to "value1, value2",
            "min" to "value1, value2",
            "sign" to "value",
            "lerp" to "start, end, t",
            "clamp" to "value, min, max",
            "length" to "string",
            "letter" to "index, string",
            "subtext" to "start, end, string",
            "upper" to "string",
            "lower" to "string",
            "reverse" to "string",
            "join" to "string1, string2",
            "join3" to "s1, s2, s3",
            "joinnumber" to "string, number",
            "replace" to "text, old, new",
            "contains_str" to "text, search",
            "repeat" to "text, count",
            "random_str" to "length",
            "regex" to "text, pattern",
            "number_of_items" to "list",
            "list_item" to "index, list",
            "get_item" to "index, list",
            "contains" to "item, list",
            "index_of_item" to "item, list",
            "flatten" to "list",
            "connect" to "list1, list2",
            "find" to "item, list",
            "json_get" to "json, path",
            "json_set" to "json, path, value",
            "json_is_valid" to "json",
            "if_then_else" to "condition, then, else",
            "distan" to "x1, y1, x2, y2",
            "map_range" to "value, fromLow, fromHigh, toLow, toHigh",
            "rgb" to "r, g, b",
            "hsv" to "h, s, v",
            "sprite_exists" to "sprite_name",
            "sprite_x" to "sprite_name",
            "sprite_y" to "sprite_name",
            "sprite_touching" to "sprite_name",
            "sprite_distance" to "sprite_name",
            "sprite_angle_to" to "sprite_name",
            "sprite_name_get" to "sprite_name",
            "to_hex" to "value",
            "to_dec" to "hex_string",
            "arcsin" to "value",
            "arccos" to "value",
            "arctan" to "value",
            "arctan2" to "y, x"
        )
        return hints[lower]
    }

    private fun insertWithParams(token: String) {
        val lower = token.lowercase(Locale.US)
        if (lower in NO_ARG_FUNCTIONS) {
            insertAtCursor(token)
        } else {
            val hint = getParamHint(token)
            val placeholder = hint ?: ""
            val insertText = "$token($placeholder)"
            val cursor = formulaInput.selectionStart.coerceAtMost(formulaInput.length())
            formulaInput.text.insert(cursor, insertText)
            if (placeholder.isNotEmpty()) {
                val selStart = cursor + token.length + 1
                val selEnd = selStart + placeholder.length
                formulaInput.setSelection(selStart, selEnd.coerceAtMost(formulaInput.length()))
            } else {
                formulaInput.setSelection((cursor + insertText.length).coerceAtMost(formulaInput.length()))
            }
        }
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

