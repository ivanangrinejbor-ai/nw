package org.catrobat.catroid.ui.formulaeditor

import android.content.Context
import org.catrobat.catroid.formulaeditor.InternFormula
import org.catrobat.catroid.formulaeditor.InternFormulaKeyboardAdapter
import org.catrobat.catroid.ui.recyclerview.fragment.CategoryListFragment

object FormulaEditor2Menus {

	fun categoriesFor(context: Context, tag: String): List<Pair<String, List<CategoryBrowser.CategoryItem>>> {
		val result = mutableListOf<Pair<String, List<CategoryBrowser.CategoryItem>>>()
		for (group in CategoryListFragment.getCategoryGroups(context, tag)) {
			val items = mutableListOf<CategoryBrowser.CategoryItem>()
			for (i in group.nameResIds.indices) {
				val resId = group.nameResIds[i]
				var param: String? = null
				group.paramResIds?.let { params ->
					if (i < params.size) {
						param = context.getString(params[i])
					}
				}
				val display = context.getString(resId) + (param ?: "")
				items.add(CategoryBrowser.CategoryItem(resId.toString(), display))
			}
			if (items.isNotEmpty()) {
				result.add((group.header ?: "") to items)
			}
		}
		return result
	}

	fun displayNameFor(context: Context, resId: Int, param: String? = null): String {
		return context.getString(resId) + (param ?: "")
	}

	fun insertionText(context: Context, resId: Int): String? {
		val tokens = InternFormulaKeyboardAdapter().createInternTokenListByResourceId(resId, "")
			?: return null
		if (tokens.isEmpty()) return null
		return try {
			val formula = InternFormula(tokens)
			formula.generateExternFormulaStringAndInternExternMapping(context)
			formula.externFormulaString
		} catch (_: Exception) {
			null
		}
	}
}