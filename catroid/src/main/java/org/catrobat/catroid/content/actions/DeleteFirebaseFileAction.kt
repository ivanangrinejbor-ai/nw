package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.FireBaseStorageManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class DeleteFirebaseFileAction : TemporalAction() {
    var scope: Scope? = null
    var bucket: Formula? = null
    var path: Formula? = null

    override fun update(percent: Float) {
        val bucketStr = bucket?.interpretString(scope) ?: ""
        val pathStr = path?.interpretString(scope) ?: ""
        if (bucketStr.isBlank() || pathStr.isBlank()) return
        FireBaseStorageManager.deleteFile(bucketStr, pathStr)
    }
}
