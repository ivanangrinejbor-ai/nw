package org.catrobat.catroid.content.actions

import android.os.Environment
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class DeleteFolderByPathAction : TemporalAction() {
    var scope: Scope? = null
    var path: Formula? = null
    var folderName: Formula? = null

    override fun update(percent: Float) {
        val pathStr = path?.interpretString(scope) ?: return
        val name = folderName?.interpretString(scope) ?: return
        if (pathStr.isBlank() || name.isBlank()) return

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val baseCanonical = downloadsDir.canonicalPath
            val parentDir = File(downloadsDir, pathStr).canonicalFile
            if (!parentDir.canonicalPath.startsWith(baseCanonical + File.separator) && parentDir.canonicalPath != baseCanonical) {
                Log.e("DeleteFolderByPathAction", "Path traversal detected: $pathStr")
                return
            }
            val folder = File(parentDir, name).canonicalFile
            if (!folder.canonicalPath.startsWith(baseCanonical + File.separator)) {
                Log.e("DeleteFolderByPathAction", "Path traversal detected: $name")
                return
            }
            if (folder.exists() && folder.isDirectory) {
                folder.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e("DeleteFolderByPathAction", "Failed to delete folder: $pathStr/$name", e)
        }
    }
}
