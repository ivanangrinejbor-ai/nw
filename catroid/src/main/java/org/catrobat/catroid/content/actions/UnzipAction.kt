package org.catrobat.catroid.content.actions

import android.os.Environment
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

open class UnzipAction : TemporalAction() {
    var scope: Scope? = null
    var name: Formula? = null

    private var started = false

    override fun restart() {
        super.restart()
        started = false
    }

    override fun update(percent: Float) {
        if (started) return
        started = true
        val fileName: String
        try {
            fileName = name?.interpretString(scope) ?: ""
        } catch (e: InterpretationException) {
            Log.e("UnzipAction", "Formula interpretation error", e)
            return
        }

        val zipName = sanitizeZipName(fileName)
        if (zipName.isEmpty()) return

        // Offload blocking disk I/O off the render thread to avoid frame freeze / ANR.
        Thread({
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, zipName)
            if (file.exists()) {
                unzip(file.absolutePath, dir.absolutePath)
            } else {
                Log.e("UnzipAction", "File does not exist: $zipName")
            }
        }, "UnzipAction-io").start()
    }

    private fun sanitizeZipName(input: String): String {
        var name = input
        val lastDotIndex = name.lastIndexOf('.')
        if (lastDotIndex < 0) {
            name += ".zip"
        }
        return name
    }

    private fun unzip(zipFilePath: String, outputDir: String) {
        val buffer = ByteArray(1024)
        try {
            val zis = ZipInputStream(FileInputStream(zipFilePath))
            try {
                var zipEntry: ZipEntry? = zis.nextEntry
                while (zipEntry != null) {
                    val newFile = File(outputDir, zipEntry.name)
                    val outputCanonical = File(outputDir).canonicalPath
                    val newFileCanonical = newFile.canonicalPath
                    if (!newFileCanonical.startsWith(outputCanonical + File.separator)) {
                        Log.e("UnzipAction", "Zip-slip detected: ${zipEntry.name}")
                        zis.closeEntry()
                        zipEntry = zis.nextEntry
                        continue
                    }
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    zipEntry = zis.nextEntry
                }
            } finally {
                zis.close()
            }
        } catch (e: IOException) {
            Log.e("UnzipAction", "Error unzipping file", e)
        }
    }
}