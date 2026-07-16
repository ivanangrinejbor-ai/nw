/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.io.asynctask

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.Constants.CACHE_DIRECTORY
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.content.backwardcompatibility.ProjectMetaDataParser
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.io.ZipArchiver
import org.catrobat.catroid.io.ProjectCrypto
import org.catrobat.catroid.apkbuild.ProtectedProjectPayload
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider
import org.catrobat.catroid.utils.FileMetaDataExtractor
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private val TAG = ProjectUnZipperAndImporter::class.java.simpleName

sealed class ImportResult {
    object Success : ImportResult()
    object Failure : ImportResult()
    object WrongPassword : ImportResult()
    data class BakedProject(val projectDir: File) : ImportResult()
    data class UnsupportedVersion(val version: Double, val projectDir: File?) : ImportResult()
}

class ProjectUnZipperAndImporter @JvmOverloads constructor(
    val onImportFinished: (ImportResult) -> Unit = {},
    val scope: CoroutineScope = CoroutineScope(
        Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Uncaught exception in import coroutine", throwable)
        }
    ),
    var password: String? = null,
    val onProgress: ((percent: Int, detail: String) -> Unit)? = null,
    val contentResolver: ContentResolver? = null,
    val importContext: Context? = null
) {



    fun reportProgress(percent: Int, detail: String) {
        onProgress?.invoke(percent, detail)
    }
    fun continueImport(projectDir: File) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val dest = importStandardProject(projectDir)
                StorageOperations.deleteDir(projectDir)
                if (dest != null) ImportResult.Success else ImportResult.Failure
            }
            withContext(Dispatchers.Main) {
                onImportFinished(result)
            }
        }
    }

    fun cancelImport(projectDir: File) {
        scope.launch {
            withContext(Dispatchers.IO) {
                if (projectDir.exists()) StorageOperations.deleteDir(projectDir)
            }
        }
    }

    fun unZipAndImportAsync(files: Array<File>) {
        scope.launch {

            val file = files.firstOrNull()
            val result = if (file != null) {
                unzipAndImportProject(file)
            } else {
                ImportResult.Failure
            }

            withContext(Dispatchers.Main) {
                onImportFinished(result)
            }
        }
    }

    fun unZipAndImportFromUris(uris: List<Uri>) {
        scope.launch {
            try {
                Log.d(TAG, "Starting import from URIs...")
                val result = if (uris.isNotEmpty()) {
                    val cachedFile = copyUrisToCache(uris)
                    if (cachedFile != null) {
                        Log.d(TAG, "Copy success, starting unzip: ${cachedFile.absolutePath}")
                        unzipAndImportProject(cachedFile)
                    } else {
                        Log.e(TAG, "No valid file could be copied to cache")
                        ImportResult.Failure
                    }
                } else {
                    ImportResult.Failure
                }
                withContext(Dispatchers.Main) {
                    onImportFinished(result)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "CRITICAL ERROR during import from URIs: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onImportFinished(ImportResult.Failure)
            }
        }
        }
    }

    private fun copyUrisToCache(uris: List<Uri>): File? {
        val resolver = contentResolver ?: return null
        for (uri in uris) {
            val fileName = StorageOperations.resolveFileName(resolver, uri)
            if (!isValidImportExtension(fileName)) continue
            reportProgress(0, "import_step_prepare")
            // Delegate to StorageOperations which handles WebP and unique filenames
            // Progress detail between 0-14% is approximate since fine-grained progress
            // during the copy would require StorageOperations API changes
            reportProgress(7, "import_step_prepare")
            val cachedFile = StorageOperations.copyUriToDir(resolver, uri, CACHE_DIRECTORY, fileName)
            reportProgress(14, "import_step_prepare")
            return cachedFile
        }
        return null
    }

    companion object {
        private fun isValidImportExtension(fileName: String): Boolean {
            return fileName.endsWith(Constants.CATROBAT_EXTENSION, ignoreCase = true) ||
                fileName.endsWith(Constants.NEW_CATROBAT_EXTENSION, ignoreCase = true) ||
                fileName.endsWith(Constants.OLD_CATROBAT_EXTENSION, ignoreCase = true) ||
                fileName.endsWith(Constants.ZIP_EXTENSION, ignoreCase = true) ||
                fileName.endsWith(Constants.NPC_EXTENSION, ignoreCase = true) ||
                fileName.endsWith(".ncp", ignoreCase = true) ||
                fileName.endsWith(".enc", ignoreCase = true)
        }
    }
}

/*fun unzipAndImportProjects(files: Array<File>): Boolean {
    var success = true
    files.forEach { projectDir ->
        success = success && unzipAndImportProject(projectDir)
    }
    return success
}*/

/*private fun unzipAndImportProject(projectDir: File): Boolean = try {
    val cachedProjectDir = File(CACHE_DIRECTORY, StorageOperations.getSanitizedFileName(projectDir.name))
    if (cachedProjectDir.isDirectory) {
        try {
            StorageOperations.deleteDir(cachedProjectDir)
        } catch (e: Exception) {
            Log.e("ZIP_TASK", "ERROR: " + e.message)
        }
    }
    ZipArchiver().unzip(projectDir, cachedProjectDir)
    importProject(cachedProjectDir)
} catch (e: IOException) {
    Log.e(TAG, "Cannot unzip project " + projectDir.name, e)
    false
}*/

private fun ProjectUnZipperAndImporter.unzipAndImportProject(projectZipFile: File): ImportResult {
    val tempDirName = StorageOperations.getSanitizedFileName(projectZipFile.name) + "_temp_import"
    val cachedProjectDir = File(CACHE_DIRECTORY, tempDirName)
    return try {
        if (cachedProjectDir.isDirectory) { StorageOperations.deleteDir(cachedProjectDir) }
        val requiredSpace = projectZipFile.length() * 5
        val usableSpace = CACHE_DIRECTORY.usableSpace
        if (usableSpace > 0 && requiredSpace > usableSpace) {
            Log.e(TAG, "Not enough disk space: need ${requiredSpace / 1024 / 1024}MB, have ${usableSpace / 1024 / 1024}MB")
            return@unzipAndImportProject ImportResult.Failure
        }
        cachedProjectDir.mkdirs()

        var fileToUnzip = projectZipFile
        if (ProjectCrypto.isEncrypted(projectZipFile)) {
            reportProgress(5, "import_step_decrypt")
            // Сначала пробуем статический ключ запечённых проектов (bake/EXE),
            // если пароль не задан явно. Иначе — предоставленный пароль.
            val key = if (!password.isNullOrEmpty()) password!! else ProtectedProjectPayload.PASSWORD
            val decryptedFile = File(CACHE_DIRECTORY, tempDirName + "_decrypted.zip")
            if (!ProjectCrypto.decrypt(projectZipFile, decryptedFile, key)) {
                Log.e(TAG, "Failed to decrypt project (wrong password?)")
                return@unzipAndImportProject ImportResult.WrongPassword
            }
            fileToUnzip = decryptedFile
        }

        val zipArchiver = ZipArchiver()
        zipArchiver.unzip(fileToUnzip, cachedProjectDir, object : ZipArchiver.UnzipProgressListener {
            override fun onProgress(percent: Int, filesDone: Int, totalFiles: Int, currentFile: String) {
                val overallPercent = 15 + (percent * 10 / 100)
                reportProgress(overallPercent, "import_step_unzip|$filesDone|$totalFiles|$currentFile")
            }
        })
        if (fileToUnzip != projectZipFile) { fileToUnzip.delete() }

        try {
            org.catrobat.catroid.utils.MatryoshkaManager.unpackIfMatryoshka(cachedProjectDir)
        } catch (e: Exception) {
            Log.e(TAG, "Matryoshka check failed, continuing with raw project", e)
        }
        val codeXml = File(cachedProjectDir, Constants.CODE_XML_FILE_NAME)
        val initLunoTxt = File(cachedProjectDir, "init.luno.txt")
        val initLunoBin = File(cachedProjectDir, "init.bin")

        if (codeXml.exists()) {
            val appVersion = readApplicationVersion(codeXml)
            // Only the legacy ".newtrobat" format is version-gated. Other formats
            // (.catrobat, .neotrobat, .zip, ...) import as-is, regardless of
            // the application version written in code.xml.
            if (appVersion != null &&
                projectZipFile.name.endsWith(Constants.OLD_CATROBAT_EXTENSION, true) &&
                isUnsupportedVersion(appVersion)
            ) {
                Log.e(TAG, "Unsupported .newtrobat application version: $appVersion")
                return@unzipAndImportProject ImportResult.UnsupportedVersion(appVersion.toDoubleOrNull() ?: 0.0, cachedProjectDir)
            }
            reportProgress(25, "import_step_scanning")
            val entries = scanProjectEntries(codeXml)
            val totalEntries = entries.size.coerceAtLeast(1)
            var current = 0
            for ((sceneName, spriteName) in entries) {
                current++
                val pct = 25 + (current * 45 / totalEntries)
                val detail = if (spriteName != null) {
                    "scene|$sceneName\nsprite|$spriteName"
                } else {
                    "scene|$sceneName"
                }
                reportProgress(pct, detail)
            }

            reportProgress(75, "import_step_copy")
            if (importStandardProject(cachedProjectDir) != null) {
                reportProgress(100, "import_step_finish")
                StorageOperations.deleteDir(cachedProjectDir)
                ImportResult.Success
            } else { ImportResult.Failure }
        } else if (initLunoTxt.exists() || initLunoBin.exists()) {
            Log.d(TAG, "Detected baked project in: ${cachedProjectDir.absolutePath}")
            reportProgress(100, "import_step_finish")
            ImportResult.BakedProject(cachedProjectDir)
        } else {
            reportProgress(100, "import_step_finish")
            Log.e(TAG, "Invalid project structure")
            ImportResult.Failure
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Cannot unzip project ${projectZipFile.name}: ${e.javaClass.simpleName}: ${e.message}", e)
        try {
            if (cachedProjectDir.exists()) StorageOperations.deleteDir(cachedProjectDir)
        } catch (_: Exception) {}
        ImportResult.Failure
    }
}

private fun scanProjectEntries(codeXml: File): List<Pair<String, String?>> {
    val entries = mutableListOf<Pair<String, String?>>()
    try {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(FileInputStream(codeXml), "UTF-8")

        var currentSceneName: String? = null
        var depth = 0
        var inScene = false
        var inSpriteList = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    if (!inScene && (tagName.endsWith(".Scene") || (tagName == "scene"))) {
                        inScene = true
                        currentSceneName = null
                    } else if (inScene && !inSpriteList && tagName == "name") {
                        currentSceneName = parser.nextText()
                        if (currentSceneName != null) {
                            entries.add(currentSceneName to null)
                        }
                    } else if (inScene && (tagName.endsWith(".Sprite") || tagName == "sprite" || tagName == "Sprite")) {
                        inSpriteList = true
                    } else if (inSpriteList && tagName == "name") {
                        val spriteName = parser.nextText()
                        if (currentSceneName != null && spriteName != null) {
                            entries.add(currentSceneName to spriteName)
                        }
                    }
                    depth++
                }
                XmlPullParser.END_TAG -> {
                    val tagName = parser.name
                    if (inScene && (tagName.endsWith(".Scene") || tagName == "scene")) {
                        inScene = false
                        currentSceneName = null
                    }
                    if (inSpriteList && (tagName.endsWith(".Sprite") || tagName == "sprite" || tagName == "Sprite")) {
                        inSpriteList = false
                    }
                    depth--
                }
            }
            parser.next()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to scan project XML", e)
    }
    return entries
}

private fun ProjectUnZipperAndImporter.importStandardProject(cachedProjectDir: File): File? {
    val projectName = getProjectName(cachedProjectDir)
    if (projectName == null) {
        Log.e(TAG, "Failed to get project name from ${cachedProjectDir.absolutePath}")
        return null
    }
    val uniqueName = UniqueNameProvider().getUniqueName(projectName, FileMetaDataExtractor
        .getProjectNames(FlavoredConstants.DEFAULT_ROOT_DIRECTORY))

    val destinationDirectory = File(
        FlavoredConstants.DEFAULT_ROOT_DIRECTORY,
        FileMetaDataExtractor.encodeSpecialCharsForFileSystem(uniqueName))

    return try {
        Log.d(TAG, "Copying project to: ${destinationDirectory.absolutePath}")
        copyProject(cachedProjectDir, destinationDirectory, uniqueName)
        normalizeProject(destinationDirectory)
        destinationDirectory
    } catch (e: IOException) {
        Log.e(TAG, "Copy failed at phase '${destinationDirectory.absolutePath}': ${e.message}", e)
        errorWhileImporting(cachedProjectDir, destinationDirectory)
        null
    }
}

/**
 * Best-effort: load the freshly imported project and re-save it so any
 * legacy / unknown bricks are converted to UnknownBrick and the on-disk project
 * is in a clean, current, loadable state. If loading or saving fails for any
 * reason we keep the copied project as-is (still a valid project on disk), so
 * this can never leave the project in a worse state than the plain copy.
 */
private fun ProjectUnZipperAndImporter.normalizeProject(projectDir: File) {
    val ctx = importContext ?: return
    try {
        val project = XstreamSerializer.getInstance().loadProject(projectDir, ctx)
        XstreamSerializer.getInstance().saveProject(project)
    } catch (e: Exception) {
        Log.w(TAG, "Project normalization (load/save) failed; keeping imported copy as-is", e)
    }
}

private fun getProjectName(projectDir: File): String? {
    val xmlFile = File(projectDir, Constants.CODE_XML_FILE_NAME)
    if (!xmlFile.exists()) {
        Log.e(TAG, "No xml file found for project " + projectDir.name)
        return null
    }
    return try {
        ProjectMetaDataParser(xmlFile).projectMetaData.name
    } catch (e: IOException) {
        Log.d(TAG, "Cannot extract projectName from xml", e)
        null
    }
}

private fun readApplicationVersion(codeXml: File): String? {
    return try {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(FileInputStream(codeXml), "UTF-8")
        var inHeader = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "header") inHeader = true
                    else if (inHeader && parser.name == "applicationVersion") {
                        return parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "header") inHeader = false
                }
            }
            parser.next()
        }
        null
    } catch (_: Exception) { null }
}

private const val MAX_SUPPORTED_VERSION = "2.1.2"

private fun isUnsupportedVersion(versionStr: String): Boolean {
    val parts = versionStr.split(".").mapNotNull { it.toIntOrNull() }
    val maxParts = MAX_SUPPORTED_VERSION.split(".").map { it.toInt() }
    val maxLen = maxOf(parts.size, maxParts.size)
    for (i in 0 until maxLen) {
        val v = parts.getOrElse(i) { 0 }
        val m = maxParts.getOrElse(i) { 0 }
        if (v > m) return true
        if (v < m) return false
    }
    return false
}

private fun importProject(projectDir: File): Boolean {
    var projectName = getProjectName(projectDir) ?: return false
    projectName = UniqueNameProvider().getUniqueName(projectName, FileMetaDataExtractor
        .getProjectNames(FlavoredConstants.DEFAULT_ROOT_DIRECTORY))
    val destinationDirectory = File(
        FlavoredConstants.DEFAULT_ROOT_DIRECTORY,
        FileMetaDataExtractor.encodeSpecialCharsForFileSystem(projectName))
    return try {
        copyProject(projectDir, destinationDirectory, projectName)
        true
    } catch (e: IOException) {
        Log.e(TAG, "Something went wrong while importing project ${projectDir.name}", e)
        errorWhileImporting(projectDir, destinationDirectory)
        false
    }
}

private fun copyProject(projectDir: File, destinationDirectory: File, projectName: String) {
    StorageOperations.copyDir(projectDir, destinationDirectory)
    XstreamSerializer.renameProject(File(destinationDirectory, Constants.CODE_XML_FILE_NAME), projectName)
}

private fun errorWhileImporting(projectDir: File, destinationDirectory: File) {
    if (destinationDirectory.isDirectory) {
        Log.e(TAG, "Folder exists, trying to delete folder.")
        try {
            StorageOperations.deleteDir(destinationDirectory)
        } catch (deleteException: IOException) {
            Log.e(TAG, "Cannot delete folder $destinationDirectory", deleteException)
        }
    }
}
