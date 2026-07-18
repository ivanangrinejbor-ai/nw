package org.catrobat.catroid.content.actions

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.RuntimeMutationTracker
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

        // BUG-NS-02 fix: file I/O and XStream parse can block for tens of ms on large
        // files or slow storage. Move work to IO dispatcher so the GDX render thread is
        // not stalled. executeConsoleScript is marshalled back to Main afterwards.
        ioScope.launch {
            try {
                val neoScriptFile = loadNeoScriptFile(path)
                val result = NeoScriptImporter.importScripts(neoScriptFile, project, targetSprite, overwrite)
                // ImportScriptBrick has no persist flag — mutations are always temporary
                RuntimeMutationTracker.hasTemporaryMutations = true
                withContext(Dispatchers.Main) {
                    val stageListener = StageActivity.getActiveStageListener()
                    for (script in result.added) {
                        stageListener?.executeConsoleScript(targetSprite, script)
                    }
                }
            } catch (e: NeoScriptException) {
                Log.e(TAG, "Failed to import script module: " + e.message, e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import script module", e)
            }
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

    private suspend fun loadNeoScriptFile(path: String) = withContext(Dispatchers.IO) {
        if (path.startsWith("content://")) {
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
    }

    companion object {
        private const val TAG = "ImportScriptAction"

        // Coroutine scope for background file I/O (survives action lifetime;
        // SupervisorJob prevents one failure cancelling sibling actions).
        private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
