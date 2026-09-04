package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.utils.Utils
import java.io.File
import java.io.IOException
import java.util.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppendToFileAction : TemporalAction() {
    var scope: Scope? = null
    var fileName: Formula? = null
    var text: Formula? = null

    private var started = false
    private var job: Job = SupervisorJob()

    override fun update(percent: Float) {
        if (started) return
        if (fileName == null || text == null) return
        started = true
        val name = getFileName()
        val content = text?.interpretString(scope) ?: return
        CoroutineScope(Dispatchers.IO + job).launch {
            val result = appendToFile(name, content)
            withContext(Dispatchers.Main) {
                if (result) {
                    showSuccessMessage(name)
                } else {
                    Log.e(javaClass.simpleName, "Could not append to file.")
                }
            }
        }
    }

    override fun restart() {
        started = false
        job.cancel()
        job = SupervisorJob()
        super.restart()
    }

    fun appendToFile(name: String, content: String): Boolean {
        return try {
            val file = File(Constants.DOWNLOAD_DIRECTORY, name)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            file.appendText(content)
            true
        } catch (e: IOException) {
            Log.e(javaClass.simpleName, "Could not append to file: $name", e)
            false
        }
    }

    private fun getFileName(): String {
        var name = Utils.sanitizeFileName(fileName?.interpretString(scope))
        if (!name.contains(Regex(Constants.ANY_EXTENSION_REGEX))) {
            name += Constants.TEXT_FILE_EXTENSION
        }
        return name
    }

    private fun showSuccessMessage(name: String) {
        val context = CatroidApplication.getAppContext()
        val message = context.getString(R.string.brick_append_to_file_success, name)
        val params = ArrayList<Any>(listOf(message))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
    }
}
