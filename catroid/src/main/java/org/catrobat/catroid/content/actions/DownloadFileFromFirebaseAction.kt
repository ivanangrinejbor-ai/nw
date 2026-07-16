package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.FireBaseStorageManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import java.io.File

class DownloadFileFromFirebaseAction : TemporalAction() {
    var scope: Scope? = null
    var bucket: Formula? = null
    var path: Formula? = null
    var destFile: Formula? = null
    var variable: UserVariable? = null

    override fun update(percent: Float) {
        val bucketStr = bucket?.interpretString(scope) ?: ""
        val pathStr = path?.interpretString(scope) ?: ""
        val destStr = destFile?.interpretString(scope) ?: ""
        if (bucketStr.isBlank() || pathStr.isBlank() || destStr.isBlank()) return
        val dest = File(destStr)
        val success = FireBaseStorageManager.downloadFile(bucketStr, pathStr, dest)
        variable?.value = if (success) dest.absolutePath else "ERROR"
    }
}
