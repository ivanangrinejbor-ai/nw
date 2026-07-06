package org.catrobat.catroid.content.actions

import android.os.Environment
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class CopyProjectFileToPathAction : TemporalAction() {
    var scope: Scope? = null
    var projectFileName: Formula? = null
    var path: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val sourceName = projectFileName?.interpretString(scope) ?: return
        val pathStr = path?.interpretString(scope) ?: return
        if (sourceName.isBlank() || pathStr.isBlank()) return

        try {
            val sourceFile = project.getFile(sourceName)
            if (!sourceFile.exists() || sourceFile.isDirectory) return

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val baseCanonical = downloadsDir.canonicalPath
            val destDir = File(downloadsDir, pathStr).canonicalFile
            if (!destDir.canonicalPath.startsWith(baseCanonical + File.separator) && destDir.canonicalPath != baseCanonical) {
                Log.e("CopyProjectFileToPathAction", "Path traversal detected: $pathStr")
                return
            }
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            val destFile = File(destDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)
        } catch (e: Exception) {
            Log.e("CopyProjectFileToPathAction", "Failed to copy file", e)
        }
    }
}
