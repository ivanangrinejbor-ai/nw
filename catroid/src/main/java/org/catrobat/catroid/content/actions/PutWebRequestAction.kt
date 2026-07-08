package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.catrobat.catroid.content.MyActivityManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import java.io.IOException
import java.util.concurrent.TimeUnit

class PutWebRequestAction : TemporalAction() {
    var scope: Scope? = null
    var rurl: Formula? = null
    var header: Formula? = null
    var body: Formula? = null
    var timeout: Formula? = null
    var userVariable: UserVariable? = null

    fun setVariable(userVariable: UserVariable?) {
        this.userVariable = userVariable ?: return
    }

    override fun update(percent: Float) {
        var urlVal = rurl?.interpretObject(scope) ?: ""
        var urlText = urlVal.toString()
        var headerVal = header?.interpretObject(scope) ?: ""
        var bodyVal = body?.interpretObject(scope) ?: ""
        var timeoutObj = timeout?.interpretObject(scope)

        var headerText = headerVal.toString().trim()
        var mediaTypeString = headerText.removePrefix("Content-Type:").trim()
        var json = bodyVal.toString()
        var timeoutSec = when (timeoutObj) {
            is Number -> (timeoutObj as Number).toLong()
            else -> 30L
        }

        if (userVariable == null) return

        val client = OkHttpClient.Builder()
            .connectTimeout(timeoutSec, TimeUnit.SECONDS)
            .readTimeout(timeoutSec, TimeUnit.SECONDS)
            .writeTimeout(timeoutSec, TimeUnit.SECONDS)
            .build()

        val mediaType = mediaTypeString.toMediaTypeOrNull() ?: "application/json".toMediaTypeOrNull() ?: return
        val bodyn = RequestBody.create(mediaType, json)
        val request = Request.Builder()
            .url(urlText)
            .put(bodyn)
            .build()
        Thread {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    MyActivityManager.stage_activity?.runOnUiThread {
                        userVariable?.value = "Response error: ${e.message}"
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: "Empty response"
                        MyActivityManager.stage_activity?.runOnUiThread {
                            userVariable?.value = bodyStr
                        }
                    } else {
                        val errorMessage = "Error ${response.code}: ${response.message}"
                        MyActivityManager.stage_activity?.runOnUiThread {
                            userVariable?.value = errorMessage
                        }
                    }
                }
            })
        }.start()
    }
}
