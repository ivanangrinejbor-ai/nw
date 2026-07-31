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
import org.catrobat.catroid.content.RuntimeMutationTracker
import org.catrobat.catroid.io.asynctask.ProjectSaver
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.UnknownBrick
import org.catrobat.catroid.content.bricks.NoteBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.neoscript.NeoScriptException
import org.catrobat.catroid.neoscript.NeoScriptFile
import org.catrobat.catroid.neoscript.NeoScriptImporter
import org.catrobat.catroid.neoscript.NeoScriptImporter.ImportStrategy
import org.catrobat.catroid.neoscript.NeoScriptSerializer
import org.catrobat.catroid.stage.StageActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class AssignScriptsAction : TemporalAction() {
    var scope: Scope? = null
    var filePath: Formula? = null
    var objectName: Formula? = null
    var sceneName: Formula? = null
    var replaceExistingScripts: Boolean = false
    var savePersistent: Boolean = false

    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true

        val project = scope?.project ?: return
        val path = filePath?.interpretString(scope) ?: return
        val targetName = objectName?.interpretString(scope) ?: return
        if (path.isBlank() || targetName.isBlank()) return

        val sceneStr = sceneName?.interpretString(scope)
        val scene = resolveScene(project, sceneStr)
        if (scene == null) {
            Log.e(TAG, "Scene not found")
            return
        }

        val targetSprite = scene.getSprite(targetName)
        if (targetSprite == null) {
            Log.e(TAG, "Object '$targetName' not found in scene '${scene.name}'")
            return
        }

        ioScope.launch {
            try {
                val neoScriptFile = loadNeoScriptFile(path)

                val hasUnknownBricks = checkForUnknownBricks(neoScriptFile)
                if (hasUnknownBricks) {
                    Log.w(TAG, "Unknown blocks detected in .neoscript file. Continuing with replacement.")
                    replaceUnknownBricks(neoScriptFile)
                }

                val strategy = if (replaceExistingScripts) ImportStrategy.REPLACE_ALL else ImportStrategy.APPEND_ALL
                val result = NeoScriptImporter.importScripts(neoScriptFile, project, targetSprite, strategy)

                if (savePersistent) {
                    RuntimeMutationTracker.hasPersistentMutations = true
                    try {
                        ProjectSaver(project, CatroidApplication.getAppContext()).saveProjectAsync(onSaveProjectComplete = { success ->
                            if (!success) Log.e(TAG, "Failed to persist project after assigning scripts to '$targetName'")
                        })
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not persist assigned scripts to '$targetName'", e)
                    }
                } else {
                    RuntimeMutationTracker.hasTemporaryMutations = true
                }

                withContext(Dispatchers.Main) {
                    val stageListener = StageActivity.getActiveStageListener()
                    if (stageListener != null) {
                        val activeScene = ProjectManager.getInstance().getCurrentlyPlayingScene()
                        val sceneIsActive = activeScene != null && activeScene.getSceneId() == scene.getSceneId()
                        if (sceneIsActive) {
                            for (script in result.added) {
                                stageListener.executeConsoleScript(targetSprite, script)
                            }
                        }
                    }
                }
            } catch (e: NeoScriptException) {
                Log.e(TAG, "Failed to assign scripts: " + e.message, e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to assign scripts", e)
            }
        }
    }

    private fun resolveScene(project: Project, sceneStr: String?): Scene? {
        if (sceneStr.isNullOrEmpty()) {
            val current = ProjectManager.getInstance().getCurrentlyPlayingScene()
            if (current != null) return current
            return project.defaultScene
        }
        return project.getSceneByName(sceneStr)
    }

    private suspend fun loadNeoScriptFile(path: String): NeoScriptFile = withContext(Dispatchers.IO) {
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

    private fun checkForUnknownBricks(file: NeoScriptFile): Boolean {
        for (script in file.scripts) {
            for (brick in script.brickList) {
                if (brick is UnknownBrick) {
                    return true
                }
            }
        }
        return false
    }

    private fun replaceUnknownBricks(file: NeoScriptFile) {
        for (script in file.scripts) {
            val newBrickList: MutableList<org.catrobat.catroid.content.bricks.Brick> = ArrayList()
            for (brick in script.brickList) {
                if (brick is UnknownBrick) {
                    val note = NoteBrick("This block is not supported")
                    newBrickList.add(note)
                } else {
                    newBrickList.add(brick)
                }
            }
            script.brickList.clear()
            script.brickList.addAll(newBrickList)
        }
    }

    companion object {
        private const val TAG = "AssignScriptsAction"

        private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
