package org.catrobat.catroid.ai.tool

data class ToolCall(
    val id: String,
    val name: String,
    val args: Map<String, String> = emptyMap()
)

data class ToolResult(
    val success: Boolean,
    val data: String,
    val toolCallId: String
)

interface Tool {
    val name: String
    val description: String
    val parameters: List<ToolParameter>

    suspend fun execute(args: Map<String, String>): ToolResult
}

data class ToolParameter(
    val name: String,
    val type: ParameterType,
    val description: String,
    val required: Boolean = true
)

enum class ParameterType {
    STRING, INTEGER, FLOAT, BOOLEAN
}

data class ProjectChange(
    val type: ChangeType,
    val description: String,
    val data: Map<String, Any> = emptyMap()
)

enum class ChangeType {
    CREATE_OBJECT,
    DELETE_OBJECT,
    CREATE_SCENE,
    DELETE_SCENE,
    REPLACE_SCRIPT,
    APPEND_SCRIPT,
    DELETE_SCRIPT,
    CREATE_VARIABLE,
    DELETE_VARIABLE,
    CREATE_BROADCAST,
    MODIFY_BRICK,
    WIRE_LOCALIZATION_SWITCH
}
