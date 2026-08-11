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
package org.catrobat.catroid.content.actions

import android.util.Log
import okhttp3.Response
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider
import org.catrobat.catroid.utils.Utils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

open class LookFromUrlAction : WebAction() {
    var response: InputStream? = null
    var errorCode: String? = null
    var lookName: Formula? = null
    private var lookNameValue: String? = null
    private var fileExtension: String? = null

    override fun act(delta: Float): Boolean {
        return if (scope?.sprite == null || formula == null || scope?.sequence == null) {
            true
        } else super.act(delta)
    }

    override fun handleResponse() {
        val sprite = scope?.sprite ?: return
        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val ec = errorCode
        when {
            ec != null -> handleError(ec)
            response == null -> handleInvalidFormat()
            else -> try {
                val tempLookFile = File.createTempFile("look_from_url", fileExtension)
                StorageOperations.copyStreamToFile(response, tempLookFile)
                val imageDirectory = File(scene.directory, Constants.IMAGE_DIRECTORY_NAME)
                if (!imageDirectory.exists()) {
                    imageDirectory.mkdirs()
                }
                val lookDataFile = StorageOperations.copyFileToDir(tempLookFile, imageDirectory)
                tempLookFile.delete()
                val requestedName = lookNameValue ?: lookDataFile.nameWithoutExtension
                val uniqueName = UniqueNameProvider().getUniqueNameInNameables(requestedName, sprite.lookList)
                val lookData = LookData(uniqueName, lookDataFile)
                lookData.collisionInformation.calculate()
                sprite.lookList.add(lookData)
                sprite.look?.lookData = lookData
            } catch (exception: IOException) {
                Log.e(javaClass.simpleName, "Couldn't interpret InputStream as image", exception)
                handleInvalidFormat()
            }
        }
    }

    private fun handleInvalidFormat() {
        CatroidApplication.getAppContext()?.let {
            showToastMessage(it.getString(R.string.look_request_type_error_message, url))
        }
    }

    private fun showToastMessage(message: String) {
        val params = ArrayList<Any>(listOf(message))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
    }

    override fun handleError(error: String) {
        errorCode = error
        CatroidApplication.getAppContext()?.let {
            showToastMessage(it.getString(R.string.look_request_http_error_message, url, errorCode))
        }
    }

    override fun restart() {
        response = null
        errorCode = null
        lookNameValue = null
        super.restart()
    }

    override fun onRequestSuccess(httpResponse: Response) {
        val body = httpResponse.body
        response = body?.byteStream()

        val contentType = body?.contentType()
        var determinedExtension: String? = null

        if (contentType != null) {
            val type = contentType.type.lowercase()
            val subtype = contentType.subtype.lowercase()
            Log.d("LookFromUrlAction", "Received Content-Type: $type/$subtype")

            when ("$type/$subtype") {
                "image/png" -> determinedExtension = ".png"
                "image/jpeg" -> determinedExtension = ".jpg"
                "image/gif" -> determinedExtension = ".gif"
                "image/bmp" -> determinedExtension = ".bmp"
                "image/webp" -> determinedExtension = ".webp"
                "application/octet-stream" -> {
                    Log.d("LookFromUrlAction", "Content-Type is application/octet-stream. Relying on filename extension.")
                }
            }
        }

        val fileNameFromHttp = Utils.getFileNameFromHttpResponse(httpResponse) ?: Utils.getFileNameFromURL(url)
        val parts = fileNameFromHttp.split('.', limit = 2)
        lookNameValue = lookName?.interpretString(scope)?.takeIf { it.isNotBlank() } ?: parts[0]
        val originalFileExtension = parts.getOrNull(1)?.let { ".$it" }?.lowercase()

        if (determinedExtension != null) {
            fileExtension = determinedExtension
        } else {
            fileExtension = originalFileExtension
        }

        if (fileExtension == null) {
            Log.w("LookFromUrlAction", "Could not determine file extension. Using default for temp file.")
        } else {
            Log.i("LookFromUrlAction", "Using file extension: $fileExtension for look: $lookNameValue")
        }

        super.onRequestSuccess(httpResponse)
    }

    override fun onRequestError(httpError: String) {
        errorCode = httpError
        super.onRequestError(httpError)
    }

    override fun onCancelledCall() {
        response = null
        errorCode = null
        lookNameValue = null
        super.onCancelledCall()
    }
}