package org.catrobat.catroid.dialogue

data class DialogueState(
    var tree: DialogueTree? = null,
    var currentNodeId: String? = null,
    var isRunning: Boolean = false,
    var lastChoiceText: String = "",
    var currentSpeaker: String = "",
    var currentDialogueText: String = "",
    var variables: MutableMap<String, Any> = mutableMapOf()
) {
    fun reset() {
        currentNodeId = null
        isRunning = false
        lastChoiceText = ""
        currentSpeaker = ""
        currentDialogueText = ""
    }

    fun getNode(): DialogueNode? = tree?.let { t -> currentNodeId?.let { t.getNode(it) } }
}
