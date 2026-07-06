package org.catrobat.catroid.content.actions

import android.util.Log
import android.widget.Toast
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import java.io.IOException

class CopyProjectFileAction : TemporalAction() {
    var scope: Scope? = null
    var sourceFileName: Formula? = null
    var newFileName: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val context = CatroidApplication.getAppContext() ?: return

        val sourceName: String
        val newName: String
        try {
            sourceName = sourceFileName?.interpretString(scope) ?: ""
            newName = newFileName?.interpretString(scope) ?: ""
        } catch (e: InterpretationException) {
            Log.e("CopyProjectFileAction", "Formula interpretation error", e)
            return
        }

        if (sourceName.isEmpty() || newName.isEmpty()) {
            return
        }

        try {
            val sourceFile = project.getFile(sourceName)
            if (!sourceFile.exists() || sourceFile.isDirectory) {
                return
            }

            val newFile = project.getFile(newName)
            sourceFile.copyTo(newFile, overwrite = true)
        } catch (e: IOException) {
            Log.e("CopyProjectFileAction", "Error copying file '$sourceName' to '$newName'", e)
        }
    }
}