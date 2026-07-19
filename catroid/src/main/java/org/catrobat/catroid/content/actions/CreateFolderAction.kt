package org.catrobat.catroid.content.actions

import android.util.Log
import org.catrobat.catroid.runtime.RuntimeServicesHolder
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class CreateFolderAction : TemporalAction() {
    var scope: Scope? = null
    var folderName: Formula? = null

    override fun update(percent: Float) {
        val name = folderName?.interpretString(scope) ?: return
        if (name.isBlank()) return

        try {
            val downloadsDir = File(RuntimeServicesHolder.services.getDownloadsDir())
            val baseCanonical = downloadsDir.canonicalPath
            val folder = File(downloadsDir, name).canonicalFile
            if (!folder.canonicalPath.startsWith(baseCanonical + File.separator) && folder.canonicalPath != baseCanonical) {
                Log.e("CreateFolderAction", "Path traversal detected: $name")
                return
            }
            if (!folder.exists()) {
                folder.mkdirs()
            }
        } catch (e: Exception) {
            Log.e("CreateFolderAction", "Failed to create folder: $name", e)
        }
    }
}
