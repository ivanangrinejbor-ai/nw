package org.catrobat.catroid.content.actions

import android.util.Log
import org.catrobat.catroid.runtime.RuntimeServicesHolder
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class CreateFolderByPathAction : TemporalAction() {
    var scope: Scope? = null
    var path: Formula? = null
    var folderName: Formula? = null

    override fun update(percent: Float) {
        val pathStr = path?.interpretString(scope) ?: return
        val name = folderName?.interpretString(scope) ?: return
        if (pathStr.isBlank() || name.isBlank()) return

        try {
            val downloadsDir = File(RuntimeServicesHolder.services.getDownloadsDir())
            val baseCanonical = downloadsDir.canonicalPath
            val parentDir = File(downloadsDir, pathStr).canonicalFile
            if (!parentDir.canonicalPath.startsWith(baseCanonical + File.separator) && parentDir.canonicalPath != baseCanonical) {
                Log.e("CreateFolderByPathAction", "Path traversal detected: $pathStr")
                return
            }
            val folder = File(parentDir, name).canonicalFile
            if (!folder.canonicalPath.startsWith(baseCanonical + File.separator)) {
                Log.e("CreateFolderByPathAction", "Path traversal detected: $name")
                return
            }
            if (!folder.exists()) {
                folder.mkdirs()
            }
        } catch (e: Exception) {
            Log.e("CreateFolderByPathAction", "Failed to create folder: $pathStr/$name", e)
        }
    }
}
