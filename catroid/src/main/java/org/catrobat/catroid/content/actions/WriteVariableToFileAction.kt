package org.catrobat.catroid.content.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
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

class WriteVariableToFileAction : TemporalAction(), IntentListener {
    var scope: Scope? = null
    var formula: Formula? = null
    var userVariable: UserVariable? = null

    private var started = false
    private var job: Job = SupervisorJob()

    override fun update(percent: Float) {
        if (started) return
        if (userVariable == null || formula == null) return
        started = true
        createAndWriteToFile()
    }

    override fun restart() {
        started = false
        job.cancel()
        job = SupervisorJob()
        super.restart()
    }

    @VisibleForTesting
    fun createAndWriteToFile() {
        val fileName = getFileName()
        CoroutineScope(Dispatchers.IO + job).launch {
            createFile(fileName)?.let { file ->
                val content = userVariable?.value.toString()
                val result = writeToFile(file, content)
                withContext(Dispatchers.Main) {
                    if (result) {
                        showSuccessMessage(file.name)
                    } else {
                        Log.e(javaClass.simpleName, "Could not write variable value to storage.")
                    }
                }
            }
        }
    }

    @VisibleForTesting
    fun createFile(fileName: String): File? {
        return try {
            val file = File(Constants.DOWNLOAD_DIRECTORY, fileName)
            if (file.exists() || file.createNewFile()) file else null
        } catch (e: IOException) {
            Log.e(javaClass.simpleName, "Could not create file: $fileName", e)
            null
        }
    }

    @VisibleForTesting
    fun writeToFile(file: File, content: String): Boolean {
        return try {
            file.writeText(content)
            true
        } catch (e: IOException) {
            Log.e(javaClass.simpleName, "Could not write variable value to storage.")
            false
        }
    }

    private fun getFileName(): String {
        var fileName = Utils.sanitizeFileName(formula?.interpretString(scope))
        if (!fileName.contains(Regex(Constants.ANY_EXTENSION_REGEX))) {
            fileName += Constants.TEXT_FILE_EXTENSION
        }
        return fileName
    }

    private fun showSuccessMessage(fileName: String) {
        val context = CatroidApplication.getAppContext()
        val message = context.getString(R.string.brick_write_variable_to_file_success, fileName)
        val params = ArrayList<Any>(listOf(message))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
    }

    private fun writeToUri(uri: Uri, content: String) {
        try {
            val context: Context = CatroidApplication.getAppContext()
            val contentResolver = context.contentResolver
            contentResolver.openOutputStream(uri).use {
                it?.write(content.toByteArray())
            }
            showSuccessMessage(getFileName())
        } catch (e: IOException) {
            Log.e(javaClass.simpleName, "Could not write variable value to storage.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getTargetIntent(): Intent {
        val fileName = getFileName()
        val context = StageActivity.activeStageActivity.get()?.context
        val title = context?.getString(R.string.brick_write_variable_to_file_top) ?: ""
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, fileName)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
        }
        return Intent.createChooser(intent, title)
    }

    override fun onIntentResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                val content: String = when (val value = userVariable?.value ?: 0) {
                    is Double -> value.toBigDecimal().toPlainString()
                    else -> value.toString()
                }
                writeToUri(it, content)
            }
            return true
        }
        return false
    }
}
