package org.catrobat.catroid.ai

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.ai.analysis.ProjectAnalyzer
import org.catrobat.catroid.ai.chat.ChatMessage
import org.catrobat.catroid.ai.context.ContextManager
import org.catrobat.catroid.ai.context.MemoryManager
import org.catrobat.catroid.ai.model.CloudModelRuntime
import org.catrobat.catroid.ai.model.ModelManager
import org.catrobat.catroid.ai.model.ModelRuntime
import org.catrobat.catroid.ai.modify.ProjectModifier
import org.catrobat.catroid.ai.modify.ValidationEngine
import org.catrobat.catroid.ai.prompt.PromptBuilder
import org.catrobat.catroid.ai.settings.AiPreferences
import org.catrobat.catroid.ai.tool.ToolCall
import org.catrobat.catroid.ai.tool.ToolCallingEngine
import java.lang.ref.WeakReference

enum class AiAgentState {
    IDLE,
    THINKING,
    USING_TOOLS,
    RESPONDING,
    ERROR
}

class AiAgentManager private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requestMutex = Mutex()
    @Volatile
    private var activeJob: Job? = null
    @Volatile
    private var initialized = false

    private val _state = MutableStateFlow(AiAgentState.IDLE)
    val state: StateFlow<AiAgentState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _activity = MutableStateFlow("")
    val activity: StateFlow<String> = _activity.asStateFlow()

    private val _reasoning = MutableStateFlow("")
    val reasoning: StateFlow<String> = _reasoning.asStateFlow()

    private val messageTimestampSeq = java.util.concurrent.atomic.AtomicLong(0)

    private fun nextTimestamp(): Long =
        System.currentTimeMillis() + messageTimestampSeq.incrementAndGet() % 1000

    @Volatile
    var scopeProjectName: String? = null
        private set

    fun setScope(projectName: String?) {
        scopeProjectName = projectName?.takeIf { it.isNotBlank() }
        toolEngine.scopeProjectName = scopeProjectName
    }

    val modelManager = ModelManager
    val modelRuntime = ModelRuntime
    val cloudRuntime = CloudModelRuntime
    val toolEngine = ToolCallingEngine
    val contextManager = ContextManager
    val memoryManager = MemoryManager
    val projectAnalyzer = ProjectAnalyzer
    val projectModifier = ProjectModifier
    val validationEngine = ValidationEngine
    val promptBuilder = PromptBuilder

    @Volatile
    private var confirmActivity: WeakReference<Activity>? = null

    fun attachActivity(activity: Activity?) {
        confirmActivity = if (activity != null) WeakReference(activity) else null
    }

    private var context: Context? = null

    fun init(appContext: Context) {
        if (initialized) return
        context = appContext
        AiPreferences.init(appContext)
        cloudRuntime.init(appContext)
        modelManager.init(appContext)
        toolEngine.init(appContext)
        memoryManager.init(appContext)
        initialized = true
    }

    fun isEnabled(): Boolean = AiPreferences.isEnabled()

    fun setEnabled(enabled: Boolean) {
        AiPreferences.setEnabled(enabled)
    }

    fun sendMessage(text: String) {
        val userMsg = ChatMessage(
            role = ChatMessage.Role.USER,
            content = text,
            timestamp = nextTimestamp()
        )
        _messages.update { it + userMsg }

        if (!isEnabled() || !isBackendReady()) {
            val reason = when {
                !isEnabled() ->
                    "AI Agent is disabled. Enable it in Settings → AI Agent."
                AiPreferences.isLocalBackend() ->
                    "No local model loaded. Open the model picker → More models, download a model and tap Use."
                else ->
                    "No Gemini API key set. Open Settings → AI Agent (or the chat menu) and enter your Google AI Studio key first."
            }
            val errMsg = ChatMessage(
                role = ChatMessage.Role.ASSISTANT,
                content = reason,
                timestamp = nextTimestamp()
            )
            _messages.update { it + errMsg }
            return
        }
        processMessage(text)
    }

    fun cancelCurrentRequest() {
        activeJob?.cancel()
        activeJob = null
        toolEngine.clearPendingChanges()
        _activity.value = ""
        _reasoning.value = ""
        _state.value = AiAgentState.IDLE
    }

    private fun isBackendReady(): Boolean =
        if (AiPreferences.isLocalBackend()) ModelRuntime.isModelLoaded() else cloudRuntime.isReady()

    private suspend fun generate(
        systemPrompt: String,
        userContent: String,
        temperature: Float,
        maxTokens: Int
    ): Pair<String, String?> {
        if (AiPreferences.isLocalBackend() && ModelRuntime.isModelLoaded()) {
            val modelId = AiPreferences.getSelectedModelId()
            val raw = modelRuntime.generate(
                applyLocalChatTemplate(systemPrompt, truncateForLocalBackend(userContent), modelId),
                temperature,
                maxTokens = LOCAL_MAX_GEN_TOKENS
            )
            return splitThinkBlock(raw)
        }
        val result = cloudRuntime.generateWithMeta(systemPrompt, userContent, temperature, maxTokens = maxTokens.coerceIn(256, 8192))
        return result.content to result.reasoning
    }

    private fun splitThinkBlock(text: String): Pair<String, String?> {
        val open = text.indexOf("<think>")
        if (open < 0) return text to null
        val close = text.indexOf("</think>", open)
        if (close < 0) {
            val thinking = text.substring(open + 7).trim()
            return text.substring(0, open).trim() to thinking.takeIf { it.isNotEmpty() }
        }
        val thinking = text.substring(open + 7, close).trim()
        val rest = (text.substring(0, open) + text.substring(close + 8)).trim()
        return rest to thinking.takeIf { it.isNotEmpty() }
    }

    private fun processMessage(userInput: String) {
        activeJob?.cancel()
        activeJob = scope.launch {
            requestMutex.withLock {
            try {
                _state.value = AiAgentState.THINKING
                _activity.value = "Reading your request…"
                _reasoning.value = ""

                val project = ProjectManager.getInstance().currentProject
                val analysis = if (project != null && AiPreferences.isAutoReadEnabled()) {
                    _activity.value = "Analyzing project '${project.name}'…"
                    projectAnalyzer.analyzeProject(project)
                } else null

                val isLocal = AiPreferences.isLocalBackend() && ModelRuntime.isModelLoaded()
                val systemPrompt = promptBuilder.buildSystemPrompt(analysis, includeCatalog = !isLocal)
                var userContent = promptBuilder.buildUserMessage(
                    userInput, analysis, contextManager.getRecentHistory()
                )

                _state.value = AiAgentState.USING_TOOLS

                val maxRounds = AiPreferences.getMaxToolCalls()
                var toolResult: String? = null
                var malformedStreak = 0
                var iteration = 0

                val temperature = AiPreferences.getTemperature()
                val maxTokens = AiPreferences.getMaxContext()

                while (iteration < maxRounds) {
                    _activity.value = "Thinking…"
                    val (response, reasoning) = generate(
                        systemPrompt,
                        userContent,
                        temperature = temperature,
                        maxTokens = maxTokens
                    )
                    if (!reasoning.isNullOrBlank()) {
                        _reasoning.value = (_reasoning.value + "\n" + reasoning).trim()
                    }
                    if (response.startsWith("Error")) {
                        toolResult = response
                        break
                    }
                    val toolCalls = toolEngine.parseToolCalls(response)
                    if (toolCalls.isEmpty()) {
                        if (response.contains("tool_call") && malformedStreak < 2) {
                            malformedStreak++
                            userContent += "\n## Correction\n" +
                                "Your previous response contained a malformed tool call. " +
                                "Output ONLY valid <tool_call> blocks exactly as specified, or a normal reply if you are done."
                            continue
                        }
                        toolResult = response
                        break
                    }
                    malformedStreak = 0
                    _state.value = AiAgentState.USING_TOOLS
                    val executionResults = executeToolCallsWithRetry(toolCalls) { toolCall ->
                        _activity.value = describeToolActivity(toolCall)
                        if (toolEngine.requiresConfirmation(toolCall.name) && AiPreferences.isConfirmEnabled()) {
                            if (awaitUserConfirmationText(describeToolActivity(toolCall))) {
                                toolEngine.approveToolCall(toolCall.id)
                            }
                        }
                    }
                    for ((toolCall, result) in executionResults) {
                        userContent += "\nTool result: $result\n"
                        contextManager.addToolCall(toolCall.name, toolCall.args, result)
                        emitToolFeed(toolCall, result)
                    }
                    val pending = toolEngine.getPendingChanges()
                    if (pending.isNotEmpty()) {
                        _activity.value = "Editing: " + describePendingChanges(pending)
                        val outcomes = applyPendingChangesSafely(project, pending)
                        _activity.value = "Done: " + describeAppliedChanges(pending, outcomes)
                        emitChangeCards(outcomes)
                        val summary = outcomes.joinToString("\n") { r ->
                            when (r) {
                                is org.catrobat.catroid.ai.modify.ProjectModifier.ModificationResult.Success -> "OK: ${r.message}"
                                is org.catrobat.catroid.ai.modify.ProjectModifier.ModificationResult.Failure -> "FAIL: ${r.error}"
                            }
                        }
                        userContent += "\n## Applied project changes:\n$summary\n"
                    }
                    iteration++
                }

                val (finalResponse, finalReasoning) = if (toolResult != null) {
                    toolResult to null
                } else {
                    _activity.value = "Thinking…"
                    generate(
                        systemPrompt,
                        userContent,
                        temperature = temperature,
                        maxTokens = maxTokens
                    )
                }
                if (!finalReasoning.isNullOrBlank()) {
                    _reasoning.value = (_reasoning.value + "\n" + finalReasoning).trim()
                }

                kotlinx.coroutines.delay(50)

                _state.value = AiAgentState.RESPONDING

                val assistantBody = buildString {
                    val thoughts = _reasoning.value.trim()
                    if (thoughts.isNotEmpty()) {
                        append("💭 **Reasoning**\n")
                        thoughts.split("\n").forEach { line -> append("> ").appendLine(line.take(400)) }
                        append("\n")
                    }
                    append(finalResponse)
                }
                val aiMsg = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = assistantBody,
                    timestamp = nextTimestamp()
                )
                _messages.update { it + aiMsg }
                contextManager.addMessage(userInput, finalResponse)

                val leftover = toolEngine.getPendingChanges()
                if (leftover.isNotEmpty()) {
                    val outcomes = applyPendingChangesSafely(project, leftover)
                    emitChangeCards(outcomes)
                }

                _activity.value = ""
                _state.value = AiAgentState.IDLE
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _activity.value = ""
                _state.value = AiAgentState.ERROR
                val errMsg = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = "Error: ${e.message}",
                    timestamp = nextTimestamp()
                )
                _messages.update { it + errMsg }
            }
            }
        }
    }

    private fun emitToolFeed(toolCall: org.catrobat.catroid.ai.tool.ToolCall, result: String) {
        val argsPreview = toolCall.args.entries.joinToString(", ") { "${it.key}=${it.value.take(60)}" }
        val ok = !(result.startsWith("ERROR") || result.startsWith("Error"))
        val status = if (ok) "✅" else "⚠️"
        val resultPreview = result.replace("\n", " ").take(200)
        val feed = ChatMessage(
            role = ChatMessage.Role.TOOL,
            content = "$status ${toolCall.name}(${argsPreview.take(160)}) → $resultPreview",
            timestamp = nextTimestamp()
        )
        _messages.update { it + feed }
    }

    private suspend fun applyPendingChangesSafely(
        project: org.catrobat.catroid.content.Project?,
        changes: List<org.catrobat.catroid.ai.tool.ProjectChange>
    ): List<ProjectModifier.ModificationResult> {
        if (project == null) {
            toolEngine.clearPendingChanges()
            return changes.map { ProjectModifier.ModificationResult.Failure("No project open") }
        }
        if (!AiPreferences.isAutoModifyEnabled()) {
            toolEngine.clearPendingChanges()
            return changes.map {
                ProjectModifier.ModificationResult.Failure(
                    "Automatic project changes are disabled. Enable 'Auto-modify project' to apply this change."
                )
            }
        }
        val validation = withContext(Dispatchers.Main.immediate) {
            validationEngine.validateChanges(project, changes)
        }
        if (!validation.isValid) {
            toolEngine.clearPendingChanges()
            return validation.errors.map { ProjectModifier.ModificationResult.Failure(it) }
        }
        if (AiPreferences.isConfirmEnabled() && !awaitUserConfirmation(changes)) {
            toolEngine.clearPendingChanges()
            return changes.map { ProjectModifier.ModificationResult.Failure("Declined by user") }
        }
        val outcomes = withContext(Dispatchers.Main.immediate) {
            projectModifier.applyChanges(changes)
        }
        toolEngine.clearPendingChanges()
        return outcomes
    }

    private suspend fun awaitUserConfirmation(changes: List<org.catrobat.catroid.ai.tool.ProjectChange>): Boolean {
        val description = changes.joinToString("\n") { "• ${describeChange(it)}" }
        return awaitUserConfirmationText(description)
    }

    private suspend fun awaitUserConfirmationText(description: String): Boolean {
        val activity = confirmActivity?.get()
            ?: return false
        if (activity.isFinishing || activity.isDestroyed) return false
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) {
                    cont.resumeWith(Result.success(false))
                    return@runOnUiThread
                }
                val dialog = android.app.AlertDialog.Builder(activity)
                    .setTitle(org.catrobat.catroid.R.string.ai_agent_confirm_changes_title)
                    .setMessage(
                        activity.getString(org.catrobat.catroid.R.string.ai_agent_confirm_changes_message) +
                            "\n\n$description"
                    )
                    .setPositiveButton(org.catrobat.catroid.R.string.ai_agent_apply) { _, _ -> cont.resumeWith(Result.success(true)) }
                    .setNegativeButton(org.catrobat.catroid.R.string.ai_agent_cancel) { _, _ -> cont.resumeWith(Result.success(false)) }
                    .setOnCancelListener { cont.resumeWith(Result.success(false)) }
                    .show()
                cont.invokeOnCancellation { dialog.dismiss() }
            }
        }
    }

    private fun describePendingChanges(changes: List<org.catrobat.catroid.ai.tool.ProjectChange>): String {
        val lines = changes.map { describeChange(it) }
        return if (lines.size == 1) lines.first() else lines.joinToString("\n") { "• $it" }
    }

    private fun describeChange(change: org.catrobat.catroid.ai.tool.ProjectChange): String {
        val d = change.data
        val scene = (d["scene"] as? String)?.takeIf { it.isNotBlank() }
        val obj = (d["object"] as? String)?.takeIf { it.isNotBlank() }
        val name = (d["name"] as? String)?.takeIf { it.isNotBlank() } ?: ""
        val index = d["index"]?.toString() ?: "?"
        val path = when {
            scene != null && obj != null -> "$scene/$obj"
            obj != null -> obj
            scene != null -> scene
            else -> ""
        }
        return when (change.type) {
            org.catrobat.catroid.ai.tool.ChangeType.CREATE_OBJECT ->
                "Creating object '$name'${if (scene != null) " in '$scene'" else ""}"
            org.catrobat.catroid.ai.tool.ChangeType.DELETE_OBJECT ->
                "Deleting object '$name'${if (scene != null) " from '$scene'" else ""}"
            org.catrobat.catroid.ai.tool.ChangeType.CREATE_SCENE -> "Creating scene '$name'"
            org.catrobat.catroid.ai.tool.ChangeType.DELETE_SCENE -> "Deleting scene '$name'"
            org.catrobat.catroid.ai.tool.ChangeType.REPLACE_SCRIPT -> "Replacing script #$index of $path"
            org.catrobat.catroid.ai.tool.ChangeType.APPEND_SCRIPT -> "Writing a script on $path"
            org.catrobat.catroid.ai.tool.ChangeType.DELETE_SCRIPT -> "Deleting script #$index of $path"
            org.catrobat.catroid.ai.tool.ChangeType.CREATE_VARIABLE -> "Creating variable '$name'"
            org.catrobat.catroid.ai.tool.ChangeType.DELETE_VARIABLE -> "Deleting variable '$name'"
            org.catrobat.catroid.ai.tool.ChangeType.CREATE_BROADCAST -> "Creating broadcast '$name'"
            org.catrobat.catroid.ai.tool.ChangeType.MODIFY_BRICK ->
                if (path.isNotEmpty()) "Editing a brick on $path" else "Editing a brick"
            org.catrobat.catroid.ai.tool.ChangeType.WIRE_LOCALIZATION_SWITCH ->
                "Wiring automatic language switching"
        }
    }

    private fun describeAppliedChanges(
        changes: List<org.catrobat.catroid.ai.tool.ProjectChange>,
        outcomes: List<ProjectModifier.ModificationResult>
    ): String {
        val lines = changes.mapIndexed { i, change ->
            val base = describeChange(change)
            when (val outcome = outcomes.getOrNull(i)) {
                is ProjectModifier.ModificationResult.Success -> {
                    val diff = brickDiff(outcome.card?.added ?: 0, outcome.card?.removed ?: 0)
                    if (diff.isNotEmpty()) "$base  $diff" else base
                }
                is ProjectModifier.ModificationResult.Failure -> "$base — failed"
                null -> base
            }
        }
        return if (lines.size == 1) lines.first() else lines.joinToString("\n") { "• $it" }
    }

    private fun brickDiff(added: Int, removed: Int): String = buildString {
        if (added > 0) append("+$added")
        if (removed > 0) {
            if (isNotEmpty()) append(" ")
            append("-$removed")
        }
    }

    private fun emitChangeCards(outcomes: List<ProjectModifier.ModificationResult>) {
        val cards = outcomes.mapNotNull { outcome ->
            (outcome as? ProjectModifier.ModificationResult.Success)?.card
        }
        if (cards.isEmpty()) return
        val newMessages = cards.map { card ->
            ChatMessage(
                role = ChatMessage.Role.CHANGE,
                content = card.label,
                timestamp = nextTimestamp(),
                changeCard = card
            )
        }
        _messages.update { it + newMessages }
    }

    fun clearHistory() {
        _messages.update { emptyList() }
        contextManager.clear()
    }

    fun clearMemory() {
        memoryManager.clear()
        contextManager.clear()
    }

    private suspend fun executeToolCallsWithRetry(
        toolCalls: List<ToolCall>,
        onBeforeCall: (suspend (ToolCall) -> Unit)? = null
    ): List<Pair<ToolCall, String>> {
        var results = toolEngine.executeToolCalls(toolCalls, onBeforeCall)
        var attempt = 0
        while (attempt < MAX_TOOL_EXEC_RETRIES) {
            val failedReads = results.filter { (toolCall, result) ->
                toolCall.name in READ_ONLY_TOOLS && isTransientFailure(result)
            }
            if (failedReads.isEmpty()) break
            attempt++
            _activity.value = "Retrying ${failedReads.size} read(s)… (attempt $attempt/$MAX_TOOL_EXEC_RETRIES)"
            val retried = toolEngine.executeToolCalls(failedReads.map { it.first }, onBeforeCall)
            results = results.map { (tc, r) ->
                retried.firstOrNull { it.first.id == tc.id } ?: (tc to r)
            }
        }
        return results
    }

    private fun describeToolActivity(toolCall: ToolCall): String {
        val a = toolCall.args
        val scene = a["scene"]?.takeIf { it.isNotBlank() }
        val obj = a["object"]?.takeIf { it.isNotBlank() }
        val path = when {
            scene != null && obj != null -> "$scene/$obj"
            obj != null -> obj
            scene != null -> scene
            else -> ""
        }
        val name = a["name"]?.takeIf { it.isNotBlank() } ?: ""
        return when (toolCall.name) {
            "readObject" -> "Reading object $path…"
            "readScene" -> "Reading scene ${scene ?: ""}…"
            "readScript" -> "Reading script #${a["index"] ?: "?"} of $path…"
            "listObjects" -> "Reading objects in ${scene ?: ""}…"
            "listScenes" -> "Reading scene list…"
            "projectInfo", "projectInventory" -> "Reading project structure…"
            "listLooks" -> if (path.isNotEmpty()) "Reading looks of $path…" else "Reading all looks…"
            "listSounds" -> if (path.isNotEmpty()) "Reading sounds of $path…" else "Reading all sounds…"
            "listVariables" -> "Reading variables…"
            "listBroadcasts" -> "Reading broadcast messages…"
            "codeAnalysis" -> "Analyzing code…"
            "searchVariable" -> "Searching variable '${a["name"] ?: ""}'…"
            "searchList" -> "Searching list '${a["name"] ?: ""}'…"
            "searchBroadcast" -> "Searching broadcast '${a["name"] ?: ""}'…"
            "searchFiles" -> "Searching files '${a["pattern"] ?: ""}'…"
            "readFile" -> "Reading file ${a["path"] ?: ""}…"
            "writeFile" -> "Writing file ${a["path"] ?: ""}…"
            "createObject" -> "Creating object '$name'${if (scene != null) " in $scene" else ""}…"
            "deleteObject" -> "Deleting object '$name'…"
            "createScene" -> "Creating scene '$name'…"
            "deleteScene" -> "Deleting scene '$name'…"
            "createVariable" -> "Creating variable '$name'…"
            "deleteVariable" -> "Deleting variable '$name'…"
            "buildScript" -> "Writing a script on $path…"
            "replaceScript" -> "Replacing script #${a["index"] ?: "?"} of $path…"
            "appendScript" -> "Adding a script to $path…"
            "deleteScript" -> "Deleting script #${a["index"] ?: "?"} of $path…"
            "listProjects" -> "Reading project list…"
            "openProject" -> "Opening project '${a["name"] ?: ""}'…"
            "remember" -> "Saving to memory '${a["key"] ?: ""}'…"
            "recall" -> "Recalling memory '${a["query"] ?: ""}'…"
            "forget" -> "Forgetting memory '${a["key"] ?: ""}'…"
            "localizeSprites" -> "Localizing sprites to '${a["targetLanguage"] ?: ""}'… (this may take a while)"
            "wireLocalizationSwitch" -> "Wiring language switch for '${a["targetLanguage"] ?: ""}'…"
            else -> "Running ${toolCall.name}…"
        }
    }

    private fun isTransientFailure(result: String): Boolean =
        (result.startsWith("ERROR") || result.startsWith("Error")) &&
            !result.contains("Unknown tool", ignoreCase = true)

    companion object {
        private const val LOCAL_MAX_GEN_TOKENS = 512

        private const val MAX_TOOL_EXEC_RETRIES = 2

        private val READ_ONLY_TOOLS = setOf(
            "listScenes", "listObjects", "readScene", "readObject", "readScript",
            "projectInfo", "projectInventory", "listLooks", "listSounds",
            "listVariables", "listBroadcasts", "codeAnalysis",
            "searchVariable", "searchList", "searchBroadcast", "searchFiles",
            "readFile", "listProjects", "recall"
        )

        private const val LOCAL_PROMPT_CHAR_LIMIT = 4000

        private fun truncateForLocalBackend(input: String): String {
            if (input.length <= LOCAL_PROMPT_CHAR_LIMIT) return input
            val userMarker = "## User Request"
            val userIdx = input.indexOf(userMarker)
            if (userIdx >= 0) {
                val tail = input.substring(userIdx)
                if (tail.length <= LOCAL_PROMPT_CHAR_LIMIT) return tail
                val last = tail.takeLast(LOCAL_PROMPT_CHAR_LIMIT)
                val nlIdx = last.indexOf('\n')
                return if (nlIdx > 0) last.substring(nlIdx + 1) else last
            }
            val last = input.takeLast(LOCAL_PROMPT_CHAR_LIMIT)
            val nlIdx = last.indexOf('\n')
            return if (nlIdx > 0) last.substring(nlIdx + 1) else last
        }

        private fun applyLocalChatTemplate(systemPrompt: String, userContent: String, modelId: String?): String {
            val id = modelId?.lowercase().orEmpty()
            return if (id.contains("llama")) {
                "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n" +
                    "$systemPrompt<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n" +
                    "$userContent<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
            } else {
                "<|im_start|>system\n$systemPrompt<|im_end|>\n" +
                    "<|im_start|>user\n$userContent<|im_end|>\n" +
                    "<|im_start|>assistant\n"
            }
        }

        @JvmStatic
        val instance: AiAgentManager by lazy { AiAgentManager() }
    }
}
