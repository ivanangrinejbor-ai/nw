package org.catrobat.catroid.ai.chat

import org.catrobat.catroid.ai.modify.ChangeCard

data class ChatMessage(
    val role: Role,
    val content: String,
    val timestamp: Long,
    val isStreaming: Boolean = false,
    val toolCalls: List<ToolCallInfo> = emptyList(),
    val changeCard: ChangeCard? = null
) {
    enum class Role { USER, ASSISTANT, SYSTEM, TOOL, CHANGE }
}

data class ToolCallInfo(
    val name: String,
    val args: Map<String, String>,
    val result: String? = null
)
