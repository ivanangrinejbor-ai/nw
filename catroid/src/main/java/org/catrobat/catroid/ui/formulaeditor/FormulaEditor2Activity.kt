package org.catrobat.catroid.ui.formulaeditor

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.Functions
import java.util.Locale

class FormulaEditor2Activity : AppCompatActivity() {

    companion object {
        const val EXTRA_FORMULA_STRING = "extra_formula_string"
        const val EXTRA_RESULT_FORMULA_STRING = "extra_result_formula_string"
    }

    private lateinit var formulaInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Редактор формул"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setContentView(R.layout.activity_formula_editor_2)

        formulaInput = findViewById(R.id.formula_editor_edit_field)
        formulaInput.setText(intent.getStringExtra(EXTRA_FORMULA_STRING) ?: "")
        formulaInput.setSelection(formulaInput.length())

        wireKeyboard()
        applyUi2Colors()
    }

    private fun applyUi2Colors() {
        val card = getDrawable(R.drawable.bg_object_card_cube)
        val thumb = getDrawable(R.drawable.bg_object_thumb_cube)
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

        findViewById<View>(R.id.formula_editor_keyboard_color_picker)?.apply { background = card }
        findViewById<View>(R.id.formula_editor_keyboard_copy)?.apply { background = card }
        findViewById<View>(R.id.formula_editor_keyboard_paste)?.apply { background = card }
        findViewById<View>(R.id.formula_editor_keyboard_delete)?.setUi2(
            background = thumb, textColor = 0xFFF87171.toInt()
        )

        findViewById<View>(R.id.formula_editor_keyboard_compute)?.setUi2(
            background = getDrawable(R.drawable.bg_object_card_cube_neon), textColor = white
        )

        formulaInput.background = getDrawable(R.drawable.bg_object_card_cube)
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
            R.id.formula_editor_keyboard_0,
            R.id.formula_editor_keyboard_1,
            R.id.formula_editor_keyboard_2,
            R.id.formula_editor_keyboard_3,
            R.id.formula_editor_keyboard_4,
            R.id.formula_editor_keyboard_5,
            R.id.formula_editor_keyboard_6,
            R.id.formula_editor_keyboard_7,
            R.id.formula_editor_keyboard_8,
            R.id.formula_editor_keyboard_9
        )
        for (i in digitIds.indices) {
            findViewById<View>(digitIds[i])?.setOnClickListener { insertAtCursor(digits[i]) }
        }

        findViewById<View>(R.id.formula_editor_keyboard_plus)?.setOnClickListener { insertAtCursor("+") }
        findViewById<View>(R.id.formula_editor_keyboard_minus)?.setOnClickListener { insertAtCursor("-") }
        findViewById<View>(R.id.formula_editor_keyboard_mult)?.setOnClickListener { insertAtCursor("*") }
        findViewById<View>(R.id.formula_editor_keyboard_divide)?.setOnClickListener { insertAtCursor("/") }
        findViewById<View>(R.id.formula_editor_keyboard_bracket_open)?.setOnClickListener { insertAtCursor("(") }
        findViewById<View>(R.id.formula_editor_keyboard_bracket_close)?.setOnClickListener { insertAtCursor(")") }
        findViewById<View>(R.id.formula_editor_keyboard_delete)?.setOnClickListener { deleteLastCharacter() }

        findViewById<View>(R.id.formula_editor_keyboard_copy)?.setOnClickListener { copyFormula() }
        findViewById<View>(R.id.formula_editor_keyboard_paste)?.setOnClickListener { pasteFormula() }
        findViewById<View>(R.id.formula_editor_keyboard_functional_button_toggle)?.setOnClickListener { toggleFunctionalRows() }

        findViewById<View>(R.id.formula_editor_keyboard_function)?.setOnClickListener { showFunctionDialog() }
        findViewById<View>(R.id.formula_editor_keyboard_logic)?.setOnClickListener {
            showTokenListDialog(getString(R.string.formula_editor_logic), logicTokens())
        }
        findViewById<View>(R.id.formula_editor_keyboard_object)?.setOnClickListener {
            showTokenListDialog(getString(R.string.formula_editor_choose_object_variable), variableNameTokens())
        }
        findViewById<View>(R.id.formula_editor_keyboard_sensors)?.setOnClickListener {
            showTokenListDialog(getString(R.string.formula_editor_device), sensorTokens())
        }
        findViewById<View>(R.id.formula_editor_keyboard_data)?.setOnClickListener { showDataDialog() }
        findViewById<View>(R.id.formula_editor_keyboard_string)?.setOnClickListener { showStringDialog() }
        findViewById<View>(R.id.formula_editor_keyboard_color_picker)?.setOnClickListener { showColorDialog() }
        findViewById<View>(R.id.formula_editor_keyboard_compute)?.setOnClickListener { applyFormula() }
    }

    private fun toggleFunctionalRows() {
        val row1 = findViewById<View>(R.id.tableRow11)
        val row2 = findViewById<View>(R.id.tableRow12)
        if (row1 == null || row2 == null) {
            return
        }
        val makeVisible = row1.visibility == View.GONE
        row1.visibility = if (makeVisible) View.VISIBLE else View.GONE
        row2.visibility = if (makeVisible) View.VISIBLE else View.GONE
    }

    private fun copyFormula() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("formula", formulaInput.text.toString()))
        Toast.makeText(this, "Скопировано", Toast.LENGTH_SHORT).show()
    }

    private fun pasteFormula() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            insertAtCursor(clip.getItemAt(0).coerceToText(this).toString())
        }
    }

    private fun showFunctionDialog() {
        showTokenListDialog(getString(R.string.formula_editor_functions), functionTokens())
    }

    private fun functionTokens(): List<String> {
        return Functions.values()
            .filter { !it.name.startsWith("VAR") }
            .map { it.name.toLowerCase(Locale.US) }
    }

    private fun logicTokens(): List<String> = listOf("true", "false", "AND", "OR", "NOT",
        "=", "!=", ">", "<", ">=", "<=")

    private fun sensorTokens(): List<String> {
        return Functions.values()
            .filter {
                val n = it.name
                n.startsWith("SPRITE_") || n.startsWith("SCREEN_") || n.startsWith("COLOR_") ||
                        n.startsWith("TOUCH") || n.startsWith("GET_") || n == "DELTA"
            }
            .map { it.name.toLowerCase(Locale.US) }
    }

    private fun variableNameTokens(): List<String> {
        val projectVars = ProjectManager.getInstance().getCurrentProject()?.getUserVariables()
            ?.map { it.name } ?: emptyList()
        val spriteVars = ProjectManager.getInstance().getCurrentSprite()?.getUserVariables()
            ?.map { it.name } ?: emptyList()
        return (spriteVars + projectVars).distinct()
    }

    private fun listNameTokens(): List<String> {
        val projectLists = ProjectManager.getInstance().getCurrentProject()?.getUserLists()
            ?.map { it.name } ?: emptyList()
        val spriteLists = ProjectManager.getInstance().getCurrentSprite()?.getUserLists()
            ?.map { it.name } ?: emptyList()
        return (spriteLists + projectLists).distinct()
    }

    private fun showDataDialog() {
        val options = mutableListOf("Формулы данных", "Переменные", "Списки")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.formula_editor_data)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> showTokenListDialog("Формулы данных", dataTokens())
                    1 -> {
                        val vars = variableNameTokens()
                        if (vars.isEmpty()) {
                            Toast.makeText(this, "Нет переменных", Toast.LENGTH_SHORT).show()
                        } else {
                            showTokenListDialog("Переменные", vars)
                        }
                    }
                    else -> {
                        val lists = listNameTokens()
                        if (lists.isEmpty()) {
                            Toast.makeText(this, "Нет списков", Toast.LENGTH_SHORT).show()
                        } else {
                            showTokenListDialog("Списки", lists)
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun dataTokens(): List<String> = functionTokens().filter {
        it.startsWith("list_") || it.startsWith("table_") || it.startsWith("var")
    }

    private fun showTokenListDialog(title: String, tokens: List<String>) {
        if (tokens.isEmpty()) {
            Toast.makeText(this, "Нет доступных значений", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(title)
            .setItems(tokens.toTypedArray()) { _, which ->
                insertAtCursor(functionInsertText(tokens[which]))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showStringDialog() {
        val input = EditText(this)
        val p = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(p, p, p, p)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.formula_editor_string)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                insertAtCursor("\"" + input.text.toString() + "\"")
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showColorDialog() {
        val labels = arrayOf(
            "Красный", "Фуксия", "Жёлтый", "Зелёный", "Голубой", "Синий",
            "Чёрный", "Белый", "Оранжевый", "Фиолетовый"
        )
        val colors = arrayOf(
            "#FF0000", "#FF00FF", "#FFFF00", "#00FF00", "#00FFFF", "#0000FF",
            "#000000", "#FFFFFF", "#FF8800", "#8800FF"
        )
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Цвет")
            .setItems(labels) { _, which -> insertAtCursor(colors[which]) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun functionInsertText(token: String): String = when (token.uppercase(Locale.US)) {
        "PI", "TRUE", "FALSE" -> token
        else -> "$token("
    }

    private fun insertAtCursor(text: String) {
        val cursor = formulaInput.selectionStart.coerceAtLeast(0)
        formulaInput.text.insert(cursor, text)
        formulaInput.setSelection((cursor + text.length).coerceAtMost(formulaInput.length()))
    }

    private fun deleteLastCharacter() {
        val text = formulaInput.text
        val cursor = formulaInput.selectionStart.coerceAtLeast(0)
        if (cursor > 0 && text.isNotEmpty()) {
            text.delete(cursor - 1, cursor)
            formulaInput.setSelection(cursor - 1)
        }
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
            Toast.makeText(this, "Проверьте скобки и выражение", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Formula(result)
        } catch (_: RuntimeException) {
            Toast.makeText(this, "Некорректная формула", Toast.LENGTH_SHORT).show()
            return
        }
        val returnIntent = Intent().apply {
            putExtra(EXTRA_RESULT_FORMULA_STRING, result)
        }
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}