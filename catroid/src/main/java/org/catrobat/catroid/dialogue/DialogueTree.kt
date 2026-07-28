package org.catrobat.catroid.dialogue

import org.json.JSONArray
import org.json.JSONObject

data class DialogueTree(
    val name: String = "",
    val version: Int = 1,
    val nodes: MutableList<DialogueNode> = mutableListOf()
) {
    fun getNode(id: String): DialogueNode? = nodes.find { it.id == id }

    fun getStartNode(): DialogueNode.StartNode? =
        nodes.firstOrNull { it is DialogueNode.StartNode } as? DialogueNode.StartNode

    fun toJson(): String = JSONObject().apply {
        put("version", version)
        put("name", name)
        val arr = JSONArray()
        nodes.forEach { arr.put(it.toJson()) }
        put("nodes", arr)
    }.toString(2)

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val ids = nodes.map { it.id }.toSet()
        val startCount = nodes.count { it is DialogueNode.StartNode }
        if (startCount == 0) errors.add("No Start node")
        if (startCount > 1) errors.add("Multiple Start nodes")

        nodes.forEach { node ->
            val nexts = DialogueNode.quickBarrier(node)
            nexts.forEach { nextId ->
                if (nextId !in ids) {
                    errors.add("Node '${node.id}' references missing node '$nextId'")
                }
            }
            if (node is DialogueNode.DialogueLine && node.text.isBlank() && node.textId.isBlank()) {
                errors.add("Dialogue node '${node.id}' has empty text and no textId")
            }
            if (node is DialogueNode.ChoiceNode && node.choices.isEmpty()) {
                errors.add("Choice node '${node.id}' has no choices")
            }
            if (node is DialogueNode.ConditionNode && node.expression.isBlank()) {
                errors.add("Condition node '${node.id}' has empty expression")
            }
        }

        val reachable = mutableSetOf<String>()
        val startNode = nodes.find { it is DialogueNode.StartNode }
        if (startNode != null) {
            val queue = ArrayDeque<String>()
            queue.add(startNode.id)
            while (queue.isNotEmpty()) {
                val currentId = queue.removeFirst()
                if (currentId in reachable) continue
                reachable.add(currentId)
                val node = getNode(currentId) ?: continue
                DialogueNode.quickBarrier(node).forEach { nextId ->
                    if (nextId !in reachable) queue.add(nextId)
                }
            }
            val unreachable = ids - reachable
            if (unreachable.isNotEmpty()) {
                errors.add("${unreachable.size} unreachable node(s): ${unreachable.take(3).joinToString(", ")}" +
                        if (unreachable.size > 3) "..." else "")
            }
        }

        return errors
    }

    companion object {
        fun fromJson(json: String): DialogueTree {
            val obj = JSONObject(json)
            val tree = DialogueTree(
                name = obj.optString("name", ""),
                version = obj.optInt("version", 1)
            )
            obj.optJSONArray("nodes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    tree.nodes.add(DialogueNode.fromJson(arr.getJSONObject(i)))
                }
            }
            return tree
        }
    }
}
