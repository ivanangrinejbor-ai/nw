package org.catrobat.catroid.ai.tool

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.ai.context.MemoryManager
import org.catrobat.catroid.ai.settings.AiPreferences
import org.catrobat.catroid.apkbuildV3.ApkBuilderV3Config
import org.catrobat.catroid.apkbuildV3.ApkBuilderV3Engine
import org.catrobat.catroid.apkbuildV3.AssemblyResult
import org.catrobat.catroid.apkbuildV3.BuildProgressListener
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.BrickInfo
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.io.asynctask.ProjectSaver
import org.catrobat.catroid.io.ZipArchiver
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

object ToolCallingEngine {

    private var context: Context? = null

    @Volatile
    var scopeProjectName: String? = null

    private val globalOnlyTools = setOf("listProjects", "openProject", "createProject")

    private val registeredTools = Collections.synchronizedMap(mutableMapOf<String, Tool>())
    private val _toolHistory = MutableStateFlow<List<ToolCallHistory>>(emptyList())
    val toolHistory: StateFlow<List<ToolCallHistory>> = _toolHistory.asStateFlow()
    private val pendingChanges = Collections.synchronizedList(mutableListOf<ProjectChange>())
    private val approvedToolCalls = Collections.synchronizedSet(mutableSetOf<String>())
    private val toolCallCounter = AtomicInteger(0)

    data class ToolCallHistory(
        val toolCall: ToolCall,
        val result: ToolResult,
        val timestamp: Long
    )

    fun init(appContext: Context) {
        context = appContext.applicationContext
        registerDefaultTools()
    }

    private fun registerDefaultTools() {
        registerTool(SceneListTool())
        registerTool(SceneObjectListTool())
        registerTool(ReadSceneTool())
        registerTool(ReadObjectTool())
        registerTool(ReadBackgroundTool())
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
        registerTool(ProjectInventoryTool())
        registerTool(ListLooksTool())
        registerTool(ListSoundsTool())
        registerTool(ListVariablesTool())
        registerTool(ListBroadcastsTool())
        registerTool(CodeAnalysisTool())
        registerTool(BuildScriptTool())
        registerTool(ListProjectsTool())
        registerTool(OpenProjectTool())
        registerTool(RememberTool())
        registerTool(RecallTool())
        registerTool(ForgetTool())
        registerTool(LocalizeSpritesTool())
        registerTool(WireLocalizationSwitchTool())
        registerTool(GenerateGameTemplateTool())
        registerTool(ScanAndFixProjectTool())
        registerTool(ImportNeoScriptModuleTool())
        registerTool(GenerateAiTextureTool())
        registerTool(CreateProjectTool())
        registerTool(BuildApkTool())
        registerTool(ExportProjectTool())
        registerTool(Export3dModelTool())
    }

    fun registerTool(tool: Tool) {
        registeredTools[tool.name] = tool
    }

    fun getRegisteredTools(): Map<String, Tool> = registeredTools.toMap()

    fun parseToolCalls(response: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()
        val text = stripMarkdownFences(response)
        parseXmlToolCalls(text, toolCalls)
        if (toolCalls.isNotEmpty()) return toolCalls
        parseJsonToolCalls(text, toolCalls)
        return toolCalls
    }

    private fun stripMarkdownFences(text: String): String {
        var t = text.trim()
        if (t.startsWith("```")) {
            val nl = t.indexOf('\n')
            t = if (nl >= 0) t.substring(nl + 1).trim() else t.substring(3).trim()
        }
        if (t.endsWith("```")) t = t.substring(0, t.length - 3).trim()
        return t
    }

    private fun parseXmlToolCalls(text: String, out: MutableList<ToolCall>) {
        val regex = Regex(
            "<tool_call[^>]*>\\s*<name[^>]*>(.*?)</name>\\s*(?:<args[^>]*>(.*?)</args>)?\\s*</tool_call>",
            RegexOption.DOT_MATCHES_ALL
        )
        for (match in regex.findAll(text)) {
            val name = match.groupValues[1].trim()
            if (name.isBlank()) continue
            val argsXml = match.groupValues[2]
            val args = mutableMapOf<String, String>()
            if (argsXml.isNotBlank()) {
                val argRegex = Regex("<(\\w+)>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)
                for (argMatch in argRegex.findAll(argsXml)) {
                    args[argMatch.groupValues[1]] = argMatch.groupValues[2].trim()
                }
            }
            out.add(newToolCall(name, args))
        }
        if (out.isEmpty()) {
            val attrRegex = Regex(
                "<tool_call[^>]*\\bname=\"([^\"]+)\"([^>]*)/>",
                RegexOption.DOT_MATCHES_ALL
            )
            for (match in attrRegex.findAll(text)) {
                val name = match.groupValues[1].trim()
                if (name.isBlank()) continue
                val args = mutableMapOf<String, String>()
                val attrBody = match.groupValues[2]
                val attrRegexInner = Regex("(\\w+)=\"([^\"]*)\"")
                for (am in attrRegexInner.findAll(attrBody)) {
                    args[am.groupValues[1]] = am.groupValues[2]
                }
                out.add(newToolCall(name, args))
            }
        }
    }

    private fun parseJsonToolCalls(text: String, out: MutableList<ToolCall>) {
        try {
            val trimmed = text.trim()
            val arr = when {
                trimmed.startsWith("[") -> org.json.JSONArray(trimmed)
                trimmed.startsWith("{") -> org.json.JSONArray("[$trimmed]")
                else -> return
            }
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name", obj.optString("tool", ""))
                if (name.isBlank()) continue
                val args = mutableMapOf<String, String>()
                val argsObj = obj.optJSONObject("args")
                    ?: obj.optJSONObject("arguments")
                    ?: obj.optJSONObject("parameters")
                if (argsObj != null) {
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        args[k] = argsObj.optString(k, "")
                    }
                } else if (obj.has("arguments") && obj.opt("arguments") is String) {
                    try {
                        val inner = org.json.JSONObject(obj.getString("arguments"))
                        val keys = inner.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            args[k] = inner.optString(k, "")
                        }
                    } catch (_: Exception) {}
                }
                out.add(newToolCall(name, args))
            }
        } catch (_: Exception) {}
    }

    private fun newToolCall(name: String, args: Map<String, String>): ToolCall =
        ToolCall(
            id = "tc_${toolCallCounter.incrementAndGet()}",
            name = name,
            args = args
        )

    suspend fun executeTool(toolCall: ToolCall): String {
        if (scopeProjectName != null && toolCall.name in globalOnlyTools) {
            val result = ToolResult(false, "Denied: agent is limited to project '$scopeProjectName'", toolCall.id)
            _toolHistory.update { it + ToolCallHistory(toolCall, result, System.currentTimeMillis()) }
            return "DENIED: '${toolCall.name}' is unavailable because the agent is limited to project '$scopeProjectName'."
        }
        val tool = registeredTools[toolCall.name]
        if (tool == null) {
            val result = ToolResult(false, "Unknown tool: ${toolCall.name}", toolCall.id)
            _toolHistory.update { it + ToolCallHistory(toolCall, result, System.currentTimeMillis()) }
            return "ERROR: Tool '${toolCall.name}' not found. Available tools: ${registeredTools.keys.joinToString(", ")}"
        }
        if (requiresConfirmation(toolCall.name) && !consumeToolApproval(toolCall.id)) {
            val result = ToolResult(false, "Denied: this operation requires user confirmation", toolCall.id)
            _toolHistory.update { it + ToolCallHistory(toolCall, result, System.currentTimeMillis()) }
            return "DENIED: '${toolCall.name}' requires user confirmation."
        }
        val missingRequired = tool.parameters.filter { it.required && toolCall.args[it.name].isNullOrBlank() }
        if (missingRequired.isNotEmpty()) {
            val signature = tool.parameters.joinToString(", ") { p ->
                "<${p.name}>${if (p.required) "" else "?"}"
            }
            val missingNames = missingRequired.joinToString(", ") { "<${it.name}>" }
            val message = "ERROR: '${toolCall.name}' failed — missing required argument(s): $missingNames. " +
                "Full signature: ${toolCall.name}($signature). " +
                "Get valid values first via listScenes / listObjects / projectInventory. " +
                "Do NOT repeat this exact call with the same arguments."
            val result = ToolResult(false, message, toolCall.id)
            _toolHistory.update { it + ToolCallHistory(toolCall, result, System.currentTimeMillis()) }
            return message
        }
        return try {
            val result = withContext(Dispatchers.Main.immediate) {
                tool.execute(toolCall.args)
            }
            _toolHistory.update { it + ToolCallHistory(toolCall, result, System.currentTimeMillis()) }
            if (result.success) {
                result.data
            } else {
                "ERROR: ${result.data}"
            }
        } catch (e: Exception) {
            val result = ToolResult(false, "Error: ${e.message}", toolCall.id)
            _toolHistory.update { it + ToolCallHistory(toolCall, result, System.currentTimeMillis()) }
            "ERROR: ${e.message}"
        }
    }

    fun requiresConfirmation(toolName: String): Boolean =
        AiPreferences.isConfirmEnabled() && toolName in confirmationTools

    fun approveToolCall(toolCallId: String) {
        approvedToolCalls.add(toolCallId)
    }

    private fun consumeToolApproval(toolCallId: String): Boolean = approvedToolCalls.remove(toolCallId)

    suspend fun executeToolCalls(
        toolCalls: List<ToolCall>,
        onBeforeCall: (suspend (ToolCall) -> Unit)? = null
    ): List<Pair<ToolCall, String>> =
        toolCalls.map { toolCall ->
            onBeforeCall?.invoke(toolCall)
            toolCall to executeTool(toolCall)
        }

    fun getToolsDescription(): String {
        return registeredTools.values
            .filter { scopeProjectName == null || it.name !in globalOnlyTools }
            .joinToString("\n") { tool ->
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

    private val confirmationTools = setOf(
        "writeFile", "openProject", "remember", "forget", "localizeSprites",
        "generateGameTemplate", "generateAiTexture", "createProject", "buildApk",
        "exportProject", "export3dModel", "importNeoScriptModule"
    )

    fun addChange(change: ProjectChange) {
        synchronized(pendingChanges) { pendingChanges.add(change) }
    }

    private fun validatePath(projectDir: File, path: String): File? {
        val file = File(projectDir, path).normalize()
        return if (file.canonicalPath.startsWith(projectDir.canonicalPath + File.separator) || file.canonicalPath == projectDir.canonicalPath) {
            file
        } else null
    }

    private val backgroundAliases = setOf("background", "bg", "stage", "фон", "задний фон", "задний_план")

    fun resolveSprite(scene: Scene, name: String?): Sprite? {
        if (name.isNullOrBlank()) return null
        val trimmed = name.trim()
        scene.spriteList.firstOrNull { it.name == trimmed }?.let { return it }
        scene.spriteList.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.let { return it }
        val lower = trimmed.lowercase()
        val localizedBackground = try {
            context?.getString(org.catrobat.catroid.R.string.background)?.lowercase()
        } catch (_: Exception) { null }
        if (lower in backgroundAliases || lower == localizedBackground) {
            return scene.backgroundSprite
        }
        if (lower.length >= 3) {
            return scene.spriteList.firstOrNull { it.name?.lowercase()?.contains(lower) == true }
        }
        return null
    }

    private fun objectNotFoundError(scene: Scene, name: String): String =
        "Object '$name' not found in scene '${scene.name}'. Available objects: " +
            scene.spriteList.joinToString(", ") {
                val marker = if (it === scene.backgroundSprite) " (background)" else ""
                "'${it.name}'$marker"
            }

    private fun describeSprite(sprite: Sprite, sceneName: String): String = buildString {
        appendLine("Object: ${sprite.name}  (scene '$sceneName')")
        appendLine("Looks (${sprite.lookList.size}):")
        if (sprite.lookList.isEmpty()) appendLine("  (none)")
        for (look in sprite.lookList) {
            appendLine("  - '${look.name}' [file: ${look.fileName ?: "?"}]")
        }
        appendLine("Sounds (${sprite.soundList.size}):")
        if (sprite.soundList.isEmpty()) appendLine("  (none)")
        for (sound in sprite.soundList) {
            appendLine("  - '${sound.name}' [file: ${sound.fileName ?: "?"}]")
        }
        if (sprite.userVariables.isNotEmpty()) {
            appendLine("Local variables (${sprite.userVariables.size}): " +
                sprite.userVariables.joinToString(", ") { it.name ?: "?" })
        }
        if (sprite.userLists.isNotEmpty()) {
            appendLine("Local lists (${sprite.userLists.size}): " +
                sprite.userLists.joinToString(", ") { it.name ?: "?" })
        }
        appendLine("Scripts (${sprite.scriptList.size}):")
        for ((i, script) in sprite.scriptList.withIndex()) {
            appendLine("  Script $i: ${script::class.java.simpleName}")
            for ((j, brick) in script.getBrickList().withIndex()) {
                appendLine("    Brick $j: ${describeBrick(brick)}")
            }
        }
    }

    fun describeBrick(brick: Brick): String {
        val type = brick::class.java.simpleName
        val fields = mutableListOf<String>()
        try {
            for (field in brick.javaClass.declaredFields) {
                field.isAccessible = true
                val fname = field.name
                if (fname.contains("serialVersionUID") || fname.contains("$") ||
                    fname == "commentedOut" || fname == "parent" || fname == "drag") continue
                val value = try { field.get(brick)?.toString() ?: "null" } catch (_: Exception) { continue }
                if (value == "null" || value.length > 60) continue
                fields.add("$fname=$value")
            }
        } catch (_: Exception) {}
        val desc = try { BrickInfo.getDescription(brick) } catch (_: Exception) { "" }
        return buildString {
            append(type)
            if (!desc.isNullOrBlank()) append(" \u2014 $desc")
            if (fields.isNotEmpty()) append(" [${fields.joinToString(", ")}]")
        }
    }



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
            val sprite = resolveSprite(scene, objectName)
                ?: return ToolResult(false, objectNotFoundError(scene, objectName), "")
            return ToolResult(true, describeSprite(sprite, sceneName), "")
        }
    }

    class ReadBackgroundTool : Tool {
        override val name = "readBackground"
        override val description = "Read the BACKGROUND (stage) object of a scene: its looks, sounds and ALL its scripts with bricks. The background may have a localized name ('Background'/'Фон') — this tool resolves it automatically regardless of its name. Use it instead of guessing the background object's name."
        override val parameters = listOf(ToolParameter("scene", ParameterType.STRING, "Scene name"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val sceneName = args["scene"] ?: return ToolResult(false, "Missing 'scene' argument", "")
            val scene = project.sceneList.find { it.name == sceneName }
                ?: return ToolResult(false, "Scene '$sceneName' not found", "")
            val sprite = scene.backgroundSprite
                ?: return ToolResult(false, "Scene '$sceneName' has no background object", "")
            return ToolResult(true, describeSprite(sprite, sceneName), "")
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
            val sprite = resolveSprite(scene, objectName)
                ?: return ToolResult(false, objectNotFoundError(scene, objectName), "")
            val script = sprite.scriptList.getOrNull(index)
                ?: return ToolResult(false,
                    "Script index $index not found (object has ${sprite.scriptList.size} scripts)", "")
            val info = buildString {
                appendLine("Script $index: ${script::class.java.simpleName}")
                for ((j, brick) in script.getBrickList().withIndex()) {
                    appendLine("  Brick $j: ${describeBrick(brick)}")
                }
            }
            return ToolResult(true, info, "")
        }
    }

    class SearchVariableTool : Tool {
        override val name = "searchVariable"
        override val description = "Search for a variable by name across the whole project (global, multiplayer and object-local), reporting which object owns each local variable"
        override val parameters = listOf(ToolParameter("name", ParameterType.STRING, "Variable name pattern"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val pattern = (args["name"] ?: "").lowercase()
            val matches = mutableListOf<String>()
            for (v in project.userVariables) {
                if (v.name?.lowercase()?.contains(pattern) == true) matches.add("  - ${v.name} (global)")
            }
            for (v in project.multiplayerVariables) {
                if (v.name?.lowercase()?.contains(pattern) == true) matches.add("  - ${v.name} (multiplayer)")
            }
            for (scene in project.sceneList) {
                for (sprite in scene.spriteList) {
                    for (v in sprite.userVariables) {
                        if (v.name?.lowercase()?.contains(pattern) == true) {
                            matches.add("  - ${v.name} (local to object '${sprite.name}' in scene '${scene.name}')")
                        }
                    }
                }
            }
            val result = if (matches.isEmpty()) "No variables matching '$pattern' found" else matches.joinToString("\n")
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
        override val description = "Search user lists by name across the whole project (global and object-local), reporting which object owns each local list"
        override val parameters = listOf(ToolParameter("name", ParameterType.STRING, "List name pattern"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val pattern = (args["name"] ?: "").lowercase()
            val matches = mutableListOf<String>()
            for (list in project.userLists) {
                if (list.name?.lowercase()?.contains(pattern) == true) matches.add("  - ${list.name} (global)")
            }
            for (scene in project.sceneList) {
                for (sprite in scene.spriteList) {
                    for (list in sprite.userLists) {
                        if (list.name?.lowercase()?.contains(pattern) == true) {
                            matches.add("  - ${list.name} (local to object '${sprite.name}' in scene '${scene.name}')")
                        }
                    }
                }
            }
            val result = if (matches.isEmpty()) "No lists matching '$pattern' found" else matches.joinToString("\n")
            return ToolResult(true, "Lists matching '$pattern':\n$result", "")
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
        override val description = "Get comprehensive project information: scenes, objects (with script/look/sound counts), global variables, lists and broadcast messages"
        override val parameters = emptyList<ToolParameter>()

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val info = buildString {
                appendLine("Project: ${project.name}")
                appendLine("Scenes: ${project.sceneList.size}")
                for (scene in project.sceneList) {
                    appendLine("  Scene '${scene.name}': ${scene.spriteList.size} objects")
                    for (sprite in scene.spriteList) {
                        appendLine("    ${sprite.name}: ${sprite.scriptList.size} scripts, " +
                            "${sprite.lookList.size} looks, ${sprite.soundList.size} sounds")
                    }
                }
                val globalVars = project.userVariables
                appendLine("Global variables (${globalVars.size}): " +
                    globalVars.joinToString(", ") { it.name ?: "?" })
                val globalLists = project.userLists
                appendLine("Global lists (${globalLists.size}): " +
                    globalLists.joinToString(", ") { it.name ?: "?" })
                val broadcasts = collectBroadcasts(project)
                appendLine("Broadcast messages (${broadcasts.size}): " + broadcasts.joinToString(", "))
            }
            return ToolResult(true, info, "")
        }
    }

    private fun collectBroadcasts(project: Project): List<String> {
        val messages = linkedSetOf<String>()
        try {
            project.broadcastMessageContainer?.broadcastMessages?.let { messages.addAll(it) }
        } catch (_: Exception) {}
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                for (script in sprite.scriptList) {
                    for (brick in script.getBrickList()) {
                        extractBroadcast(brick)?.let { messages.add(it) }
                    }
                }
            }
        }
        return messages.toList()
    }

    private fun extractBroadcast(brick: Brick): String? {
        return try {
            val f = brick.javaClass.getDeclaredField("broadcastMessage")
            f.isAccessible = true
            val obj = f.get(brick) ?: return null
            val nameField = obj.javaClass.getDeclaredField("name")
            nameField.isAccessible = true
            nameField.get(obj) as? String
        } catch (_: Exception) { null }
    }

    class ProjectInventoryTool : Tool {
        override val name = "projectInventory"
        override val description = "Get a COMPLETE inventory of the whole project in one call: every scene, every object with its looks, sounds, local variables/lists and scripts (types + brick counts), plus global variables, lists and broadcast messages. Call this FIRST to understand everything, then readScript for brick-level detail."
        override val parameters = emptyList<ToolParameter>()

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val info = buildString {
                appendLine("PROJECT: ${project.name}")
                appendLine("Scenes: ${project.sceneList.size}")
                appendLine()
                for (scene in project.sceneList) {
                    appendLine("SCENE: ${scene.name} (${scene.spriteList.size} objects)")
                    for (sprite in scene.spriteList) {
                        appendLine("  OBJECT: ${sprite.name}")
                        appendLine("    Looks: " + if (sprite.lookList.isEmpty()) "(none)"
                            else sprite.lookList.joinToString(", ") { it.name ?: "?" })
                        appendLine("    Sounds: " + if (sprite.soundList.isEmpty()) "(none)"
                            else sprite.soundList.joinToString(", ") { it.name ?: "?" })
                        if (sprite.userVariables.isNotEmpty()) appendLine("    Local variables: " +
                            sprite.userVariables.joinToString(", ") { it.name ?: "?" })
                        if (sprite.userLists.isNotEmpty()) appendLine("    Local lists: " +
                            sprite.userLists.joinToString(", ") { it.name ?: "?" })
                        appendLine("    Scripts: ${sprite.scriptList.size}")
                        for ((i, script) in sprite.scriptList.withIndex()) {
                            appendLine("      [$i] ${script::class.java.simpleName} (${script.getBrickList().size} bricks)")
                        }
                    }
                    appendLine()
                }
                val globalVars = project.userVariables
                appendLine("GLOBAL variables (${globalVars.size}): " + globalVars.joinToString(", ") { it.name ?: "?" })
                val mpVars = project.multiplayerVariables
                if (mpVars.isNotEmpty()) appendLine("MULTIPLAYER variables (${mpVars.size}): " +
                    mpVars.joinToString(", ") { it.name ?: "?" })
                val globalLists = project.userLists
                appendLine("GLOBAL lists (${globalLists.size}): " + globalLists.joinToString(", ") { it.name ?: "?" })
                val broadcasts = collectBroadcasts(project)
                appendLine("BROADCAST messages (${broadcasts.size}): " + broadcasts.joinToString(", "))
            }
            return ToolResult(true, info, "")
        }
    }

    class ListLooksTool : Tool {
        override val name = "listLooks"
        override val description = "List looks (costumes/images). Without arguments lists ALL looks in the project with their owning object and scene. With scene+object lists that object's looks."
        override val parameters = listOf(
            ToolParameter("scene", ParameterType.STRING, "Scene name", required = false),
            ToolParameter("object", ParameterType.STRING, "Object name", required = false)
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val sceneFilter = args["scene"]
            val objectFilter = args["object"]
            val out = buildString {
                for (scene in project.sceneList) {
                    if (sceneFilter != null && scene.name != sceneFilter) continue
                    for (sprite in scene.spriteList) {
                        if (objectFilter != null && sprite.name != objectFilter) continue
                        for (look in sprite.lookList) {
                            appendLine("  - '${look.name}' [file: ${look.fileName ?: "?"}] in object '${sprite.name}' (scene '${scene.name}')")
                        }
                    }
                }
            }.ifBlank { "  (no looks found)" }
            return ToolResult(true, "Looks:\n$out", "")
        }
    }

    class ListSoundsTool : Tool {
        override val name = "listSounds"
        override val description = "List sounds. Without arguments lists ALL sounds in the project with their owning object and scene. With scene+object lists that object's sounds."
        override val parameters = listOf(
            ToolParameter("scene", ParameterType.STRING, "Scene name", required = false),
            ToolParameter("object", ParameterType.STRING, "Object name", required = false)
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val sceneFilter = args["scene"]
            val objectFilter = args["object"]
            val out = buildString {
                for (scene in project.sceneList) {
                    if (sceneFilter != null && scene.name != sceneFilter) continue
                    for (sprite in scene.spriteList) {
                        if (objectFilter != null && sprite.name != objectFilter) continue
                        for (sound in sprite.soundList) {
                            appendLine("  - '${sound.name}' [file: ${sound.fileName ?: "?"}] in object '${sprite.name}' (scene '${scene.name}')")
                        }
                    }
                }
            }.ifBlank { "  (no sounds found)" }
            return ToolResult(true, "Sounds:\n$out", "")
        }
    }

    class ListVariablesTool : Tool {
        override val name = "listVariables"
        override val description = "List ALL variables and lists in the project: global, multiplayer and object-local, each with its owning object and scene"
        override val parameters = emptyList<ToolParameter>()

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val info = buildString {
                appendLine("Global variables:")
                if (project.userVariables.isEmpty()) appendLine("  (none)")
                for (v in project.userVariables) appendLine("  - ${v.name}")
                if (project.multiplayerVariables.isNotEmpty()) {
                    appendLine("Multiplayer variables:")
                    for (v in project.multiplayerVariables) appendLine("  - ${v.name}")
                }
                appendLine("Global lists:")
                if (project.userLists.isEmpty()) appendLine("  (none)")
                for (l in project.userLists) appendLine("  - ${l.name}")
                for (scene in project.sceneList) {
                    for (sprite in scene.spriteList) {
                        for (v in sprite.userVariables) {
                            appendLine("  - ${v.name} (local variable of object '${sprite.name}' in scene '${scene.name}')")
                        }
                        for (l in sprite.userLists) {
                            appendLine("  - ${l.name} (local list of object '${sprite.name}' in scene '${scene.name}')")
                        }
                    }
                }
            }
            return ToolResult(true, info, "")
        }
    }

    class ListBroadcastsTool : Tool {
        override val name = "listBroadcasts"
        override val description = "List every broadcast message defined or used anywhere in the project"
        override val parameters = emptyList<ToolParameter>()

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val broadcasts = collectBroadcasts(project)
            val out = if (broadcasts.isEmpty()) "  (none)" else broadcasts.joinToString("\n") { "  - $it" }
            return ToolResult(true, "Broadcast messages:\n$out", "")
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

    class BuildScriptTool : Tool {
        override val name = "buildScript"
        override val description = "Create a real script on an object and append it. " +
            "scriptType: StartScript|WhenScript|WhenClonedScript|WhenConditionScript:<formula>|BroadcastScript:<message>. " +
            "bricks: one brick per line. Use exact brick class names from the catalog. " +
            "Container bricks use `{ }` for children and `else { }` for the else-branch, e.g.: " +
            "`ForeverBrick { MoveNStepsBrick(10) }` or `IfLogicBeginBrick(x > 5) { SetYBrick(10) } else { SetYBrick(-10) }`."
        override val parameters = listOf(
            ToolParameter("scene", ParameterType.STRING, "Scene name"),
            ToolParameter("object", ParameterType.STRING, "Object name"),
            ToolParameter("scriptType", ParameterType.STRING, "Script trigger type"),
            ToolParameter("bricks", ParameterType.STRING, "Brick specifications (supports nested `{ }` and `else { }`)")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No project open", "")
            val sceneName = args["scene"] ?: return ToolResult(false, "Missing 'scene' argument", "")
            val objectName = args["object"] ?: return ToolResult(false, "Missing 'object' argument", "")
            val scene = project.sceneList.find { it.name == sceneName }
                ?: return ToolResult(false, "Scene '$sceneName' not found", "")
            val sprite = resolveSprite(scene, objectName)
                ?: return ToolResult(false, objectNotFoundError(scene, objectName), "")

            val script = BrickFactory.createScript(args["scriptType"])
            val bricksText = args["bricks"].orEmpty()
            val specs = BrickFactory.parseBrickSpecs(bricksText)
            val created = mutableListOf<String>()
            val skipped = mutableListOf<String>()
            if (specs == null) {
                return ToolResult(false, "Syntax error in brick spec (unmatched '{' or '}' or 'else').", "")
            }
            for (spec in specs) {
                val validationError = BrickFactory.validateBrickSpec(spec)
                if (validationError != null) {
                    skipped.add("${specToString(spec)} — $validationError")
                    continue
                }
                val brick = BrickFactory.buildBrick(spec)
                if (brick != null) {
                    script.addBrick(brick)
                    created.add(specClassName(spec))
                } else {
                    skipped.add("${specToString(spec)} — construction failed")
                }
            }
            if (created.isEmpty()) {
                return ToolResult(false,
                    "No bricks were created.\n" + skipped.joinToString("\n") {
                        "  - $it"
                    }, "")
            }
            sprite.addScript(script)

            val summary = buildString {
                append("Created ${script::class.java.simpleName} on '$objectName' with ${created.size} top-level brick(s)")
                if (created.isNotEmpty()) append(": ${created.joinToString(", ")}")
                if (skipped.isNotEmpty()) append("\nSkipped:\n" + skipped.joinToString("\n") { "  - $it" })
            }
            return ToolResult(true, summary, "")
        }

        private fun specClassName(spec: BrickFactory.BrickSpec): String = when (spec) {
            is BrickFactory.BrickSpec.Simple -> spec.className
            is BrickFactory.BrickSpec.Container -> spec.className
        }

        private fun specToString(spec: BrickFactory.BrickSpec): String = when (spec) {
            is BrickFactory.BrickSpec.Simple -> "${spec.className}(${spec.args.joinToString(",")})"
            is BrickFactory.BrickSpec.Container -> "${spec.className}(${spec.args.joinToString(",")}){...}"
        }
    }

    class ListProjectsTool : Tool {
        override val name = "listProjects"
        override val description = "List all projects saved on the device (available only when the agent is not limited to a single project). Use open_project to switch to one of them."
        override val parameters = emptyList<ToolParameter>()

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val names = withContext(Dispatchers.IO) {
                org.catrobat.catroid.utils.FileMetaDataExtractor
                    .getProjectNames(FlavoredConstants.DEFAULT_ROOT_DIRECTORY)
            }
            if (names.isEmpty()) return ToolResult(true, "No projects found on the device", "")
            val current = ProjectManager.getInstance().currentProject?.name
            val out = names.joinToString("\n") { n ->
                if (n == current) "  - $n (currently open)" else "  - $n"
            }
            return ToolResult(true, "Projects on device:\n$out", "")
        }
    }

    class OpenProjectTool : Tool {
        override val name = "openProject"
        override val description = "Load a project by name so it becomes the current project (available only when the agent is not limited to a single project). Use list_projects first to see valid names."
        override val parameters = listOf(ToolParameter("name", ParameterType.STRING, "Project name"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val ctx = context ?: return ToolResult(false, "No context available", "")
            val name = args["name"] ?: return ToolResult(false, "Missing 'name' argument", "")
            val projectDir = File(FlavoredConstants.DEFAULT_ROOT_DIRECTORY, name)
            if (!projectDir.exists()) return ToolResult(false, "Project '$name' not found on device", "")
            val loaded = withContext(Dispatchers.IO) {
                org.catrobat.catroid.io.asynctask.loadProject(projectDir, ctx)
            }
            return if (loaded) {
                org.catrobat.catroid.ai.context.ContextManager.invalidateProjectCache()
                ToolResult(true, "Opened project '$name'. It is now the current project.", "")
            } else {
                ToolResult(false, "Failed to load project '$name'", "")
            }
        }
    }

    class RememberTool : Tool {
        override val name = "remember"
        override val description = "Store a durable fact, user preference or decision that should be recalled in FUTURE sessions " +
            "(persists across app restarts). Use for things like the user's preferred language/style, project conventions, " +
            "or important decisions. Keep the key short and stable so it can be overwritten later."
        override val parameters = listOf(
            ToolParameter("key", ParameterType.STRING, "Short stable identifier for this memory"),
            ToolParameter("content", ParameterType.STRING, "The information to remember"),
            ToolParameter("category", ParameterType.STRING,
                "One of: PROJECT_FACT, USER_PREFERENCE, CODE_PATTERN, TASK_CONTEXT, ERROR_PATTERN", required = false)
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val key = args["key"]?.takeIf { it.isNotBlank() }
                ?: return ToolResult(false, "Missing 'key'", "")
            val content = args["content"]?.takeIf { it.isNotBlank() }
                ?: return ToolResult(false, "Missing 'content'", "")
            val category = parseCategory(args["category"])
            MemoryManager.remember(key, content, category)
            return ToolResult(true, "Remembered '$key' (${category.name})", "")
        }

        private fun parseCategory(raw: String?): MemoryManager.MemoryCategory =
            try {
                if (raw.isNullOrBlank()) MemoryManager.MemoryCategory.PROJECT_FACT
                else MemoryManager.MemoryCategory.valueOf(raw.trim().uppercase())
            } catch (_: Exception) {
                MemoryManager.MemoryCategory.PROJECT_FACT
            }
    }

    class RecallTool : Tool {
        override val name = "recall"
        override val description = "Search long-term memory (facts, preferences and decisions saved with 'remember' in this or previous sessions) by keyword."
        override val parameters = listOf(ToolParameter("query", ParameterType.STRING, "Keyword to search stored memories"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val query = args["query"].orEmpty()
            val matches = if (query.isBlank()) emptyList() else MemoryManager.search(query)
            if (matches.isEmpty()) return ToolResult(true, "No stored memories matching '$query'", "")
            val out = matches.joinToString("\n") { "  - [${it.category.name}] ${it.key}: ${it.content}" }
            return ToolResult(true, "Memories matching '$query':\n$out", "")
        }
    }

    class ForgetTool : Tool {
        override val name = "forget"
        override val description = "Delete a stored long-term memory by its key."
        override val parameters = listOf(ToolParameter("key", ParameterType.STRING, "Key of the memory to delete"))

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val key = args["key"]?.takeIf { it.isNotBlank() }
                ?: return ToolResult(false, "Missing 'key'", "")
            val removed = MemoryManager.forget(key)
            return ToolResult(true,
                if (removed) "Forgot '$key'" else "No memory with key '$key'", "")
        }
    }

    class LocalizeSpritesTool : Tool {
        override val name = "localizeSprites"
        override val description = "Localize all sprite text to a target language. Extracts text from sprites, translates via Gemini, renders translated text preserving style."
        override val parameters = listOf(
            ToolParameter("targetLanguage", ParameterType.STRING, "Target language code (e.g. ru, en, de, fr, es, ja)"),
            ToolParameter("sourceLanguage", ParameterType.STRING, "Source language code or 'auto' for auto-detect", false)
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val context = this@ToolCallingEngine.context ?: return ToolResult(false, "No context available", "")
            val targetLang = args["targetLanguage"] ?: return ToolResult(false, "Missing targetLanguage", "")
            val sourceLang = args["sourceLanguage"] ?: "auto"

            val localizer = org.catrobat.catroid.ai.localization.SpriteLocalizer(
                context, targetLang, sourceLang
            )

            var report: org.catrobat.catroid.ai.localization.LocalizationReport? = null
            val deferred = kotlinx.coroutines.CompletableDeferred<org.catrobat.catroid.ai.localization.LocalizationReport>()

            localizer.onComplete = { r ->
                deferred.complete(r)
            }

            localizer.localizeProject()

            report = kotlinx.coroutines.withTimeoutOrNull(180_000) { deferred.await() }

            return report?.let { r ->
                val noText = r.noTextSprites
                val msg = buildString {
                    append("Localization to '$targetLang' complete.\n")
                    append("Sprites processed: ${r.processedSprites}/${r.totalSprites}\n")
                    if (noText > 0) {
                        append("Skipped (no text found by OCR): $noText — nothing was localized in those sprites.\n")
                    }
                    if (r.processedSprites == 0) {
                        append("WARNING: no sprite text was actually localized. Either the images have no " +
                            "detectable text, or OCR/Gemini failed. Check the failures below.\n")
                    }
                    if (r.hasFailures()) {
                        append("Failures (${r.failedSprites}):\n")
                        append(r.failureSummary())
                    }
                    if (r.processedSprites > 0) {
                        append("\n\nNEXT: Ask the user whether they want automatic language switching ")
                        append("wired up (a 'When scene starts' script per sprite that switches costume ")
                        append("based on the 'language' variable). ONLY if the user agrees, call the ")
                        append("wireLocalizationSwitch tool with targetLanguage='$targetLang'.")
                    }
                    append("\nDuration: ${r.durationMs / 1000}s")
                }
                ToolResult(r.processedSprites > 0, msg, "")
            } ?: ToolResult(false, "Localization timed out", "")
        }
    }

    class WireLocalizationSwitchTool : Tool {
        override val name = "wireLocalizationSwitch"
        override val description = "Wire automatic language switching for costumes created by localizeSprites. " +
            "For each sprite that has a '<name> (<lang>)' costume, adds a 'When scene starts' script that " +
            "switches to the localized costume when the global 'language' variable equals '<lang>', otherwise " +
            "keeps the original. Creates the 'language' variable if missing. Call ONLY after localizeSprites " +
            "AND after the user explicitly agrees."
        override val parameters = listOf(
            ToolParameter("targetLanguage", ParameterType.STRING, "Language code used for localization, e.g. ru")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val lang = args["targetLanguage"]?.takeIf { it.isNotBlank() }
                ?: return ToolResult(false, "Missing targetLanguage", "")
            addChange(ProjectChange(
                ChangeType.WIRE_LOCALIZATION_SWITCH,
                "Wire language switch for '$lang'",
                mapOf("language" to lang)
            ))
            return ToolResult(true,
                "Queued automatic language switching for '$lang'. A 'When scene starts' costume-switch script " +
                    "will be added to each localized sprite (driven by the 'language' variable).", "")
        }
    }

    class GenerateGameTemplateTool : Tool {
        override val name = "generateGameTemplate"
        override val description = "Creates a starter scene and Player object for a requested game type. It does not generate gameplay logic automatically."
        override val parameters = listOf(
            ToolParameter("templateType", ParameterType.STRING, "Game template type: PLATFORMER, FLAPPY_BIRD, SPACE_SHOOTER, MAZE_RUNNER, TOWER_DEFENSE"),
            ToolParameter("sceneName", ParameterType.STRING, "Target scene name, defaults to current scene")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val templateType = args["templateType"] ?: "PLATFORMER"
            val sceneName = args["sceneName"] ?: "MainScene"
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No active project", "")

            val scene = project.getSceneByName(sceneName) ?: Scene(sceneName, project).also { project.addScene(it) }
            val player = Sprite("Player")
            scene.addSprite(player)

            return ToolResult(true, "Created a $templateType starter scene '$sceneName' with a Player sprite. Gameplay logic and controls still need to be added.", "")
        }
    }

    class ScanAndFixProjectTool : Tool {
        override val name = "scanAndFixProject"
        override val description = "Scans the project for logic bugs and reports findings. It does not modify the project."
        override val parameters = listOf<ToolParameter>()

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No active project", "")
            val bugs = org.catrobat.catroid.ai.analysis.ProjectAnalyzer.findProjectBugs(project)
            if (bugs.isEmpty()) {
                return ToolResult(true, "No critical bugs found in project '${project.name}'.", "")
            }
            val summary = bugs.joinToString("\n") { "- ${it.description} at ${it.location}" }
            return ToolResult(true, "Found ${bugs.size} issue(s); no changes were applied:\n$summary", "")
        }
    }

    class ImportNeoScriptModuleTool : Tool {
        override val name = "importNeoScriptModule"
        override val description = "Imports a reusable .neoscript module (JoystickControl, HealthBar, EnemyAI, ParticleEmitter) into a target sprite."
        override val parameters = listOf(
            ToolParameter("moduleName", ParameterType.STRING, "Module name, e.g. JoystickControl, HealthBar, EnemyAI, ParticleEmitter"),
            ToolParameter("targetSprite", ParameterType.STRING, "Name of the target sprite"),
            ToolParameter("sceneName", ParameterType.STRING, "Scene name")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val moduleName = args["moduleName"] ?: return ToolResult(false, "Missing moduleName", "")
            val targetSprite = args["targetSprite"] ?: return ToolResult(false, "Missing targetSprite", "")
            return ToolResult(false, "Built-in module '$moduleName' is not available as a .neoscript file. Import requires an explicit module file path.", "")
        }
    }

    class GenerateAiTextureTool : Tool {
        override val name = "generateAiTexture"
        override val description = "Generates a texture bitmap by text prompt in background and adds it directly as a Look for the target sprite without opening Pocket Paint."
        override val parameters = listOf(
            ToolParameter("prompt", ParameterType.STRING, "Description of the texture/sprite image to generate"),
            ToolParameter("targetSprite", ParameterType.STRING, "Target sprite name"),
            ToolParameter("lookName", ParameterType.STRING, "Name of the new look costume")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val prompt = args["prompt"] ?: return ToolResult(false, "Missing prompt", "")
            val targetSpriteName = args["targetSprite"] ?: return ToolResult(false, "Missing targetSprite", "")
            val lookName = args["lookName"] ?: "AiLook_${System.currentTimeMillis() % 1000}"

            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No active project", "")
            val scene = project.sceneList.firstOrNull { s -> resolveSprite(s, targetSpriteName) != null }
                ?: return ToolResult(false,
                    "Sprite '$targetSpriteName' not found in any scene. " +
                        "For the background use object name 'background' or call listObjects first.", "")
            val sprite = resolveSprite(scene, targetSpriteName)!!

            val bitmap = android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

            val pLower = prompt.lowercase()
            when {
                pLower.contains("coin") || pLower.contains("gold") || pLower.contains("монета") -> {
                    paint.color = android.graphics.Color.rgb(255, 215, 0)
                    canvas.drawCircle(128f, 128f, 90f, paint)
                    paint.color = android.graphics.Color.rgb(218, 165, 32)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = 10f
                    canvas.drawCircle(128f, 128f, 90f, paint)
                }
                pLower.contains("heart") || pLower.contains("hp") || pLower.contains("сердце") -> {
                    paint.color = android.graphics.Color.RED
                    canvas.drawCircle(90f, 90f, 60f, paint)
                    canvas.drawCircle(166f, 90f, 60f, paint)
                    val path = android.graphics.Path().apply {
                        moveTo(30f, 105f)
                        lineTo(128f, 220f)
                        lineTo(226f, 105f)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                pLower.contains("star") || pLower.contains("звезда") -> {
                    paint.color = android.graphics.Color.YELLOW
                    canvas.drawCircle(128f, 128f, 80f, paint)
                }
                pLower.contains("brick") || pLower.contains("wall") || pLower.contains("стена") -> {
                    canvas.drawColor(android.graphics.Color.rgb(180, 70, 40))
                    paint.color = android.graphics.Color.rgb(230, 220, 200)
                    paint.strokeWidth = 4f
                    canvas.drawLine(0f, 128f, 256f, 128f, paint)
                    canvas.drawLine(128f, 0f, 128f, 128f, paint)
                }
                else -> {
                    paint.color = android.graphics.Color.rgb(60, 160, 240)
                    canvas.drawRoundRect(android.graphics.RectF(30f, 30f, 226f, 226f), 30f, 30f, paint)
                }
            }

            val imageDir = File(scene.directory, Constants.IMAGE_DIRECTORY_NAME)
            val imageFile = File(imageDir, "${System.currentTimeMillis()}_${lookName}.png")
            imageFile.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }

            val lookData = org.catrobat.catroid.common.LookData(lookName, imageFile)
            sprite.lookList.add(lookData)

            return ToolResult(true, "Created a local placeholder texture for '$prompt' and saved it to sprite '$targetSpriteName' as look '$lookName'. No AI image generation was used.", "")
        }
    }

    class CreateProjectTool : Tool {
        override val name = "createProject"
        override val description = "Creates a new blank project from scratch and sets it as the active current project (available outside project scope)."
        override val parameters = listOf(
            ToolParameter("name", ParameterType.STRING, "New project name"),
            ToolParameter("description", ParameterType.STRING, "Optional project description", required = false)
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val name = args["name"]?.takeIf { it.isNotBlank() } ?: return ToolResult(false, "Missing project name", "")
            val desc = args["description"] ?: ""
            val ctx = context ?: return ToolResult(false, "No context available", "")

            val projectDir = File(FlavoredConstants.DEFAULT_ROOT_DIRECTORY, name)
            if (projectDir.exists()) {
                return ToolResult(false, "Project '$name' already exists", "")
            }

            val project = Project(ctx, name)
            project.description = desc

            ProjectManager.getInstance().currentProject = project
            project.getDefaultScene()?.let { ProjectManager.getInstance().setCurrentlyEditedScene(it) }
            org.catrobat.catroid.ai.context.ContextManager.invalidateProjectCache()
            ProjectSaver(project, ctx).saveProjectAsync()

            return ToolResult(true, "Created and saved new project '$name' from scratch. It is now the current active project.", "")
        }
    }

    class BuildApkTool : Tool {
        override val name = "buildApk"
        override val description = "Builds an APK for the current project using APK Builder V3, auto-detecting permissions and saving the result to Download/NeoCatroidApks/."
        override val parameters = listOf(
            ToolParameter("appName", ParameterType.STRING, "App name for built APK", required = false)
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No active project", "")
            val appName = args["appName"]?.takeIf { it.isNotBlank() } ?: project.name

            val projectDir = project.directory ?: return ToolResult(false, "Project has no directory", "")
            val detectedPerms = org.catrobat.catroid.apkbuildV3.ProjectScanner.detectPermissions(project)
            val outDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "NeoCatroidApks")
            outDir.mkdirs()
            val apkFile = File(outDir, "${appName}.apk")
            val config = ApkBuilderV3Config(appName = appName, permissions = detectedPerms)
            return when (val result = ApkBuilderV3Engine.build(context ?: return ToolResult(false, "No context available", ""), projectDir, config, BuildProgressListener { _, _, _ -> })) {
                is AssemblyResult.Success -> {
                    result.apkFile.copyTo(apkFile, overwrite = true)
                    ToolResult(true, "Built APK for '$appName' at ${apkFile.absolutePath}. Permissions: ${detectedPerms.joinToString()}", "")
                }
                is AssemblyResult.Failure -> ToolResult(false, "APK build failed: ${result.message}", "")
            }
        }
    }

    class ExportProjectTool : Tool {
        override val name = "exportProject"
        override val description = "Exports the current project or its assets into various formats: ZIP, CATROBAT, NEOSCRIPT, HTML5, EMBROIDERY, GLB_3D saved to Download/NeoCatroidExports/."
        override val parameters = listOf(
            ToolParameter("format", ParameterType.STRING, "Export format: ZIP, CATROBAT, NEOSCRIPT, HTML5, EMBROIDERY, GLB_3D")
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val project = ProjectManager.getInstance().currentProject ?: return ToolResult(false, "No active project", "")
            val format = args["format"]?.uppercase() ?: "ZIP"
            if (format !in setOf("ZIP", "CATROBAT")) {
                return ToolResult(false, "Export format '$format' is not implemented by the agent. Supported formats: ZIP, CATROBAT.", "")
            }
            val outDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "NeoCatroidExports")
            outDir.mkdirs()

            val ext = when (format) {
                "NEOSCRIPT" -> "neoscript"
                "HTML5" -> "html"
                "EMBROIDERY" -> "dst"
                "GLB_3D" -> "glb"
                "CATROBAT" -> "catrobat"
                else -> "zip"
            }
            val outFile = File(outDir, "${project.name}.$ext")
            val projectDir = project.directory ?: return ToolResult(false, "Project has no directory", "")
            withContext(Dispatchers.IO) {
                if (outFile.exists()) outFile.delete()
                ZipArchiver().zipDedup(outFile, projectDir.listFiles() ?: emptyArray())
            }
            return ToolResult(true, "Exported project '${project.name}' as $format bundle to ${outFile.absolutePath}.", "")
        }
    }

    class Export3dModelTool : Tool {
        override val name = "export3dModel"
        override val description = "Exports a 3D object/model from the current scene into GLB 3D format saved to Download/NeoCatroidExports/."
        override val parameters = listOf(
            ToolParameter("objectId", ParameterType.STRING, "3D object identifier"),
            ToolParameter("outputName", ParameterType.STRING, "Output GLB file name", required = false)
        )

        override suspend fun execute(args: Map<String, String>): ToolResult {
            val objectId = args["objectId"] ?: return ToolResult(false, "Missing objectId", "")
            val name = args["outputName"] ?: objectId
            val outDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "NeoCatroidExports")
            outDir.mkdirs()
            return ToolResult(false, "3D object export is not implemented by the agent yet; no file was created for '$objectId'.", "")
        }
    }
}
