package org.catrobat.catroid.io.asynctask

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*

import org.catrobat.catroid.utils.notifications.NotificationData
import org.catrobat.catroid.io.ZipArchiver
import org.catrobat.catroid.utils.notifications.StatusBarNotificationManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.ProjectCrypto

import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ProjectExportTask(
    private val projectDir: File,
    private val projectDestination: Uri,
    private val notificationData: NotificationData,
    private val context: Context,
    private val password: String? = null
) {

    enum class ExportPhase {
        ZIPPING, COPYING, VERIFYING
    }

    interface ProjectExportProgressListener {
        fun onPhaseChanged(phase: ExportPhase)
        fun onProgress(bytesDone: Long, bytesTotal: Long)
    }

    private var finishedExportingCallback: ProjectExportCallback? = null
    private var progressListener: ProjectExportProgressListener? = null

    fun setProgressListener(listener: ProjectExportProgressListener?) {
        progressListener = listener
    }

    suspend fun exportProjectToExternalStorage() {
        deleteUndoFile()

        val projectFileName = projectDir.name + Constants.ZIP_EXTENSION
        val cacheFile = File(Constants.CACHE_DIRECTORY, projectFileName)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        try {
            progressListener?.onPhaseChanged(ExportPhase.ZIPPING)
            ZipArchiver().zipDedup(cacheFile, projectDir.listFiles() ?: emptyArray())

            if (password != null && password.isNotEmpty()) {
                val encryptedFile = File(Constants.CACHE_DIRECTORY, "$projectFileName.enc")
                ProjectCrypto.encrypt(cacheFile, encryptedFile, password)
                cacheFile.delete()
                encryptedFile.renameTo(cacheFile)
            }

            progressListener?.onPhaseChanged(ExportPhase.COPYING)
            val contentResolver = context.contentResolver
            val sourceHash = copyFileToUriWithProgress(contentResolver, projectDestination, cacheFile)

            progressListener?.onPhaseChanged(ExportPhase.VERIFYING)
            if (!verifyUriContent(contentResolver, projectDestination, sourceHash, cacheFile.length())) {
                throw IOException("SHA-256 verification failed for exported file")
            }
            updateNotification(context)
            finishedExportingCallback?.onProjectExportFinished()
        } catch (e: IOException) {
            Log.e(TAG, "Cannot create archive.", e)
            deletePartialDestination()
            abortNotification(context)
            finishedExportingCallback?.onProjectExportFailed(e.message)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot create archive.", e)
            deletePartialDestination()
            abortNotification(context)
            finishedExportingCallback?.onProjectExportFailed(e.message)
        } finally {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        }
    }

    fun execute() {
        CoroutineScope(Dispatchers.IO).launch {
            exportProjectToExternalStorage()
        }
    }

    fun registerCallback(callback: ProjectExportCallback) {
        finishedExportingCallback = callback
    }

    private fun updateNotification(context: Context) {
        StatusBarNotificationManager(context).showOrUpdateNotification(
            context, notificationData, NOTIFICATION_PROGRESS_COMPLETE, null)
    }

    private fun abortNotification(context: Context) {
        StatusBarNotificationManager(context).abortProgressNotificationWithMessage(
            context, notificationData,
            R.string.save_project_to_external_storage_io_exception_message
        )
    }

    private fun deleteUndoFile() {
        val undoCodeFile = File(projectDir, Constants.UNDO_CODE_XML_FILE_NAME)
        if (undoCodeFile.exists()) {
            try {
                StorageOperations.deleteFile(undoCodeFile)
            } catch (exception: IOException) {
                Log.e(TAG, "Deleting undo file failed.", exception)
            }
        }
    }

    private fun copyFileToUriWithProgress(
        contentResolver: android.content.ContentResolver,
        uri: Uri,
        sourceFile: File
    ): ByteArray {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            FileInputStream(sourceFile).use { inputStream ->
                return copyStreamWithProgressAndHash(inputStream, outputStream, sourceFile.length()) { done, total ->
                    progressListener?.onProgress(done, total)
                }
            }
        } ?: throw IOException("Cannot open output stream for export destination")
    }

    private fun verifyUriContent(
        contentResolver: android.content.ContentResolver,
        uri: Uri,
        expectedHash: ByteArray,
        expectedSize: Long
    ): Boolean {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            if (!verifyStreamHash(inputStream, expectedHash) { done, _ ->
                progressListener?.onProgress(done, expectedSize)
            }) {
                return false
            }
        } ?: return false
        if (expectedSize < 0) return true
        return readUriSize(contentResolver, uri) == expectedSize
    }

    private fun readUriSize(contentResolver: android.content.ContentResolver, uri: Uri): Long {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).length() } ?: -1L
        }
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex)
                }
            }
        }
        return -1L
    }

    private fun deletePartialDestination() {
        try {
            if (projectDestination.scheme == "file") {
                projectDestination.path?.let { File(it).delete() }
            } else {
                context.contentResolver.delete(projectDestination, null, null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot delete partial export file.", e)
        }
    }

    interface ProjectExportCallback {
        fun onProjectExportFinished()
        fun onProjectExportFailed(errorMessage: String?) {}
    }

    companion object {
        private val TAG = ProjectExportTask::class.java.simpleName
        private const val NOTIFICATION_PROGRESS_COMPLETE = 100
    }
}

internal const val EXPORT_COPY_BUFFER_SIZE = 65536

internal fun copyStreamWithProgressAndHash(
    input: java.io.InputStream,
    output: java.io.OutputStream,
    totalBytes: Long,
    onProgress: (bytesDone: Long, bytesTotal: Long) -> Unit
): ByteArray {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(EXPORT_COPY_BUFFER_SIZE)
    var done = 0L
    var read: Int
    while (input.read(buffer).also { read = it } != -1) {
        output.write(buffer, 0, read)
        digest.update(buffer, 0, read)
        done += read
        onProgress(done, totalBytes)
    }
    output.flush()
    onProgress(done, totalBytes)
    return digest.digest()
}

internal fun verifyStreamHash(
    input: java.io.InputStream,
    expectedHash: ByteArray,
    onProgress: (bytesDone: Long, bytesTotal: Long) -> Unit
): Boolean {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(EXPORT_COPY_BUFFER_SIZE)
    var done = 0L
    var read: Int
    while (input.read(buffer).also { read = it } != -1) {
        digest.update(buffer, 0, read)
        done += read
        onProgress(done, -1L)
    }
    return java.security.MessageDigest.isEqual(digest.digest(), expectedHash)
}
