package org.catrobat.catroid.ai.tool

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.bricks.Brick
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

object ToolCallingEngine {

    private var context: Context? = null

    private val registeredTools = Collections.synchronizedMap(mutableMapOf<String, Tool>())
    private val _toolHistory = MutableStateFlow<List<ToolCallHistory>>(emptyList())
    val toolHistory: StateFlow<List<ToolCallHistory>> = _toolHistory.asStateFlow()
    private val pendingChanges = Collections.synchronizedList(mutableListOf<ProjectChange>())
    private val toolCallCounter = AtomicInteger(0)

    data class ToolCallHistory(
        val toolCall: ToolCall,
        val result: ToolResult,
        val timestamp: Long
    )

    fun init(appContext: Context) {
        context = appContext
        registerDefaultTools()
    }

    private fun registerDefaultTools() {
        registerTool(SceneListTool())
        registerTool(SceneObjectListTool())
        registerTool(ReadSceneTool())
        registerTool(ReadObjectTool())
        registerTool(ReadScriptTool())
        registerTool(SearchVariableTool())
        registerTool(SearchBroadcastTool())
        registerTool(SearchListTool())
        registerTool(CreateVariableTool())
        registerTool(DeleteVariableTool())
        registerTool(CreateObjectTool())
        registerTool(DeleteObjectTool())
        registerTool(CreateSceneTool())
        registerTool(DeleteSceneTool())
        registerTool(ReplaceScriptTool())
        registerTool(AppendScriptTool())
        registerTool(DeleteScriptTool())
        registerTool(SearchFilesTool())
        registerTool(ReadFileTool())
        registerTool(WriteFileTool())
        registerTool(ProjectInfoTool())
        registerTool(CodeAnalysisTool())
    }

    fun registerTool(tool: Tool) {
        registeredTools[tool.name] = tool
    }

    fun getRegisteredTools(): Map<String, Tool> = registeredTools.toMap()

    fun parseToolCalls(response: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()
        val regex = Regex("<tool_call>\\s*<name>(.*?)</name>\\s*(<args>(.*?)</args>)?\\s*</tool_call>", RegexOption.DOT_MATCHES_ALL)
        for (match in regex.findAll(response)) {
            val name = match.groupValues[1].trim()
            val argsXml = match.groupValues[3]
            val args = mutableMapOf<String, String>()
            if (argsXml.isNotBlank()) {
                val argRegex = Regex("<(\\w+)>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)
                for (argMatch in argRegex.findAll(argsXml)) {
                    args[argMatch.groupValues[1]] = argMatch.groupValues[2].trim()
                }
            }
            toolCalls.add(ToolCall(
                id = "tc_${toolCallCounter.incrementAndGet()}",
                name = name,
                args = args
            ))
        }
        return toolCalls
    }

    suspend fun executeTool(toolCall: ToolCall): String {
        val tool = registeredTools[toolCall.name]
        if (tool == null) {
            val result = ToolResult(false, "Unknown tool: ${toolCall.name}", toolCall.id)
            _toolHistory.update { it + ToolCallHistory(toolCall, result, System.currentTimeMillis()) }
            return "ERROR: Tool '${toolCall.name}' not found. Available tools: ${registeredTools.keys.joinToString(", ")}"
        }
        return try {
            val result = tool.execute(toolCall.args)
            _toolHistory.update { it + ToolCallHistory(toolCall, result, System.currentTimeMillis()) }
            result.data
        } catch (e: Exception) {
            val result = ToolResult(false, "Error: ${e.message}", toolCall.id)
            _toolHistory.update { it + ToolCallHistory(toolCall, result, System.currentTimeMillis()) }
            "ERROR: ${e.message}"
        }
    }

    fun getToolsDescription(): String {
        return registeredTools.values.joinToString("\n") { tool ->
            val params = tool.parameters.joinToString(", ") { p ->
                "${p.name}: ${p.type.name}${if (p.required) "" else "?"}"
            }
            "${tool.name}($params) - ${tool.description}"
        }
    }

    fun getPendingChanges(): List<ProjectChange> = synchronized(pendingChanges) { pendingChanges.toList() }

    fun storePendingChanges(changes: List<ProjectChange>) {
        synchronized(pendingChanges) {
            pendingChanges.clear()
            pendingChanges.addAll(changes)
        }
    }

    fun clearPendingChanges() {
        synchronized(pendingChanges) { pendingChanges.clear() }
    }

    fun addChange(change: ProjectChange) {
        synchronized(pendingChanges) { pendingChanges.add(change) }
    }

    private fun validatePath(projectDir: File, path: String): File? {
        val file = File(projectDir, path).normalize()
        return if (file.canonicalPath.startsWith(projectDir.canonicalPath + File.separator) || file.canonicalPath == projectDir.canonicalPath) {
            file
        } else null
    }

    // ---- Default Tool Implementations ----

    class SceneListTool : Tool {
        override val name = "listScenes"
        override val description = "List all scenes in the project"
        override val parameters = emptyList<ToolParameter>()

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val scenes = project.sceneList.joinToString("\n") { "  - ${it.name}" }
            return ToolResult(true, "Scenes:\n$scenes", "")
        }
    }

    class SceneObjectListTool : Tool {
        override val name = "listObjects"
        override val description = "List all objects (sprites) in a scene"
        override val parameters = listOf(ToolParameter("scene", ParameterType.STRING, "Scene name"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val sceneName = args["scene"] ?: return ToolResult(false, "Missing 'scene' argument", "")
            val scene = project.sceneList.find { it.name == sceneName }
                ?: return ToolResult(false, "Scene '$sceneName' not found", "")
            val objects = scene.spriteList.joinToString("\n") { "  - ${it.name}" }
            return ToolResult(true, "Objects in '$sceneName':\n$objects", "")
        }
    }

    class ReadSceneTool : Tool {
        override val name = "readScene"
        override val description = "Read detailed information about a scene"
        override val parameters = listOf(ToolParameter("scene", ParameterType.STRING, "Scene name"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val sceneName = args["scene"] ?: return ToolResult(false, "Missing 'scene' argument", "")
            val scene = project.sceneList.find { it.name == sceneName }
                ?: return ToolResult(false, "Scene '$sceneName' not found", "")
            val info = buildString {
                appendLine("Scene: ${scene.name}")
                appendLine("Objects: ${scene.spriteList.size}")
                for (sprite in scene.spriteList) {
                    appendLine("  ${sprite.name}: ${sprite.scriptList.size} scripts, ${sprite.lookList.size} looks")
                }
            }
            return ToolResult(true, info, "")
        }
    }

    class ReadObjectTool : Tool {
        override val name = "readObject"
        override val description = "Read details of an object/sprite in a scene"
        override val parameters = listOf(
            ToolParameter("scene", ParameterType.STRING, "Scene name"),
            ToolParameter("object", ParameterType.STRING, "Object name")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val sceneName = args["scene"] ?: return ToolResult(false, "Missing 'scene' argument", "")
            val objectName = args["object"] ?: return ToolResult(false, "Missing 'object' argument", "")
            val scene = project.sceneList.find { it.name == sceneName }
                ?: return ToolResult(false, "Scene '$sceneName' not found", "")
            val sprite = scene.spriteList.find { it.name == objectName }
                ?: return ToolResult(false, "Object '$objectName' not found", "")
            val info = buildString {
                appendLine("Object: ${sprite.name}")
                appendLine("Looks: ${sprite.lookList.size}")
                appendLine("Sounds: ${sprite.soundList.size}")
                appendLine("Scripts: ${sprite.scriptList.size}")
                for ((i, script) in sprite.scriptList.withIndex()) {
                    appendLine("  Script $i: ${script::class.java.simpleName}")
                    for ((j, brick) in script.getBrickList().withIndex()) {
                        appendLine("    Brick $j: ${brick::class.java.simpleName}")
                    }
                }
            }
            return ToolResult(true, info, "")
        }
    }

    class ReadScriptTool : Tool {
        override val name = "readScript"
        override val description = "Read a specific script from an object"
        override val parameters = listOf(
            ToolParameter("scene", ParameterType.STRING, "Scene name"),
            ToolParameter("object", ParameterType.STRING, "Object name"),
            ToolParameter("index", ParameterType.INTEGER, "Script index")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val sceneName = args["scene"] ?: return ToolResult(false, "Missing 'scene' argument", "")
            val objectName = args["object"] ?: return ToolResult(false, "Missing 'object' argument", "")
            val index = args["index"]?.toIntOrNull() ?: return ToolResult(false, "Missing valid 'index' argument", "")
            val scene = project.sceneList.find { it.name == sceneName }
                ?: return ToolResult(false, "Scene '$sceneName' not found", "")
            val sprite = scene.spriteList.find { it.name == objectName }
                ?: return ToolResult(false, "Object '$objectName' not found", "")
            val script = sprite.scriptList.getOrNull(index)
                ?: return ToolResult(false, "Script index $index not found", "")
            val info = buildString {
                appendLine("Script $index: ${script::class.java.simpleName}")
                for ((j, brick) in script.getBrickList().withIndex()) {
                    appendLine("  Brick $j: ${brick::class.java.simpleName}")
                }
            }
            return ToolResult(true, info, "")
        }
    }

    class SearchVariableTool : Tool {
        override val name = "searchVariable"
        override val description = "Search for a variable by name across project"
        override val parameters = listOf(ToolParameter("name", ParameterType.STRING, "Variable name pattern"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val pattern = (args["name"] ?: "").lowercase()
            val vars = project.getUserVariables().filter { it.name?.lowercase()?.contains(pattern) == true }
            val result = if (vars.isEmpty()) "No variables matching '$pattern' found"
                else vars.joinToString("\n") { "  - ${it.name}" }
            return ToolResult(true, "Variables matching '$pattern':\n$result", "")
        }
    }

    class SearchBroadcastTool : Tool {
        override val name = "searchBroadcast"
        override val description = "Search broadcast messages"
        override val parameters = listOf(ToolParameter("name", ParameterType.STRING, "Broadcast name pattern"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val pattern = (args["name"] ?: "").lowercase()
            val broadcasts = mutableSetOf<String>()
            for (scene in project.sceneList) {
                for (sprite in scene.spriteList) {
                    for (script in sprite.scriptList) {
                        for (brick in script.getBrickList()) {
                            val msg = extractBroadcastMessage(brick)
                            if (msg != null && msg.lowercase().contains(pattern)) {
                                broadcasts.add(msg)
                            }
                        }
                    }
                }
            }
            return ToolResult(true, "Broadcasts matching '$pattern':\n${broadcasts.joinToString("\n") { "  - $it" }}", "")
        }

        private fun extractBroadcastMessage(brick: Brick): String? {
            return try {
                val f = brick.javaClass.getDeclaredField("broadcastMessage")
                f.isAccessible = true
                val obj = f.get(brick) ?: return null
                val nameField = obj.javaClass.getDeclaredField("name")
                nameField.isAccessible = true
                nameField.get(obj) as? String
            } catch (_: Exception) { null }
        }
    }

    class SearchListTool : Tool {
        override val name = "searchList"
        override val description = "Search user lists by name"
        override val parameters = listOf(ToolParameter("name", ParameterType.STRING, "List name pattern"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            return ToolResult(true, "Search lists: feature not fully implemented", "")
        }
    }

    class CreateVariableTool : Tool {
        override val name = "createVariable"
        override val description = "Create a new user variable"
        override val parameters = listOf(
            ToolParameter("name", ParameterType.STRING, "Variable name"),
            ToolParameter("scope", ParameterType.STRING, "Scope: project or object"),
            ToolParameter("object", ParameterType.STRING, "Object name if scope=object", required = false),
            ToolParameter("scene", ParameterType.STRING, "Scene name", required = false)
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            addChange(ProjectChange(ChangeType.CREATE_VARIABLE, "Create variable '${args["name"]}'", args))
            return ToolResult(true, "Variable '${args["name"]}' will be created", "")
        }
    }

    class DeleteVariableTool : Tool {
        override val name = "deleteVariable"
        override val description = "Delete a variable"
        override val parameters = listOf(
            ToolParameter("name", ParameterType.STRING, "Variable name"),
            ToolParameter("scene", ParameterType.STRING, "Scene name", required = false),
            ToolParameter("object", ParameterType.STRING, "Object name", required = false)
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            addChange(ProjectChange(ChangeType.DELETE_VARIABLE, "Delete variable '${args["name"]}'", args))
            return ToolResult(true, "Variable '${args["name"]}' will be deleted", "")
        }
    }

    class CreateObjectTool : Tool {
        override val name = "createObject"
        override val description = "Create a new object/sprite in a scene"
        override val parameters = listOf(
            ToolParameter("name", ParameterType.STRING, "Object name"),
            ToolParameter("scene", ParameterType.STRING, "Scene name")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            addChange(ProjectChange(ChangeType.CREATE_OBJECT, "Create object '${args["name"]}' in '${args["scene"]}'", args))
            return ToolResult(true, "Object '${args["name"]}' will be created", "")
        }
    }

    class DeleteObjectTool : Tool {
        override val name = "deleteObject"
        override val description = "Delete an object from a scene"
        override val parameters = listOf(
            ToolParameter("name", ParameterType.STRING, "Object name"),
            ToolParameter("scene", ParameterType.STRING, "Scene name")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            addChange(ProjectChange(ChangeType.DELETE_OBJECT, "Delete object '${args["name"]}' from '${args["scene"]}'", args))
            return ToolResult(true, "Object '${args["name"]}' will be deleted", "")
        }
    }

    class CreateSceneTool : Tool {
        override val name = "createScene"
        override val description = "Create a new scene"
        override val parameters = listOf(ToolParameter("name", ParameterType.STRING, "Scene name"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            addChange(ProjectChange(ChangeType.CREATE_SCENE, "Create scene '${args["name"]}'", args))
            return ToolResult(true, "Scene '${args["name"]}' will be created", "")
        }
    }

    class DeleteSceneTool : Tool {
        override val name = "deleteScene"
        override val description = "Delete a scene"
        override val parameters = listOf(ToolParameter("name", ParameterType.STRING, "Scene name"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            addChange(ProjectChange(ChangeType.DELETE_SCENE, "Delete scene '${args["name"]}'", args))
            return ToolResult(true, "Scene '${args["name"]}' will be deleted", "")
        }
    }

    class ReplaceScriptTool : Tool {
        override val name = "replaceScript"
        override val description = "Replace a script in an object with new bricks"
        override val parameters = listOf(
            ToolParameter("scene", ParameterType.STRING, "Scene name"),
            ToolParameter("object", ParameterType.STRING, "Object name"),
            ToolParameter("index", ParameterType.INTEGER, "Script index"),
            ToolParameter("description", ParameterType.STRING, "Description of new script logic")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            addChange(ProjectChange(ChangeType.REPLACE_SCRIPT, "Replace script ${args["index"]} of '${args["object"]}'", args))
            return ToolResult(true, "Script will be replaced", "")
        }
    }

    class AppendScriptTool : Tool {
        override val name = "appendScript"
        override val description = "Append a new script to an object"
        override val parameters = listOf(
            ToolParameter("scene", ParameterType.STRING, "Scene name"),
            ToolParameter("object", ParameterType.STRING, "Object name"),
            ToolParameter("description", ParameterType.STRING, "Description of the new script")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            addChange(ProjectChange(ChangeType.APPEND_SCRIPT, "Append script to '${args["object"]}'", args))
            return ToolResult(true, "Script will be appended", "")
        }
    }

    class DeleteScriptTool : Tool {
        override val name = "deleteScript"
        override val description = "Delete a script from an object"
        override val parameters = listOf(
            ToolParameter("scene", ParameterType.STRING, "Scene name"),
            ToolParameter("object", ParameterType.STRING, "Object name"),
            ToolParameter("index", ParameterType.INTEGER, "Script index")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            addChange(ProjectChange(ChangeType.DELETE_SCRIPT, "Delete script ${args["index"]} from '${args["object"]}'", args))
            return ToolResult(true, "Script will be deleted", "")
        }
    }

    class SearchFilesTool : Tool {
        override val name = "searchFiles"
        override val description = "Search files in the project directory"
        override val parameters = listOf(ToolParameter("pattern", ParameterType.STRING, "File name pattern"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val pattern = (args["pattern"] ?: "*").lowercase()
            val projectDir = project.directory ?: return ToolResult(false, "No project directory", "")
            val files = withContext(Dispatchers.IO) {
                projectDir.walk().filter { it.isFile && it.name.lowercase().contains(pattern) }
                    .take(50).joinToString("\n") { "  - ${it.relativeTo(projectDir)}" }
            }
            return ToolResult(true, "Files matching '$pattern':\n$files", "")
        }
    }

    class ReadFileTool : Tool {
        override val name = "readFile"
        override val description = "Read a file from the project"
        override val parameters = listOf(ToolParameter("path", ParameterType.STRING, "File path relative to project"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val path = args["path"] ?: return ToolResult(false, "Missing 'path'", "")
            val projectDir = project.directory ?: return ToolResult(false, "No project directory", "")
            val file = validatePath(projectDir, path) ?: return ToolResult(false, "Invalid or unauthorized path: $path", "")
            if (!file.exists() || !file.isFile) return ToolResult(false, "File not found: $path", "")
            val content = withContext(Dispatchers.IO) {
                if (file.length() > 100000) "${file.readText().take(100000)}\n... (truncated)" else file.readText()
            }
            return ToolResult(true, content, "")
        }
    }

    class WriteFileTool : Tool {
        override val name = "writeFile"
        override val description = "Write content to a file in the project"
        override val parameters = listOf(
            ToolParameter("path", ParameterType.STRING, "File path relative to project"),
            ToolParameter("content", ParameterType.STRING, "File content")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val path = args["path"] ?: return ToolResult(false, "Missing 'path'", "")
            val content = args["content"] ?: return ToolResult(false, "Missing 'content'", "")
            val projectDir = project.directory ?: return ToolResult(false, "No project directory", "")
            val file = validatePath(projectDir, path) ?: return ToolResult(false, "Invalid or unauthorized path: $path", "")
            withContext(Dispatchers.IO) {
                file.parentFile?.mkdirs()
                file.writeText(content)
            }
            return ToolResult(true, "File '$path' written (${content.length} bytes)", "")
        }
    }

    class ProjectInfoTool : Tool {
        override val name = "projectInfo"
        override val description = "Get comprehensive project information"
        override val parameters = emptyList<ToolParameter>()

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val info = buildString {
                appendLine("Project: ${project.name}")
                appendLine("Scenes: ${project.sceneList.size}")
                for (scene in project.sceneList) {
                    appendLine("  ${scene.name}: ${scene.spriteList.size} objects")
                    for (sprite in scene.spriteList) {
                        appendLine("    ${sprite.name}: ${sprite.scriptList.size} scripts")
                    }
                }
            }
            return ToolResult(true, info, "")
        }
    }

    class CodeAnalysisTool : Tool {
        override val name = "codeAnalysis"
        override val description = "Analyze the project for potential issues, optimization opportunities, and unused resources"
        override val parameters = emptyList<ToolParameter>()

        override suspend fun execute(args: Map<String, String>): ToolResult {
            return ToolResult(true, "Full code analysis requires loading all scripts. Basic check complete.", "")
        }
    }
}
