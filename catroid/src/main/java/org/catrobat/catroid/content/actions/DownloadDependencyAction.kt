package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadDependencyAction : TemporalAction() {
    var scope: Scope? = null
    var url: Formula? = null
    var destinationPath: Formula? = null

    override fun update(percent: Float) {
        if (scope == null) return
        val urlStr = url?.interpretString(scope) ?: return
        val destStr = destinationPath?.interpretString(scope) ?: return

        if (urlStr.isEmpty() || destStr.isEmpty()) return

        try {
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("DownloadDependency", "HTTP $responseCode for $urlStr")
                return
            }

            val destFile = File(destStr)
            destFile.parentFile?.mkdirs()

            connection.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("DownloadDependency", "Downloaded $urlStr -> $destStr")
        } catch (e: Exception) {
            Log.e("DownloadDependency", "Download failed: ${e.message}", e)
        }
    }
}
