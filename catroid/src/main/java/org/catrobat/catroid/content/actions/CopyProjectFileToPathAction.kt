package org.catrobat.catroid.content.actions

import android.util.Log
import org.catrobat.catroid.runtime.RuntimeServicesHolder
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

            val downloadsDir = File(RuntimeServicesHolder.services.getDownloadsDir())
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
