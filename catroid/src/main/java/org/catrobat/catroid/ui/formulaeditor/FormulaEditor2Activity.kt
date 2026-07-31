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
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable

class FormulaEditor2Activity : AppCompatActivity() {

    companion object {
        const val EXTRA_FORMULA_STRING = "extra_formula_string"
        const val EXTRA_RESULT_FORMULA_STRING = "extra_result_formula_string"
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

        val categories = listOf("Основные", "Функции", "Переменные", "Списки")
        for (cat in categories) {
            val btn = TextView(this).apply {
                text = cat
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
                val returnIntent = Intent().apply {
                    putExtra(EXTRA_RESULT_FORMULA_STRING, result)
                }
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
        }
        root.addView(doneBtn)

        setContentView(root)

        loadCategoryKeys("Основные")
    }

    private fun loadCategoryKeys(category: String) {
        keysContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val margin = (4 * density).toInt()

        val keys = when (category) {
            "Основные" -> listOf(
                "7", "8", "9", "+", "(",
                "4", "5", "6", "-", ")",
                "1", "2", "3", "*", "=",
                "0", ".", ",", "/", ">", "<"
            )
            "Функции" -> listOf(
                "sin", "cos", "tan", "abs", "sqrt",
                "round", "random", "min", "max", "log",
                "AND", "OR", "NOT", "true", "false"
            )
            "Переменные" -> {
                val project = ProjectManager.getInstance().currentProject
                val vars = project?.userVariables?.map { it.name } ?: emptyList()
                if (vars.isEmpty()) listOf("Нет переменных") else vars
            }
            "Списки" -> {
                val project = ProjectManager.getInstance().currentProject
                val lists = project?.userLists?.map { it.name } ?: emptyList()
                if (lists.isEmpty()) listOf("Нет списков") else lists
            }
            else -> emptyList()
        }

        for (k in keys) {
            val btn = TextView(this).apply {
                text = k
                setTextColor(Color.WHITE)
                textSize = 15f
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
                    if (k != "Нет переменных" && k != "Нет списков") {
                        val insertText = if (category == "Функции") "$k(" else k
                        val cursor = formulaInput.selectionStart
                        formulaInput.text.insert(cursor, insertText)
                    }
                }
            }
            keysContainer.addView(btn)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
