package org.catrobat.catroid.ai.chat

data class ChatMessage(
    val role: Role,
    val content: String,
    val timestamp: Long,
    val isStreaming: Boolean = false,
    val toolCalls: List<ToolCallInfo> = emptyList()
) {
    enum class Role { USER, ASSISTANT, SYSTEM, TOOL }
}

data class ToolCallInfo(
    val name: String,
    val args: Map<String, String>,
    val result: String? = null
)
