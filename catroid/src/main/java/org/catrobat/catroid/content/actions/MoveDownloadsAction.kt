package org.catrobat.catroid.content.actions

import android.content.ContentUris
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

class MoveDownloadsAction() : TemporalAction() {
    var scope: Scope? = null
    var fileName: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val context = CatroidApplication.getAppContext() ?: return

        val fileNameStr: String
        try {
            fileNameStr = fileName?.interpretString(scope) ?: ""
        } catch (e: InterpretationException) {
            Log.e("MoveDownloadsAction", "Formula interpretation error", e)
            return
        }

        val name = project.checkExtension(fileNameStr, "txt")
        if (name.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyFileFromDownloads(context, name, project.filesDir)
        } else {
            copyFileFromDownloadsLegacy(name, project.filesDir)
        }
    }

    private fun copyFileFromDownloadsLegacy(fileName: String, destinationDir: File) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val sourceFile = File(downloadsDir, fileName)
        val destinationFile = File(destinationDir, fileName).canonicalFile
        if (!destinationFile.canonicalPath.startsWith(destinationDir.canonicalPath + File.separator)) {
            Log.e("MoveDownloadsAction", "Path traversal detected in destination: $fileName")
            return
        }
        if (!sourceFile.exists()) {
            Log.w("MoveDownloadsAction", "File '$fileName' not found in Downloads.")
            return
        }
        try {
            sourceFile.copyTo(destinationFile, overwrite = true)
        } catch (e: IOException) {
            Log.e("MoveDownloadsAction", "Error copying file from Downloads", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyFileFromDownloads(context: android.content.Context, fileName: String, destinationDir: File) {
        val resolver = context.contentResolver ?: return

        val destinationFile = File(destinationDir, fileName).canonicalFile
        if (!destinationFile.canonicalPath.startsWith(destinationDir.canonicalPath + File.separator)) {
            Log.e("MoveDownloadsAction", "Path traversal detected in destination: $fileName")
            return
        }

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        val sortOrder = "${MediaStore.Downloads.DATE_ADDED} DESC"

        try {
            resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)

                    resolver.openInputStream(contentUri)?.use { inputStream ->
                        destinationFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } else {
                    Log.w("MoveDownloadsAction", "File '$fileName' not found in Downloads.")
                }
            }
        } catch (e: IOException) {
            Log.e("MoveDownloadsAction", "Error copying file from Downloads: ${e.message}")
        }
    }
}