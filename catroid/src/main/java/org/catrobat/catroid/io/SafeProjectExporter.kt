package org.catrobat.catroid.io

import android.content.Context
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.utils.lunoscript.baker.ProjectBaker
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SafeProjectExporter {
    fun export(context: Context, project: Project, destination: File) {
        val stagingDir = File(context.cacheDir, "safe_project_${System.nanoTime()}")
        val initFile = File(stagingDir, "init.bin")
        try {
            stagingDir.mkdirs()
            ProjectBaker(context).bakeToFile(project, initFile)
            copyProjectFiles(project, stagingDir)
            copyLooksAndSounds(project, stagingDir)
            destination.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(destination)).use { zip ->
                addDirectory(stagingDir, stagingDir, zip)
            }
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun copyProjectFiles(project: Project, stagingDir: File) {
        val source = File(project.directory, "files")
        if (source.exists() && source.isDirectory) {
            source.copyRecursively(File(stagingDir, "files"), overwrite = true)
        }
    }

    private fun copyLooksAndSounds(project: Project, stagingDir: File) {
        val imagesDir = File(stagingDir, "images")
        val soundsDir = File(stagingDir, "sounds")
        imagesDir.mkdirs()
        soundsDir.mkdirs()
        project.sceneList.forEach { scene ->
            scene.spriteList.forEach { sprite ->
                sprite.lookList.mapNotNull { it.file }.filter { it.isFile }.forEach { file ->
                    file.copyTo(File(imagesDir, file.name), overwrite = true)
                }
                sprite.soundList.mapNotNull { it.file }.filter { it.isFile }.forEach { file ->
                    file.copyTo(File(soundsDir, file.name), overwrite = true)
                }
            }
        }
    }

    private fun addDirectory(root: File, current: File, zip: ZipOutputStream) {
        current.listFiles()?.forEach { file ->
            val entryName = file.relativeTo(root).path.replace(File.separatorChar, '/')
            if (file.isDirectory) {
                zip.putNextEntry(ZipEntry("$entryName/"))
                zip.closeEntry()
                addDirectory(root, file, zip)
            } else {
                zip.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { it.copyTo(zip, 8192) }
                zip.closeEntry()
            }
        }
    }
}
