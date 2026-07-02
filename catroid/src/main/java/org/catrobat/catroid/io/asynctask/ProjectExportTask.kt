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
import java.io.IOException

class ProjectExportTask(
    private val projectDir: File,
    private val projectDestination: Uri,
    private val notificationData: NotificationData,
    private val context: Context,
    private val password: String? = null
) {

    private var finishedExportingCallback: ProjectExportCallback? = null

    suspend fun exportProjectToExternalStorage() {
        deleteUndoFile()

        val projectFileName = projectDir.name + Constants.ZIP_EXTENSION
        val cacheFile = File(Constants.CACHE_DIRECTORY, projectFileName)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        try {
            ZipArchiver().zip(cacheFile, projectDir.listFiles())

            if (password != null && password.isNotEmpty()) {
                val encryptedFile = File(Constants.CACHE_DIRECTORY, "$projectFileName.enc")
                ProjectCrypto.encrypt(cacheFile, encryptedFile, password)
                cacheFile.delete()
                encryptedFile.renameTo(cacheFile)
            }

            val contentResolver = context.contentResolver
            StorageOperations.copyFileContentToUri(contentResolver, projectDestination, cacheFile)
            updateNotification(context)
            finishedExportingCallback?.onProjectExportFinished()
        } catch (e: IOException) {
            Log.e(TAG, "Cannot create archive.", e)
            abortNotification(context)
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

    interface ProjectExportCallback {
        fun onProjectExportFinished()
    }

    companion object {
        private val TAG = ProjectExportTask::class.java.simpleName
        private const val NOTIFICATION_PROGRESS_COMPLETE = 100
    }
}
