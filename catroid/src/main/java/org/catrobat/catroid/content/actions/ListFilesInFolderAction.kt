package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.utils.Utils
import java.io.File

class ListFilesInFolderAction : TemporalAction() {
    var scope: Scope? = null
    var folder: Formula? = null
    var userList: UserList? = null

    private var started = false

    override fun restart() {
        started = false
        super.restart()
    }

    override fun update(percent: Float) {
        if (started) return
        started = true
        val list = userList ?: return
        list.reset()
        for (name in listFileNames()) {
            list.addListItem(name)
        }
    }

    fun listFileNames(): List<String> {
        val folderName = Utils.sanitizeFileName(folder?.interpretString(scope))
        if (folderName.isEmpty()) return emptyList()
        return listFileNamesIn(File(Constants.DOWNLOAD_DIRECTORY, folderName))
    }

    fun listFileNamesIn(dir: File): List<String> {
        val files = try {
            dir.listFiles { file -> file.isFile }
        } catch (e: SecurityException) {
            Log.e(javaClass.simpleName, "Cannot list folder: ${dir.name}", e)
            null
        } ?: return emptyList()
        return files.map { it.name }.sorted()
    }
}
