package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

open class DownloadZippedLooksAction : TemporalAction() {
    var scope: Scope? = null
    var url: Formula? = null
    var lookNamePrefix: Formula? = null

    private var started = false

    override fun restart() {
        super.restart()
        started = false
    }

    override fun update(percent: Float) {
        if (scope == null) return
        if (started) return
        started = true
        val urlStr = url?.interpretString(scope) ?: return
        if (urlStr.isBlank()) return
        thread(name = "DownloadZippedLooks-io") {
            downloadAndExtract(urlStr)
        }
    }

    private fun downloadAndExtract(urlStr: String) {
        var connection: HttpURLConnection? = null
        val tempZip = File.createTempFile("zipped_looks", ".zip")
        try {
            val urlObj = URL(urlStr)
            if (urlObj.protocol !in arrayOf("http", "https")) {
                Log.e(TAG, "Invalid URL scheme: ${urlObj.protocol}")
                return
            }
            connection = urlObj.openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error: ${connection.responseCode}")
                return
            }
            connection.inputStream.use { inputStream ->
                FileOutputStream(tempZip).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
            extractLooks(tempZip)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
        } finally {
            connection?.disconnect()
            tempZip.delete()
        }
    }

    private fun extractLooks(zipFile: File) {
        val sprite = scope?.sprite ?: return
        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val prefix = lookNamePrefix?.interpretString(scope)?.takeIf { it.isNotBlank() } ?: ""

        val tempExtractDir = File.createTempFile("zipped_looks_dir", null)
        tempExtractDir.delete()
        tempExtractDir.mkdirs()

        val imageDirectory = File(scene.directory, Constants.IMAGE_DIRECTORY_NAME)
        if (!imageDirectory.exists()) {
            imageDirectory.mkdirs()
        }

        val addedLooks = mutableListOf<LookData>()
        try {
            val zis = ZipInputStream(FileInputStream(zipFile))
            try {
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                    val entryName = zipEntry.name
                    val canonicalBase = tempExtractDir.canonicalPath
                    val target = File(tempExtractDir, entryName)
                    val targetCanonical = target.canonicalPath
                    if (!targetCanonical.startsWith(canonicalBase + File.separator)) {
                        Log.e(TAG, "Zip-slip detected: $entryName")
                        zis.closeEntry()
                        zipEntry = zis.nextEntry
                        continue
                    }
                    if (!zipEntry.isDirectory) {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { fos ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        if (isImageFile(target)) {
                            val originalName = target.name.substringBeforeLast('.')
                            val baseName = if (prefix.isEmpty()) originalName else "$prefix$originalName"
                            val uniqueName = UniqueNameProvider().getUniqueNameInNameables(baseName, sprite.lookList)
                            val lookFile = StorageOperations.copyFileToDir(target, imageDirectory)
                            val lookData = LookData(uniqueName, lookFile)
                            lookData.collisionInformation.calculate()
                            sprite.lookList.add(lookData)
                            addedLooks.add(lookData)
                        }
                    }
                    zis.closeEntry()
                    zipEntry = zis.nextEntry
                }
            } finally {
                zis.close()
            }
            addedLooks.firstOrNull()?.let { sprite.look?.lookData = it }
            Log.i(TAG, "Added ${addedLooks.size} looks from zip")
        } catch (e: IOException) {
            Log.e(TAG, "Zip extraction failed", e)
        } finally {
            tempExtractDir.walkTopDown().forEach { it.delete() }
        }
    }

    private fun isImageFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in IMAGE_EXTENSIONS
    }

    companion object {
        private const val TAG = "DownloadZippedLooksAction"
        private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
    }
}