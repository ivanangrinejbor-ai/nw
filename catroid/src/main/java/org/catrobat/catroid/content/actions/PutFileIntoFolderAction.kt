package org.catrobat.catroid.content.actions

import android.util.Log
import org.catrobat.catroid.runtime.RuntimeServicesHolder
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class PutFileIntoFolderAction : TemporalAction() {
    var scope: Scope? = null
    var projectFileName: Formula? = null
    var folderName: Formula? = null

    private var started = false

    override fun restart() {
        super.restart()
        started = false
    }

    override fun update(percent: Float) {
        if (started) return
        started = true
        val project = scope?.project ?: return
        val sourceName = projectFileName?.interpretString(scope) ?: return
        val folder = folderName?.interpretString(scope) ?: return
        if (sourceName.isBlank() || folder.isBlank()) return

        // Offload blocking disk I/O off the render thread to avoid frame freeze / ANR.
        Thread({
            try {
                val sourceFile = project.getFile(sourceName)
                if (!sourceFile.exists() || sourceFile.isDirectory) return@Thread

                val downloadsDir = File(RuntimeServicesHolder.services.getDownloadsDir())
                val destDir = File(downloadsDir, folder).canonicalFile
                val baseCanonical = downloadsDir.canonicalPath
                if (!destDir.canonicalPath.startsWith(baseCanonical + File.separator) && destDir.canonicalPath != baseCanonical) {
                    Log.e("PutFileIntoFolderAction", "Path traversal detected: $folder")
                    return@Thread
                }
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }
                val destFile = File(destDir, sourceFile.name)
                sourceFile.copyTo(destFile, overwrite = true)
            } catch (e: Exception) {
                Log.e("PutFileIntoFolderAction", "Failed to copy file", e)
            }
        }, "PutFileIntoFolder-io").start()
    }
}
