package org.catrobat.catroid.dialogue

import java.io.File

object DialogueSerializer {

    fun serialize(dialogue: DialogueTree): String = dialogue.toJson()

    fun deserialize(json: String): DialogueTree = DialogueTree.fromJson(json)

    fun saveToFile(dialogue: DialogueTree, file: File) {
        file.parentFile?.mkdirs()
        file.writeText(serialize(dialogue))
    }

    fun loadFromFile(file: File): DialogueTree {
        return deserialize(file.readText())
    }
}
