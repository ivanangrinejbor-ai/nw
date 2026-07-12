/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

package org.catrobat.catroid.content.actions

import android.os.Environment
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.utils.Utils
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class DownloadToPathAction() : TemporalAction() {
    var scope: Scope? = null
    var url: Formula? = null
    var path: Formula? = null
    private var started = false

    override fun update(percent: Float) {
        if (scope == null) return
        if (started) return; started = true
        val urlStr = url?.interpretString(scope) ?: return
        if (urlStr.isBlank()) return
        val pathStr = path?.interpretString(scope) ?: return
        if (pathStr.isBlank()) return
        thread {
            var connection: HttpURLConnection? = null
            try {
                val urlObj = URL(urlStr)
                if (urlObj.protocol !in arrayOf("http", "https")) {
                    Log.e("DownloadToPathAction", "Invalid URL scheme: ${urlObj.protocol}")
                    return@thread
                }
                connection = urlObj.openConnection() as HttpURLConnection
                connection.connect()
                val statusCode = connection.responseCode
                DownloadState.lastStatusCode = statusCode
                val contentLength = connection.contentLength
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(pathStr)
                val baseCanonical = downloadsDir.canonicalPath
                val fileCanonical = file.canonicalPath
                if (!fileCanonical.startsWith(baseCanonical + File.separator)) {
                    Log.e("DownloadToPathAction", "Path traversal detected: $pathStr")
                    return@thread
                }
                file.parentFile?.mkdirs()
                connection.inputStream.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                DownloadState.progress = (totalRead * 100 / contentLength).toInt()
                            }
                        }
                    }
                }
                Log.d("DownloadToPath", "Downloaded to: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("DownloadToPath", "Error: ${e.message}")
            } finally {
                connection?.disconnect()
                DownloadState.progress = -1
            }
        }
    }
}
