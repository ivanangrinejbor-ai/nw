package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException

class DeleteFilesAction() : TemporalAction() {
    var scope: Scope? = null
    var fileName: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return

        val fileNameStr: String
        try {
            fileNameStr = fileName?.interpretString(scope) ?: ""
        } catch (e: InterpretationException) {
            Log.e("DeleteFilesAction", "Formula interpretation error", e)
            return
        }

        if (fileNameStr.isEmpty()) return

        val name = project.checkExtension(fileNameStr, "txt")
        if (name.isEmpty()) return

        try {
            project.deleteFile(name)
        } catch (e: SecurityException) {
            Log.e("DeleteFilesAction", "Security error deleting file", e)
        }
    }
}