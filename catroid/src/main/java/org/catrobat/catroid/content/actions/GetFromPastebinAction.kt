package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import java.net.HttpURLConnection
import java.net.URL

class GetFromPastebinAction() : TemporalAction() {
    var scope: Scope? = null
    var url: Formula? = null
    var variable: UserVariable? = null

    override fun update(percent: Float) {
        val link = url?.interpretString(scope) ?: return
        val result = try {
            val connection = URL(link).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                val code = connection.responseCode
                if (code in 200..299) {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    "ERROR"
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            "ERROR"
        }
        variable?.value = result
    }
}