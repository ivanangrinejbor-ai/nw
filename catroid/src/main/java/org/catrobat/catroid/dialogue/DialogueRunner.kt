package org.catrobat.catroid.dialogue

class DialogueRunner(
    val state: DialogueState = DialogueState()
) {
    var onShowDialogue: ((speaker: String, text: String, portrait: String?, typingSpeed: Float) -> Unit)? = null
    var onShowChoices: ((choices: List<DialogueNode.Choice>) -> Unit)? = null
    var onDialogueEnd: (() -> Unit)? = null
    var onExecuteAction: ((action: DialogueNode.ActionEntry) -> Unit)? = null
    var onVariableRequest: ((name: String) -> Any?)? = null

    fun load(tree: DialogueTree) {
        state.tree = tree
        state.reset()
    }

    fun start(startNodeId: String? = null) {
        val tree = state.tree ?: return
        val node = if (startNodeId != null) tree.getNode(startNodeId)
            else tree.getStartNode()
        if (node == null) return
        state.isRunning = true
        processNode(node)
    }

    fun selectChoice(choiceIndex: Int) {
        if (!state.isRunning) return
        val node = state.getNode()
        if (node !is DialogueNode.ChoiceNode) return
        if (choiceIndex < 0 || choiceIndex >= node.choices.size) return

        val choice = node.choices[choiceIndex]
        if (!evaluateCondition(choice.visibleCondition)) return
        if (choice.enableCondition.isNotEmpty() && !evaluateCondition(choice.enableCondition)) return

        state.lastChoiceText = choice.text
        val varText = DialogueVariableResolver.resolve(choice.text, state.variables)
        state.lastChoiceText = varText

        choice.next?.let { jumpTo(it) } ?: endDialogue()
    }

    fun jumpTo(nodeId: String) {
        if (!state.isRunning) return
        val tree = state.tree ?: return
        val node = tree.getNode(nodeId) ?: return
        processNode(node)
    }

    fun endDialogue() {
        state.isRunning = false
        state.currentNodeId = null
        onDialogueEnd?.invoke()
    }

    fun isRunning(): Boolean = state.isRunning

    fun getCurrentSpeaker(): String = state.currentSpeaker

    fun getCurrentText(): String = state.currentDialogueText

    fun getCurrentNodeId(): String = state.currentNodeId ?: ""

    fun getSelectedChoice(): String = state.lastChoiceText

    fun setVariable(name: String, value: Any) {
        state.variables[name] = value
    }

    fun getVariable(name: String): Any? {
        onVariableRequest?.let { cb ->
            val value = cb(name)
            if (value != null) return value
        }
        return state.variables[name]
    }

    fun getAllVariables(): Map<String, Any> = state.variables.toMap()

    private fun processNode(node: DialogueNode) {
        state.currentNodeId = node.id
        when (node) {
            is DialogueNode.StartNode -> {
                node.next?.let { jumpTo(it) } ?: endDialogue()
            }
            is DialogueNode.DialogueLine -> {
                val text = DialogueVariableResolver.resolve(node.text, state.variables)
                state.currentSpeaker = node.speaker
                state.currentDialogueText = text
                val portrait = node.portrait.ifEmpty { null }
                onShowDialogue?.invoke(node.speaker, text, portrait, node.typingSpeed)
            }
            is DialogueNode.ChoiceNode -> {
                val availableChoices = node.choices.filter { evaluateCondition(it.visibleCondition) }
                state.currentDialogueText = ""
                onShowChoices?.invoke(availableChoices)
            }
            is DialogueNode.ConditionNode -> {
                val result = evaluateCondition(node.expression)
                val nextId = if (result) node.trueNext else node.falseNext
                nextId?.let { jumpTo(it) } ?: endDialogue()
            }
            is DialogueNode.ActionNode -> {
                node.actions.forEach { action ->
                    executeAction(action)
                }
                node.next?.let { jumpTo(it) } ?: endDialogue()
            }
            is DialogueNode.EndNode -> {
                endDialogue()
            }
            is DialogueNode.CommentNode -> {
                endDialogue()
            }
        }
    }

    fun evaluateCondition(expression: String): Boolean {
        if (expression.isBlank()) return true
        val resolved = DialogueVariableResolver.resolve(expression, state.variables)

        return try {
            when {
                resolved.equals("true", ignoreCase = true) -> true
                resolved.equals("false", ignoreCase = true) -> false
                resolved.contains(">=") -> {
                    val parts = resolved.split(">=").map { it.trim().toDouble() }
                    parts[0] >= parts[1]
                }
                resolved.contains("<=") -> {
                    val parts = resolved.split("<=").map { it.trim().toDouble() }
                    parts[0] <= parts[1]
                }
                resolved.contains("!=") -> {
                    val parts = resolved.split("!=").map { it.trim() }
                    parts[0] != parts[1]
                }
                resolved.contains("==") -> {
                    val parts = resolved.split("==").map { it.trim() }
                    parts[0] == parts[1]
                }
                resolved.contains(" contains ") -> {
                    val parts = resolved.split(" contains ").map { it.trim().removeSurrounding("\"") }
                    parts[0].contains(parts[1])
                }
                resolved.contains(">") -> {
                    val parts = resolved.split(">").map { it.trim().toDouble() }
                    parts[0] > parts[1]
                }
                resolved.contains("<") -> {
                    val parts = resolved.split("<").map { it.trim().toDouble() }
                    parts[0] < parts[1]
                }
                else -> resolved.toDoubleOrNull()?.let { it != 0.0 } ?: resolved.isNotEmpty()
            }
        } catch (e: Exception) {
            false
        }
    }

    fun continueAfterDialogue() {
        if (!state.isRunning) return
        val node = state.getNode()
        if (node is DialogueNode.DialogueLine) {
            node.next?.let { jumpTo(it) } ?: endDialogue()
        }
    }

    private fun executeAction(action: DialogueNode.ActionEntry) {
        when (action.type) {
            "setVariable" -> {
                val current = (getVariable(action.name) as? Number)?.toDouble() ?: 0.0
                val value = action.value
                val newValue = when {
                    value.startsWith("+") -> current + (value.substring(1).toDoubleOrNull() ?: 0.0)
                    value.startsWith("-") -> current - (value.substring(1).toDoubleOrNull() ?: 0.0)
                    value.startsWith("*") -> current * (value.substring(1).toDoubleOrNull() ?: 1.0)
                    else -> value.toDoubleOrNull() ?: value
                }
                setVariable(action.name, newValue)
            }
            "giveItem" -> setVariable("inventory_${action.name}", true)
            "removeItem" -> setVariable("inventory_${action.name}", false)
        }
        onExecuteAction?.invoke(action)
    }
}
