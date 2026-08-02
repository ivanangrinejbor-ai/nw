package org.catrobat.catroid.ui.formulaeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
        const val CAT_BASIC = "basic"
        const val CAT_MATH = "math"
        const val CAT_STRINGS = "strings"
        const val CAT_LISTS = "lists"
        const val CAT_LOGIC = "logic"
        const val CAT_SENSORS = "sensors"
        const val CAT_OBJECT = "object"
        const val CAT_DATA = "data"
        const val CAT_3D = "3d"
        const val CAT_DEVICE = "device"
        const val CAT_SECURITY = "security"
        const val CAT_ALL = "all"
        const val CAT_VARIABLES = "variables"
        const val CAT_USER_LISTS = "user_lists"
    }

    private lateinit var formulaInput: EditText
    private lateinit var keysContainer: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Редактор формул 2.0"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0B1220.toInt())
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }

        val inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_object_card_cube)
            gravity = Gravity.CENTER_VERTICAL
            val p = (12 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = p }
        }

        formulaInput = EditText(this).apply {
            val initial = intent.getStringExtra(EXTRA_FORMULA_STRING) ?: ""
            setText(initial)
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputCard.addView(formulaInput)

        val backspaceBtn = TextView(this).apply {
            text = "⌫"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding((12 * resources.displayMetrics.density).toInt(), 0, 0, 0)
            setOnClickListener {
                val text = formulaInput.text.toString()
                if (text.isNotEmpty()) {
                    formulaInput.setText(text.substring(0, text.length - 1))
                    formulaInput.setSelection(formulaInput.text.length)
                }
            }
        }
        inputCard.addView(backspaceBtn)

        val clearBtn = TextView(this).apply {
            text = "C"
            setTextColor(0xFFEF4444.toInt())
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding((12 * resources.displayMetrics.density).toInt(), 0, 0, 0)
            setOnClickListener {
                formulaInput.setText("")
            }
        }
        inputCard.addView(clearBtn)

        root.addView(inputCard)

        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (10 * resources.displayMetrics.density).toInt() }
        }

        val categoryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categories = listOf(
            CAT_BASIC, CAT_MATH, CAT_STRINGS, CAT_LISTS, CAT_LOGIC,
            CAT_SENSORS, CAT_OBJECT, CAT_DATA, CAT_3D, CAT_DEVICE, CAT_SECURITY, CAT_ALL,
            CAT_VARIABLES, CAT_USER_LISTS
        )
        for (cat in categories) {
            val btn = TextView(this).apply {
                text = categoryLabel(cat)
                setTextColor(0xFF38BDF8.toInt())
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setPadding(
                    (12 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt()
                )
                setBackgroundResource(R.drawable.bg_object_card_cube)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.rightMargin = (8 * resources.displayMetrics.density).toInt()
                layoutParams = lp
                setOnClickListener { loadCategoryKeys(cat) }
            }
            categoryLayout.addView(btn)
        }
        categoryScroll.addView(categoryLayout)
        root.addView(categoryScroll)

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        keysContainer = GridLayout(this).apply {
            columnCount = if (isTablet) 6 else 4
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        // Keep one weighted grid, as in the stable editor.  A second weighted
        // container or a nested ScrollView makes GridLayout measure to zero on
        // some Android configurations.
        root.addView(keysContainer)

        val doneBtn = TextView(this).apply {
            text = "Применить формулу 2.0"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_object_card_cube)
            val p = (14 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            setOnClickListener {
                val result = formulaInput.text.toString()
                if (!isFormulaShapeValid(result)) {
                    Toast.makeText(this@FormulaEditor2Activity, "Проверьте скобки и выражение", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                try {
                    Formula(result)
                } catch (_: RuntimeException) {
                    Toast.makeText(this@FormulaEditor2Activity, "Некорректная формула", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val returnIntent = Intent().apply {
                    putExtra(EXTRA_RESULT_FORMULA_STRING, result)
                }
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
        }
        root.addView(doneBtn)

        setContentView(root)

        loadCategoryKeys(CAT_BASIC)
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

    private fun categoryLabel(category: String): String = when (category) {
        CAT_BASIC -> "Основные"
        CAT_MATH -> getString(R.string.formula_editor_functions_maths)
        CAT_STRINGS -> getString(R.string.formula_editor_functions_strings)
        CAT_LISTS -> getString(R.string.formula_editor_functions_lists)
        CAT_LOGIC -> getString(R.string.formula_editor_logic)
        CAT_SENSORS -> getString(R.string.formula_editor_device)
        CAT_OBJECT -> getString(R.string.formula_editor_object)
        CAT_DATA -> getString(R.string.formula_editor_data)
        CAT_3D -> "3D"
        CAT_DEVICE -> "Device / files"
        CAT_SECURITY -> "Security / JSON"
        CAT_ALL -> "All formulas"
        CAT_VARIABLES -> "Variables"
        CAT_USER_LISTS -> "Lists"
        else -> category
    }

    private fun loadCategoryKeys(category: String) {
        keysContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val margin = (4 * density).toInt()
        val keys: List<String> = when (category) {
            CAT_BASIC -> listOf(
                "7", "8", "9", "+", "(",
                "4", "5", "6", "-", ")",
                "1", "2", "3", "*", "=",
                "0", ".", ",", "/", ">", "<", ">=", "<=", "!=", "AND", "OR", "NOT"
            )
            CAT_MATH -> functionTokens(category) { name ->
                name in setOf("SIN", "COS", "TAN", "LN", "LOG", "SQRT", "RAND", "ROUND", "ROUNDTO",
                    "ABS", "PI", "MOD", "ARCSIN", "ARCCOS", "ARCTAN", "ARCTAN2", "EXP", "POWER",
                    "FLOOR", "CEIL", "SIGN", "LERP", "MAP_RANGE", "MAX", "MIN", "CLAMP")
            }
            CAT_STRINGS -> functionTokens(category) { name ->
                name in setOf("LENGTH", "LETTER", "SUBTEXT", "UPPER", "LOWER", "REVERSE", "RANDOM_STR",
                    "REPLACE", "CONTAINS_STR", "REPEAT", "JOIN", "JOIN3", "JOINNUMBER", "REGEX",
                    "TO_HEX", "TO_DEC", "FILE", "JSON_GET", "JSON_SET", "JSON_IS_VALID")
            }
            CAT_LISTS -> functionTokens(category) { name ->
                name in setOf("LIST_ITEM", "CONTAINS", "INDEX_OF_ITEM", "NUMBER_OF_ITEMS", "FLATTEN",
                    "CONNECT", "FIND", "GET_ITEM", "TABLE_X", "TABLE_Y", "TABLE_ELEMENT", "TABLE_JOIN")
            }
            CAT_LOGIC -> listOf("true", "false", "AND", "OR", "NOT", "=", "!=", ">", "<", ">=", "<=")
            CAT_SENSORS -> functionTokens(category) { name ->
                name.startsWith("SPRITE_") || name.startsWith("SCREEN_") || name.startsWith("MULTI_") ||
                    name.startsWith("COLOR_") || name.startsWith("TOUCH") || name.startsWith("COLL") ||
                    name.startsWith("GET_") || name == "DELTA" || name == "CURRENT_STATE"
            }
            CAT_OBJECT -> functionTokens(category) { name ->
                name.startsWith("SPRITE_") || name.startsWith("VIEW_") || name.startsWith("OBJECT_") ||
                    name == "DISTANCE" || name == "SPRITE_DISTANCE" || name == "SPRITE_ANGLE_TO" ||
                    name == "TOUCHES_OBJECT_BY_NAME"
            }
            CAT_DATA -> functionTokens(category) { name ->
                name == "VAR" || name == "VARNAME" || name == "VARVALUE" || name.startsWith("LIST_") ||
                    name.startsWith("TABLE_") || name == "NUMBER_OF_ITEMS" || name == "GET_ITEM"
            }
            CAT_3D -> functionTokens(category) { name ->
                name.startsWith("GET_3D_") || name.startsWith("GET_CAMERA_") || name.startsWith("OBJECT_") ||
                    name.startsWith("RAY_") || name == "GET_ANGLE" || name == "GET_DIRECTION_X" || name == "GET_DIRECTION_Y"
            }
            CAT_DEVICE -> functionTokens(category) { name ->
                name.startsWith("DEVICE_") || name.startsWith("ANDROID_") || name.startsWith("CPU_") ||
                    name.startsWith("BATTERY_") || name.startsWith("INTERNET_") || name.startsWith("FILE_") ||
                    name.startsWith("TOTAL_") || name.startsWith("FREE_") || name.startsWith("VOLUME_") ||
                    name.startsWith("GPU_") || name.startsWith("OPENGL_") || name.startsWith("VULKAN_") ||
                    name.startsWith("API_") || name == "SYSTEM_LANGUAGE" || name == "SYSTEM_THEME"
            }
            CAT_SECURITY -> functionTokens(category) { name ->
                name.startsWith("SHA_") || name.startsWith("HASH_") || name.startsWith("AES_") ||
                    name.startsWith("CHACHA_") || name.startsWith("PBKDF") || name.startsWith("GENERATE_") ||
                    name.startsWith("DERIVE_") || name.startsWith("BASE64_") || name.startsWith("HEX_") ||
                    name.startsWith("HMAC_") || name.startsWith("RSA_") || name.startsWith("IS_BASE64") || name.startsWith("IS_HEX")
            }
            CAT_ALL -> Functions.values().map { functionToken(it) }
            CAT_VARIABLES -> {
                val vars = ProjectManager.getInstance().currentProject?.userVariables?.map { it.name } ?: emptyList()
                if (vars.isEmpty()) listOf("No variables") else vars
            }
            CAT_USER_LISTS -> {
                val lists = ProjectManager.getInstance().currentProject?.userLists?.map { it.name } ?: emptyList()
                if (lists.isEmpty()) listOf("No lists") else lists
            }
            else -> emptyList()
        }

        for (key in keys) {
            val btn = TextView(this).apply {
                text = key
                setTextColor(Color.WHITE)
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_object_thumb_cube)
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = (48 * density).toInt()
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(margin, margin, margin, margin)
                }
                layoutParams = params
                setOnClickListener {
                    if (!key.startsWith("No ")) {
                        val insertText = if (category in setOf(CAT_MATH, CAT_STRINGS, CAT_LISTS, CAT_SENSORS,
                                CAT_OBJECT, CAT_DATA, CAT_3D, CAT_DEVICE, CAT_SECURITY, CAT_ALL)) {
                            functionInsertText(key)
                        } else key
                        insertAtCursor(insertText)
                    }
                }
            }
            keysContainer.addView(btn)
        }
    }

    private fun functionTokens(category: String, predicate: (String) -> Boolean): List<String> =
        Functions.values().filter { predicate(it.name) }.map { functionToken(it) }

    private fun functionToken(function: org.catrobat.catroid.formulaeditor.Functions): String =
        function.name.lowercase(Locale.US)

    private fun functionInsertText(token: String): String = when (token.uppercase(Locale.US)) {
        "PI", "TRUE", "FALSE", "CURRENT_STATE" -> token
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
