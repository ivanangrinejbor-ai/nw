package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.json.JSONArray
import org.json.JSONObject

class LoadGameAction : TemporalAction() {
    var scope: Scope? = null
    var slot: Formula? = null
    private var started = false

    override fun update(percent: Float) {
        if (started) return
        started = true
        val currentScope = scope ?: return
        try {
            val slotValue = slot?.interpretDouble(currentScope)?.toInt() ?: return
            val file = SaveGameAction.gameFile(slotValue) ?: return
            if (!file.exists()) return
            val root = JSONObject(file.readText())
            applySnapshot(root)
        } catch (e: Exception) {
            Log.e(TAG, "LoadGame failed", e)
        }
    }

    private fun applySnapshot(root: JSONObject) {
        val pm = ProjectManager.getInstance()
        val project = pm.currentProject ?: return
        val sceneName = root.optString("sceneName", "")
        var scene: Scene? = null
        if (sceneName.isNotEmpty()) {
            for (candidate in project.sceneList) {
                if (candidate.name == sceneName) {
                    scene = candidate
                    break
                }
            }
        }
        val targetScene = scene
            ?: pm.currentlyPlayingScene
            ?: pm.currentlyEditedScene
            ?: project.defaultScene
            ?: return

        val projectVariables = root.optJSONObject("projectVariables")
        if (projectVariables != null) {
            for (variable in project.userVariables) {
                if (projectVariables.has(variable.name)) {
                    variable.value = parseValue(projectVariables.opt(variable.name))
                }
            }
            for (userList in project.userLists) {
                val stored = projectVariables.optJSONArray(userList.name)
                if (stored != null) {
                    applyList(userList, stored)
                }
            }
        }

        val spritesObject = root.optJSONObject("sprites") ?: return
        for (sprite in targetScene.spriteList) {
            val spriteObject = spritesObject.optJSONObject(sprite.name) ?: continue
            for (variable in sprite.userVariables) {
                if (spriteObject.has(variable.name)) {
                    variable.value = parseValue(spriteObject.opt(variable.name))
                }
            }
            val lists = spriteObject.optJSONArray("lists") ?: continue
            for (i in 0 until lists.length()) {
                val listEntry = lists.optJSONObject(i) ?: continue
                val name = listEntry.optString("name")
                val stored = listEntry.optJSONArray("items") ?: continue
                for (userList in sprite.userLists) {
                    if (userList.name == name) {
                        applyList(userList, stored)
                    }
                }
            }
        }
    }

    private fun applyList(userList: org.catrobat.catroid.formulaeditor.UserList, stored: JSONArray) {
        val restored = ArrayList<Any>()
        for (i in 0 until stored.length()) {
            restored.add(parseValue(stored.opt(i)))
        }
        userList.value?.clear()
        userList.value?.addAll(restored)
    }

    private fun parseValue(raw: Any?): Any = when (raw) {
        is Number -> raw.toDouble()
        is String -> raw.toLongOrNull()?.toDouble() ?: raw.toDoubleOrNull() ?: raw
        else -> raw ?: ""
    }

    companion object {
        private val TAG = LoadGameAction::class.java.simpleName
    }
}
