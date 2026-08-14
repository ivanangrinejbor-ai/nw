package org.catrobat.catroid.desktop.export

import android.content.Context
import android.os.Environment
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.ProjectManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DesktopExeBuilder {

    private val MAGIC = "NEOCAT01".toByteArray(Charsets.US_ASCII)

    fun buildStandaloneExe(context: Context, project: Project, outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()

            val projectZipBytes = createProjectZip(project)
            val templateStream = getTemplateExeStream(context) ?: return false

            FileOutputStream(outputFile).use { outStream ->
                templateStream.use { inStream ->
                    inStream.copyTo(outStream)
                }

                val payloadLength = projectZipBytes.size.toLong()
                outStream.write(projectZipBytes)

                val lenBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(payloadLength).array()
                outStream.write(lenBytes)
                outStream.write(MAGIC)
                outStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDefaultExportFile(project: Project): File {
        val sanitizedName = project.name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val exportDir = File(downloadsDir, "NeoCatroid")
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, "$sanitizedName.exe")
    }

    private fun getTemplateExeStream(context: Context): InputStream? {
        val assetManager = context.assets
        return try {
            assetManager.open("template_desktop.exe")
        } catch (e: Exception) {
            try {
                val winZipStream = assetManager.open("template_win.zip")
                val zipIn = ZipInputStream(winZipStream)
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".exe", ignoreCase = true)) {
                        val buffer = ByteArrayOutputStream()
                        zipIn.copyTo(buffer)
                        zipIn.close()
                        return buffer.toByteArray().inputStream()
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                null
            } catch (ex: Exception) {
                null
            }
        }
    }

    private fun createProjectZip(project: Project): ByteArray {
        val byteOut = ByteArrayOutputStream()
        val projectDir = project.directory ?: File(contextProjectDir(project.name))

        ZipOutputStream(byteOut).use { zipOut ->
            if (projectDir.exists()) {
                addFolderToZip(projectDir, projectDir, zipOut)
            }
        }
        return byteOut.toByteArray()
    }

    private fun contextProjectDir(projectName: String): String {
        val curr = ProjectManager.getInstance().currentProject
        if (curr != null && curr.directory != null) {
            return curr.directory.absolutePath
        }
        return ""
    }

    private fun addFolderToZip(rootDir: File, currentFolder: File, zipOut: ZipOutputStream) {
        val files = currentFolder.listFiles() ?: return
        for (file in files) {
            val relativePath = file.relativeTo(rootDir).path.replace('\\', '/')
            if (file.isDirectory) {
                zipOut.putNextEntry(ZipEntry("$relativePath/"))
                zipOut.closeEntry()
                addFolderToZip(rootDir, file, zipOut)
            } else {
                zipOut.putNextEntry(ZipEntry(relativePath))
                FileInputStream(file).use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }
}
