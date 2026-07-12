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

import org.catrobat.catroid.content.CustomDns
import android.widget.Toast
import android.content.Context
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.app.Activity
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import com.google.gson.Gson
import okhttp3.Call
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import org.catrobat.catroid.content.GeminiManager
import java.io.IOException
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.UserVariable

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class AskGemini2Action() : TemporalAction() {
    companion object {
        private val okHttpClient by lazy {
            OkHttpClient.Builder()
                .dns(CustomDns())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    private var started = false

    var scope: Scope? = null
    var ask: Formula? = null
    var model: Formula? = null
    var userVariable: UserVariable? = null

    fun setVariable(userVariable: UserVariable?) {
        this.userVariable = userVariable ?: return
    }

    override fun update(percent: Float) {
        if (started) return
        started = true
        if (scope == null) { started = false; return }
        val model_str = model?.interpretString(scope) ?: "models/gemini-1.5-flash-latest"
        val askVal = ask?.interpretObject(scope) ?: ""
        val apiKey = GeminiManager.api_key
        if (apiKey.isNullOrBlank()) return
        val urlText = "https://generativelanguage.googleapis.com/v1beta/$model_str:generateContent"

        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", askVal.toString())
                        })
                    })
                })
            })
        }.toString()

        if (userVariable == null) {
            return
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = RequestBody.create(mediaType, json)

        val request = Request.Builder()
            .url(urlText)
            .post(body)
            .header("x-goog-api-key", apiKey)
            .build()

        Log.d("GeminiAPI", "URL: $urlText")
        Log.d("GeminiAPI", "Request Body: $json")

        Thread {
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    userVariable?.value = "Response error: ${e.message}"
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: "Empty response"
                        try {
                            val gson = Gson()
                            val responseData = gson.fromJson(bodyStr, ResponseData::class.java)
                            val text = responseData.candidates.first().content.parts.first().text
                            userVariable?.value = text
                        } catch (e: Exception) {
                            userVariable?.value = "Error parsing JSON: ${e.message}"
                            Log.e("GeminiAPI", "JSON Parsing Error: ${e.message}")
                        }
                    } else {
                        userVariable?.value = "Error ${response.code}: ${response.message}"
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Log.e("GeminiAPI", "Error body: $errorBody")
                    }
                }
            })
        }.start()
    }

    override fun restart() {
        super.restart()
        started = false
    }
}