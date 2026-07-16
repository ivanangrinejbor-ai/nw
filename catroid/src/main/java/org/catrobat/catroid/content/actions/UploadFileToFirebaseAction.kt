package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.FireBaseStorageManager
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class UploadFileToFirebaseAction : TemporalAction() {
    var scope: Scope? = null
    var bucket: Formula? = null
    var path: Formula? = null
    var file: Formula? = null

    override fun update(percent: Float) {
        val bucketStr = bucket?.interpretString(scope) ?: ""
        val pathStr = path?.interpretString(scope) ?: ""
        val fileStr = file?.interpretString(scope) ?: ""
        if (bucketStr.isBlank() || pathStr.isBlank() || fileStr.isBlank()) return
        val localFile = resolveLocalFile(fileStr) ?: return
        FireBaseStorageManager.uploadFile(bucketStr, pathStr, localFile)
    }

    private fun resolveLocalFile(name: String): File? {
        val project = ProjectManager.getInstance().getCurrentProject() ?: return null
        val projectDir = try {
            project.directory
        } catch (e: Throwable) {
            null
        } ?: return null
        val absolute = File(name)
        if (absolute.isAbsolute) return absolute
        return File(projectDir, name)
    }
}
