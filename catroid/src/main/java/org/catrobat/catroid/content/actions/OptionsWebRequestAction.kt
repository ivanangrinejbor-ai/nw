package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import okhttp3.Call
import okhttp3.Callback
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

class OptionsWebRequestAction : TemporalAction() {
    companion object {
        private val okHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    private var started = false

    var scope: Scope? = null
    var rurl: Formula? = null
    var header: Formula? = null
    var timeout: Formula? = null
    var userVariable: UserVariable? = null

    fun setVariable(userVariable: UserVariable?) {
        this.userVariable = userVariable ?: return
    }

    override fun update(percent: Float) {
        if (started) return
        started = true
        if (scope == null) { started = false; return }
        var urlVal = rurl?.interpretObject(scope) ?: ""
        var urlText = urlVal.toString()
        var timeoutObj = timeout?.interpretObject(scope)

        var timeoutSec = when (timeoutObj) {
            is Number -> (timeoutObj as Number).toLong()
            else -> 30L
        }

        if (userVariable == null) return

        try {
            val reqBuilder = Request.Builder().url(urlText).method("OPTIONS", null)
            val headerStr = header?.interpretString(scope)
            if (!headerStr.isNullOrBlank()) {
                headerStr.split("\n").forEach { line ->
                    val colonIdx = line.indexOf(':')
                    if (colonIdx > 0) {
                        val key = line.substring(0, colonIdx).trim()
                        val value = line.substring(colonIdx + 1).trim()
                        reqBuilder.addHeader(key, value)
                    }
                }
            }
            val request = reqBuilder.build()
            Thread {
                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        MyActivityManager.stage_activity?.runOnUiThread {
                            userVariable?.value = "Response error: ${e.message}"
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val allowHeader = response.header("Allow") ?: response.header("allow") ?: ""
                            MyActivityManager.stage_activity?.runOnUiThread {
                                userVariable?.value = allowHeader
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
        } catch (e: Exception) {
            userVariable?.value = "Request error: ${e.message}"
        }
    }

    override fun restart() {
        super.restart()
        started = false
    }
}
