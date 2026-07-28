package org.catrobat.catroid.dialogue

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class DialogueNode {
    abstract val id: String
    abstract val x: Float
    abstract val y: Float

    data class StartNode(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float = 0f,
        override val y: Float = 0f,
        val next: String? = null
    ) : DialogueNode()

    data class DialogueLine(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float = 0f,
        override val y: Float = 0f,
        val textId: String = "",
        val speaker: String = "",
        val portrait: String = "",
        val text: String = "",
        val voiceSound: String = "",
        val typingSpeed: Float = 0.05f,
        val backgroundImage: String = "",
        val next: String? = null
    ) : DialogueNode()

    data class Choice(
        val text: String = "",
        val next: String? = null,
        val visibleCondition: String = "",
        val enableCondition: String = "",
        val icon: String = "",
        val color: String = ""
    )

    data class ChoiceNode(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float = 0f,
        override val y: Float = 0f,
        val choices: MutableList<Choice> = mutableListOf()
    ) : DialogueNode()

    data class ConditionNode(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float = 0f,
        override val y: Float = 0f,
        val expression: String = "",
        val trueNext: String? = null,
        val falseNext: String? = null
    ) : DialogueNode()

    data class ActionEntry(
        val type: String = "",
        val name: String = "",
        val value: String = "",
        val target: String = "",
        val duration: Float = 0f
    )

    data class ActionNode(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float = 0f,
        override val y: Float = 0f,
        val actions: MutableList<ActionEntry> = mutableListOf(),
        val next: String? = null
    ) : DialogueNode()

    data class EndNode(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float = 0f,
        override val y: Float = 0f
    ) : DialogueNode()

    data class CommentNode(
        override val id: String = UUID.randomUUID().toString(),
        override val x: Float = 0f,
        override val y: Float = 0f,
        val text: String = "",
        val color: String = "#808080"
    ) : DialogueNode()

    fun asNextNode(): String? = when (this) {
        is StartNode -> next
        is DialogueLine -> next
        is ActionNode -> next
        else -> null
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", typeName())
        put("x", x.toDouble())
        put("y", y.toDouble())
        when (this@DialogueNode) {
            is StartNode -> next?.let { put("next", it) }
            is DialogueLine -> {
                if (textId.isNotEmpty()) put("textId", textId)
                put("speaker", speaker)
                put("portrait", portrait)
                put("text", text)
                if (voiceSound.isNotEmpty()) put("voiceSound", voiceSound)
                put("typingSpeed", typingSpeed.toDouble())
                if (backgroundImage.isNotEmpty()) put("backgroundImage", backgroundImage)
                next?.let { put("next", it) }
            }
            is ChoiceNode -> {
                val arr = JSONArray()
                choices.forEach { c ->
                    arr.put(JSONObject().apply {
                        put("text", c.text)
                        c.next?.let { put("next", it) }
                        if (c.visibleCondition.isNotEmpty()) put("visibleCondition", c.visibleCondition)
                        if (c.enableCondition.isNotEmpty()) put("enableCondition", c.enableCondition)
                        if (c.icon.isNotEmpty()) put("icon", c.icon)
                        if (c.color.isNotEmpty()) put("color", c.color)
                    })
                }
                put("choices", arr)
            }
            is ConditionNode -> {
                put("expression", expression)
                trueNext?.let { put("trueNext", it) }
                falseNext?.let { put("falseNext", it) }
            }
            is ActionNode -> {
                val arr = JSONArray()
                actions.forEach { a ->
                    arr.put(JSONObject().apply {
                        put("type", a.type)
                        put("name", a.name)
                        put("value", a.value)
                        if (a.target.isNotEmpty()) put("target", a.target)
                        if (a.duration > 0f) put("duration", a.duration.toDouble())
                    })
                }
                put("actions", arr)
                next?.let { put("next", it) }
            }
            is CommentNode -> {
                put("text", text)
                put("color", color)
            }
            is EndNode -> {}
        }
    }

    fun typeName(): String = when (this) {
        is StartNode -> "Start"
        is DialogueLine -> "Dialogue"
        is ChoiceNode -> "Choice"
        is ConditionNode -> "Condition"
        is ActionNode -> "Action"
        is EndNode -> "End"
        is CommentNode -> "Comment"
    }

    companion object {
        fun fromJson(json: JSONObject): DialogueNode {
            val id = json.optString("id", UUID.randomUUID().toString())
            val x = json.optDouble("x", 0.0).toFloat()
            val y = json.optDouble("y", 0.0).toFloat()
            return when (json.optString("type", "Dialogue")) {
                "Start" -> StartNode(id, x, y, json.optString("next", null))
                "Dialogue" -> DialogueLine(
                    id, x, y,
                    json.optString("textId", ""),
                    json.optString("speaker", ""),
                    json.optString("portrait", ""),
                    json.optString("text", ""),
                    json.optString("voiceSound", ""),
                    json.optDouble("typingSpeed", 0.05).toFloat(),
                    json.optString("backgroundImage", ""),
                    json.optString("next", null)
                )
                "Choice" -> {
                    val choices = mutableListOf<Choice>()
                    json.optJSONArray("choices")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val c = arr.getJSONObject(i)
                            choices.add(Choice(
                                c.optString("text", ""),
                                c.optString("next", null),
                                c.optString("visibleCondition", ""),
                                c.optString("enableCondition", ""),
                                c.optString("icon", ""),
                                c.optString("color", "")
                            ))
                        }
                    }
                    ChoiceNode(id, x, y, choices)
                }
                "Condition" -> ConditionNode(
                    id, x, y,
                    json.optString("expression", ""),
                    json.optString("trueNext", null),
                    json.optString("falseNext", null)
                )
                "Action" -> {
                    val actions = mutableListOf<ActionEntry>()
                    json.optJSONArray("actions")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val a = arr.getJSONObject(i)
                            actions.add(ActionEntry(
                                a.optString("type", ""),
                                a.optString("name", ""),
                                a.optString("value", ""),
                                a.optString("target", ""),
                                a.optDouble("duration", 0.0).toFloat()
                            ))
                        }
                    }
                    ActionNode(id, x, y, actions, json.optString("next", null))
                }
                "End" -> EndNode(id, x, y)
                "Comment" -> CommentNode(id, x, y, json.optString("text", ""), json.optString("color", "#808080"))
                else -> DialogueLine(id, x, y, "unknown", "", "", json.toString(), "", 0.05f, "", null)
            }
        }

        fun quickBarrier(node: DialogueNode): Set<String> {
            return when (node) {
                is StartNode -> node.next?.let { setOf(it) } ?: emptySet()
                is DialogueLine -> node.next?.let { setOf(it) } ?: emptySet()
                is ChoiceNode -> node.choices.mapNotNull { it.next }.toSet()
                is ConditionNode -> setOfNotNull(node.trueNext, node.falseNext)
                is ActionNode -> node.next?.let { setOf(it) } ?: emptySet()
                is EndNode -> emptySet()
                is CommentNode -> emptySet()
            }
        }
    }
}
