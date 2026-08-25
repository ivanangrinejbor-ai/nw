package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SaveGameAction : TemporalAction() {
    var scope: Scope? = null
    var slot: Formula? = null
    private var started = false

    override fun update(percent: Float) {
        if (started) return
        started = true
        val currentScope = scope ?: return
        try {
            val slotValue = slot?.interpretDouble(currentScope)?.toInt() ?: return
            val json = buildSnapshot() ?: return
            saveGameFile(slotValue, json)
        } catch (e: Exception) {
            Log.e(TAG, "SaveGame failed", e)
        }
    }

    private fun buildSnapshot(): JSONObject? {
        val pm = ProjectManager.getInstance()
        val project = pm.currentProject ?: return null
        val scene = pm.currentlyPlayingScene
            ?: pm.currentlyEditedScene
            ?: project.defaultScene
                ?: return null
        val root = JSONObject()
        root.put("projectName", project.name)
        root.put("sceneName", scene.name ?: "")

        val variables = JSONObject()
        for (variable in project.userVariables) {
            variables.put(variable.name ?: continue, stringify(variable.value))
        }
        for (userList in project.userLists) {
            variables.put(userList.name ?: continue, listToJson(userList))
        }
        root.put("projectVariables", variables)

        val spritesObject = JSONObject()
        for (sprite in scene.spriteList) {
            val spriteObject = JSONObject()
            for (variable in sprite.userVariables) {
                spriteObject.put(variable.name ?: continue, stringify(variable.value))
            }
            val lists = JSONArray()
            for (userList in sprite.userLists) {
                val entry = JSONObject()
                entry.put("name", userList.name)
                entry.put("items", listToJson(userList))
                lists.put(entry)
            }
            if (lists.length() > 0) {
                spriteObject.put("lists", lists)
            }
            spritesObject.put(sprite.name, spriteObject)
        }
        root.put("sprites", spritesObject)
        return root
    }

    private fun listToJson(userList: UserList): JSONArray {
        val array = JSONArray()
        for (item in userList.value ?: emptyList()) {
            array.put(stringify(item))
        }
        return array
    }

    private fun stringify(value: Any?): String = when (value) {
        null -> ""
        is Double -> if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()
        is Float -> stringify(value.toDouble())
        else -> value.toString()
    }

    companion object {
        private val TAG = SaveGameAction::class.java.simpleName

        @JvmStatic
        fun saveGameFile(slot: Int, json: JSONObject): File? {
            val file = gameFile(slot) ?: return null
            file.parentFile?.mkdirs()
            file.writeText(json.toString())
            return file
        }

        @JvmStatic
        fun gameFile(slot: Int): File? {
            val appContext = CatroidApplication.getAppContext() ?: return null
            val dir = appContext.getExternalFilesDir(null) ?: appContext.filesDir ?: return null
            val safeSlot = if (slot < 1) 1 else if (slot > 99) 99 else slot
            return File(File(dir, "savegames"), "savegame_slot$safeSlot.json")
        }
    }
}
