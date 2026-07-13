package org.catrobat.catroid.content.actions

import android.util.Log
import org.catrobat.catroid.runtime.RuntimeServicesHolder
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class PutFileIntoPathAction : TemporalAction() {
    var scope: Scope? = null
    var projectFileName: Formula? = null
    var destPath: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val sourceName = projectFileName?.interpretString(scope) ?: return
        val pathStr = destPath?.interpretString(scope) ?: return
        if (sourceName.isBlank() || pathStr.isBlank()) return

        try {
            val sourceFile = project.getFile(sourceName)
            if (!sourceFile.exists() || sourceFile.isDirectory) return

            val downloadsDir = File(RuntimeServicesHolder.services.getDownloadsDir())
            val baseCanonical = downloadsDir.canonicalPath
            val destFile = File(pathStr).canonicalFile
            if (!destFile.canonicalPath.startsWith(baseCanonical + File.separator) && destFile.canonicalPath != baseCanonical) {
                Log.e("PutFileIntoPathAction", "Path traversal detected: $pathStr")
                return
            }

            if (destFile.isDirectory) {
                val fileInDir = File(destFile, sourceFile.name)
                sourceFile.copyTo(fileInDir, overwrite = true)
            } else {
                destFile.parentFile?.mkdirs()
                sourceFile.copyTo(destFile, overwrite = true)
            }
        } catch (e: Exception) {
            Log.e("PutFileIntoPathAction", "Failed to copy file", e)
        }
    }
}
