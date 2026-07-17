package org.catrobat.catroid.content.actions

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.neoscript.NeoScriptException
import org.catrobat.catroid.neoscript.NeoScriptImporter
import org.catrobat.catroid.neoscript.NeoScriptSerializer
import org.catrobat.catroid.stage.StageActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class ImportScriptAction : TemporalAction() {
    var scope: Scope? = null
    var objectName: Formula? = null
    var filePath: Formula? = null
    var overwrite: Boolean = false

    private var executed = false

    override fun update(percent: Float) {
        if (executed) {
            return
        }
        executed = true

        val project = scope?.project ?: return
        val targetName = objectName?.interpretString(scope) ?: return
        val path = filePath?.interpretString(scope) ?: return
        if (targetName.isBlank() || path.isBlank()) {
            return
        }

        val targetSprite = findSprite(project, targetName)
        if (targetSprite == null) {
            Log.e(TAG, "Object not found: $targetName")
            return
        }

        try {
            val neoScriptFile = loadNeoScriptFile(path)
            val result = NeoScriptImporter.importScripts(neoScriptFile, project, targetSprite, overwrite)
            val stageListener = StageActivity.getActiveStageListener()
            for (script in result.added) {
                stageListener?.executeConsoleScript(targetSprite, script)
            }
        } catch (e: NeoScriptException) {
            Log.e(TAG, "Failed to import script module: " + e.message, e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import script module", e)
        }
    }

    private fun findSprite(project: Project, name: String): Sprite? {
        for (scene: Scene in project.sceneList) {
            val sprite = scene.getSprite(name)
            if (sprite != null) {
                return sprite
            }
        }
        return null
    }

    private fun loadNeoScriptFile(path: String) = if (path.startsWith("content://")) {
        val context = CatroidApplication.getAppContext()
        val resolver: ContentResolver = context.contentResolver
        val builder = StringBuilder()
        resolver.openInputStream(Uri.parse(path))?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
            var line: String? = reader.readLine()
            while (line != null) {
                builder.append(line).append('\n')
                line = reader.readLine()
            }
        }
        NeoScriptSerializer.deserializeFromString(builder.toString())
    } else {
        NeoScriptSerializer.deserializeFromFile(java.io.File(path))
    }

    companion object {
        private const val TAG = "ImportScriptAction"
    }
}
