package org.catrobat.catroid.ui.formulaeditor

import android.app.AlertDialog
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.SearchView
import android.widget.TextView

object CategoryBrowser {

    private const val COLOR_HEADER = 0xFF00D4FF.toInt()
    private const val COLOR_ITEM = 0xFFE2E8F0.toInt()
    private const val COLOR_PARAM = 0xFF94A3B8.toInt()

    data class CategoryItem(
        val name: String,
        val displayName: String,
        val params: String? = null,
        val isHeader: Boolean = false
    )

    fun show(
        context: Context,
        title: String,
        categories: List<Pair<String, List<CategoryItem>>>,
        onItemSelected: (String, String?) -> Unit
    ) {
        val flatList = mutableListOf<CategoryItem>()
        for ((header, items) in categories) {
            flatList.add(CategoryItem(header, header, isHeader = true))
            flatList.addAll(items)
        }

        val adapter = CategoryAdapter(context, flatList)

        val searchView = SearchView(context).apply {
            queryHint = "Search..."
            isIconified = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter.filter(newText)
                    return true
                }
            })
        }

        val builder = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(title)
            .setView(searchView)

        val dialog = builder.create()

        val listView = android.widget.ListView(context)
        listView.adapter = adapter
        listView.divider = null
        listView.dividerHeight = 0
        listView.setPadding(0, 0, 0, 0)

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position)
            if (item != null && !item.isHeader) {
                onItemSelected(item.name, item.params)
                dialog.dismiss()
            }
        }

        dialog.setView(searchView)
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(searchView)
            addView(listView)
        }
        dialog.setView(container)
        dialog.show()

        searchView.post {
            searchView.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private class CategoryAdapter(
        context: Context,
        private val allItems: List<CategoryItem>
    ) : ArrayAdapter<CategoryItem>(context, 0, allItems), Filterable {

        private var filteredItems: List<CategoryItem> = allItems.toList()

        override fun getCount(): Int = filteredItems.size
        override fun getItem(position: Int): CategoryItem? = filteredItems.getOrNull(position)

        override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
            val item = filteredItems[position]
            val textView = (convertView as? TextView) ?: TextView(context).apply {
                setPadding(48, 24, 48, 24)
                textSize = 16f
            }

            if (item.isHeader) {
                val ssb = SpannableStringBuilder(item.displayName)
                ssb.setSpan(ForegroundColorSpan(COLOR_HEADER), 0, item.displayName.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                textView.text = ssb
                textView.setTextSize(14f)
                textView.setPadding(48, 32, 48, 12)
                textView.isClickable = false
            } else {
                val ssb = SpannableStringBuilder(item.displayName)
                ssb.setSpan(ForegroundColorSpan(COLOR_ITEM), 0, item.displayName.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (item.params != null) {
                    val paramStr = "  ${item.params}"
                    ssb.append(paramStr)
                    ssb.setSpan(ForegroundColorSpan(COLOR_PARAM),
                        item.displayName.length, ssb.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                textView.text = ssb
                textView.setTextSize(16f)
                textView.setPadding(64, 20, 48, 20)
                textView.isClickable = true
            }

            return textView
        }

        override fun getFilter(): Filter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""
                val filtered = if (query.isEmpty()) {
                    allItems
                } else {
                    val result = mutableListOf<CategoryItem>()
                    var lastHeader: CategoryItem? = null
                    for (item in allItems) {
                        if (item.isHeader) {
                            lastHeader = item
                        } else if (item.name.lowercase().contains(query) ||
                            item.displayName.lowercase().contains(query)) {
                            if (lastHeader != null && lastHeader !in result) {
                                result.add(lastHeader)
                                lastHeader = null
                            }
                            result.add(item)
                        }
                    }
                    result
                }
                return FilterResults().apply { values = filtered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<CategoryItem> ?: allItems
                notifyDataSetChanged()
            }
        }
    }
}
