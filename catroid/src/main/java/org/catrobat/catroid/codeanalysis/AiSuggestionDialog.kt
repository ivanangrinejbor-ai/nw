package org.catrobat.catroid.codeanalysis

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.catrobat.catroid.R

object AiSuggestionDialog {
    fun show(context: Context) {
        val analysis = AiProjectAssistant.getSuggestionsForAllScripts()
        if (analysis.isEmpty()) {
            Toast.makeText(context, R.string.ai_assist_no_suggestions, Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        for (entry in analysis) {
            val scriptType = entry.first.javaClass.simpleName
            val header = TextView(context).apply {
                text = "$scriptType"
                textSize = 16f
                setPadding(0, 16, 0, 8)
            }
            layout.addView(header)

            for (s in entry.second.take(3)) {
                val item = TextView(context).apply {
                    text = "  + ${s.brickType}  (${s.reason})"
                    textSize = 14f
                    setPadding(16, 4, 0, 4)
                }
                layout.addView(item)
            }
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.ai_assist)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
