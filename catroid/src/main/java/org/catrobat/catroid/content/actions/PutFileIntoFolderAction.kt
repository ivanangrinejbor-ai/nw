package org.catrobat.catroid.content.actions

import android.os.Environment
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class PutFileIntoFolderAction : TemporalAction() {
    var scope: Scope? = null
    var projectFileName: Formula? = null
    var folderName: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val sourceName = projectFileName?.interpretString(scope) ?: return
        val folder = folderName?.interpretString(scope) ?: return
        if (sourceName.isBlank() || folder.isBlank()) return

        try {
            val sourceFile = project.getFile(sourceName)
            if (!sourceFile.exists() || sourceFile.isDirectory) return

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destDir = File(downloadsDir, folder)
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            val destFile = File(destDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)
        } catch (e: Exception) {
            Log.e("PutFileIntoFolderAction", "Failed to copy file", e)
        }
    }
}
