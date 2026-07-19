package org.catrobat.catroid.content.actions

import android.os.Environment
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class ZipAction : TemporalAction() {
    var scope: Scope? = null
    var name: Formula? = null
    var files: Formula? = null

    override fun update(percent: Float) {
        val fileName: String
        val filesListStr: String
        try {
            fileName = name?.interpretString(scope) ?: ""
            filesListStr = files?.interpretString(scope) ?: ""
        } catch (e: InterpretationException) {
            Log.e("ZipAction", "Formula interpretation error", e)
            return
        }

        val zipName = sanitizeZipName(fileName)
        if (zipName.isEmpty()) return

        val paths = getFilePaths(filesListStr)
        if (paths.isEmpty()) return

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val baseCanonical = dir.canonicalPath
        val file = File(dir, zipName).canonicalFile
        if (!file.canonicalPath.startsWith(baseCanonical + File.separator)) {
            Log.e("ZipAction", "Path traversal detected: $zipName")
            return
        }
        zipFiles(paths, file.absolutePath)
    }

    private fun sanitizeZipName(input: String): String {
        var name = input
        val lastDotIndex = name.lastIndexOf('.')
        if (lastDotIndex < 0) {
            name += ".zip"
        }
        return name
    }

    private fun getFilePaths(input: String): List<String> {
        val delimiter = ","
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val baseCanonical = dir.canonicalPath

        return input.split(delimiter).map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { fileName ->
                val f = File(dir, fileName).canonicalFile
                if (!f.canonicalPath.startsWith(baseCanonical + File.separator)) {
                    Log.e("ZipAction", "Path traversal detected in file list: $fileName")
                    null
                } else {
                    f.absolutePath
                }
            }
    }

    private fun zipFiles(fileList: List<String>, zipFilePath: String) {
        try {
            ZipOutputStream(FileOutputStream(zipFilePath)).use { zos ->
                for (filePath in fileList) {
                    val file = File(filePath)
                    if (!file.exists() || file.isDirectory) continue
                    val zipEntry = ZipEntry(file.name)
                    zos.putNextEntry(zipEntry)
                    file.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        } catch (e: IOException) {
            Log.e("ZipAction", "Error creating zip file", e)
        }
    }
}