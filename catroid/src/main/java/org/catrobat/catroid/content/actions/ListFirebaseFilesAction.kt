package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.FireBaseStorageManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable

class ListFirebaseFilesAction : TemporalAction() {
    var scope: Scope? = null
    var bucket: Formula? = null
    var prefix: Formula? = null
    var variable: UserVariable? = null

    override fun update(percent: Float) {
        val bucketStr = bucket?.interpretString(scope) ?: ""
        val prefixStr = prefix?.interpretString(scope) ?: ""
        if (bucketStr.isBlank()) return
        val files = FireBaseStorageManager.listFiles(bucketStr, prefixStr)
        variable?.value = files.joinToString(", ")
    }
}
