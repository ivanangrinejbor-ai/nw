package org.catrobat.catroid.content.actions

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
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
    var targetSceneName: String? = null  // null = Current scene
    var replaceExistingScripts: Boolean = false

    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true

        val project = scope?.project ?: return
        val path = filePath?.interpretString(scope) ?: return
        val targetName = objectName?.interpretString(scope) ?: return
        if (path.isBlank() || targetName.isBlank()) return

        // Resolve scene
        val scene = resolveScene(project)
        if (scene == null) {
            Log.e(TAG, "Scene not found")
            return
        }

        // Find target object — scoped to the resolved scene
        val targetSprite = scene.getSprite(targetName)
        if (targetSprite == null) {
            Log.e(TAG, "Object '$targetName' not found in scene '${scene.name}'")
            return
        }

        try {
            val neoScriptFile = loadNeoScriptFile(path)

            // Phase 6: UnknownBrick detection — check before import
            val hasUnknownBricks = checkForUnknownBricks(neoScriptFile)
            if (hasUnknownBricks) {
                // Cannot show dialog from runtime — log warning and continue
                Log.w(TAG, "Unknown blocks detected in .neoscript file. Continuing with replacement.")
                replaceUnknownBricks(neoScriptFile)
            }

            // Mode 0 = keep existing + add imported (APPEND_ALL); Mode 1 = replace all (REPLACE_ALL)
            val strategy = if (replaceExistingScripts) ImportStrategy.REPLACE_ALL else ImportStrategy.APPEND_ALL
            val result = NeoScriptImporter.importScripts(neoScriptFile, project, targetSprite, strategy)

            // Execute added scripts — only if the scene is currently active
            val stageListener = StageActivity.getActiveStageListener()
            if (stageListener != null) {
                val activeScene = ProjectManager.getInstance().getCurrentlyPlayingScene()
                val sceneIsActive = activeScene != null && activeScene.getSceneId() == scene.getSceneId()
                if (sceneIsActive) {
                    for (script in result.added) {
                        stageListener.executeConsoleScript(targetSprite, script)
                    }
                }
                // If inactive: scripts are in the model and will execute when scene starts
            }
        } catch (e: NeoScriptException) {
            Log.e(TAG, "Failed to assign scripts: " + e.message, e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to assign scripts", e)
        }
    }

    private fun resolveScene(project: Project): Scene? {
        val sceneName = targetSceneName
        if (sceneName == null || sceneName.isEmpty()) {
            val current = ProjectManager.getInstance().getCurrentlyPlayingScene()
            if (current != null) return current
            return project.defaultScene
        }
        return project.getSceneByName(sceneName)
    }

    private fun loadNeoScriptFile(path: String): NeoScriptFile = if (path.startsWith("content://")) {
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
    }
}
