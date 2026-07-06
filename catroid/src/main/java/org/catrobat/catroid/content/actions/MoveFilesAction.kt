package org.catrobat.catroid.content.actions

import android.content.Context
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import java.io.File
import java.io.IOException

class MoveFilesAction() : TemporalAction() {
    var scope: Scope? = null
    var fileName: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val context = CatroidApplication.getAppContext() ?: return

        val fileNameStr: String
        try {
            fileNameStr = fileName?.interpretString(scope) ?: ""
        } catch (e: InterpretationException) {
            Log.e("MoveFilesAction", "Formula interpretation error", e)
            return
        }

        val name = project.checkExtension(fileNameStr, "txt")
        if (name.isEmpty()) return

        val file = project.getFile(name)
        if (!file.exists() || file.isDirectory) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyFileToDownloads(context, file)
        } else {
            copyFileToLegacy(file)
        }
    }

    private fun copyFileToLegacy(file: File) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val destFile = File(downloadsDir, file.name)
        try {
            file.copyTo(destFile, overwrite = true)
        } catch (e: IOException) {
            Log.e("MoveFilesAction", "Error copying file to Downloads", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyFileToDownloads(context: Context, sourceFile: File) {
        val resolver = context.contentResolver ?: return

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        try {
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Failed to create new MediaStore record.")

            resolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            Log.e("MoveFilesAction", "Error copying file to Downloads: ${e.message}")
        }
    }
}