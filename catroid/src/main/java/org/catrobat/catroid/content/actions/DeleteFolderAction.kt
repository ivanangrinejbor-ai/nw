package org.catrobat.catroid.content.actions

import android.os.Environment
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class DeleteFolderAction : TemporalAction() {
    var scope: Scope? = null
    var folderName: Formula? = null

    override fun update(percent: Float) {
        val name = folderName?.interpretString(scope) ?: return
        if (name.isBlank()) return

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val folder = File(downloadsDir, name)
            if (folder.exists() && folder.isDirectory) {
                folder.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e("DeleteFolderAction", "Failed to delete folder: $name", e)
        }
    }
}
