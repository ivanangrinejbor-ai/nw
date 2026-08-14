package org.catrobat.catroid.desktop.project

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.security.AnyTypePermission
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

data class ProjectMetadata(
    val title: String,
    val description: String = "",
    val programVersionName: String = "",
    val sceneCount: Int = 1
)

class DesktopProjectLoader(
    private val rootDir: File
) {
    private val xstream = XStream().apply {
        addPermission(AnyTypePermission.ANY)
        ignoreUnknownElements()
    }

    fun hasEmbeddedPayload(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(file.length() - 8)
                val magic = ByteArray(8)
                raf.readFully(magic)
                String(magic, Charsets.US_ASCII) == "NEOCAT01"
            }
        } catch (e: Exception) {
            false
        }
    }

    fun extractEmbeddedPayload(exeFile: File, destinationDir: File): Boolean {
        if (!exeFile.exists() || exeFile.length() < 16) return false
        return try {
            RandomAccessFile(exeFile, "r").use { raf ->
                val fileLength = exeFile.length()
                raf.seek(fileLength - 8)
                val magic = ByteArray(8)
                raf.readFully(magic)
                if (String(magic, Charsets.US_ASCII) != "NEOCAT01") return false

                raf.seek(fileLength - 16)
                val buffer = ByteArray(8)
                raf.readFully(buffer)
                val payloadLength = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).long

                val payloadOffset = fileLength - 16 - payloadLength
                if (payloadOffset < 0) return false

                raf.seek(payloadOffset)
                val payloadBytes = ByteArray(payloadLength.toInt())
                raf.readFully(payloadBytes)

                if (!destinationDir.exists()) destinationDir.mkdirs()
                ZipInputStream(ByteArrayInputStream(payloadBytes)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val destFile = File(destinationDir, entry.name)
                        if (entry.isDirectory) {
                            destFile.mkdirs()
                        } else {
                            destFile.parentFile?.mkdirs()
                            destFile.outputStream().use { zipIn.copyTo(it) }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun extractIfCatrobatBundle(file: File, destinationDir: File): File {
        if (!file.name.endsWith(".catrobat", ignoreCase = true)) {
            return file
        }

        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        ZipInputStream(FileInputStream(file)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val filePath = File(destinationDir, entry.name)
                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    filePath.parentFile?.mkdirs()
                    filePath.outputStream().use { zipIn.copyTo(it) }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        return destinationDir
    }

    fun findCodeXml(projectDir: File): File? {
        val codeXml = File(projectDir, "code.xml")
        if (codeXml.exists()) return codeXml

        val nested = projectDir.listFiles()?.firstOrNull { File(it, "code.xml").exists() }
        return nested?.let { File(it, "code.xml") }
    }

    fun getImagesDir(projectDir: File): File {
        val dir = File(projectDir, "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSoundsDir(projectDir: File): File {
        val dir = File(projectDir, "sounds")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
