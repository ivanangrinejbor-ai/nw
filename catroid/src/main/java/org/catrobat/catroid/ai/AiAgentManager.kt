package org.catrobat.catroid.ai

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

enum class AiAgentState {
    IDLE,
    THINKING,
    USING_TOOLS,
    RESPONDING,
    ERROR
}

class AiAgentManager private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    private var initialized = false

    private val _state = MutableStateFlow(AiAgentState.IDLE)
    val state: StateFlow<AiAgentState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _activity = MutableStateFlow("")
    val activity: StateFlow<String> = _activity.asStateFlow()

    /** null = global scope (all projects); otherwise the agent is confined to this project. */
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
        // Always show the user's message first, regardless of readiness.
        val userMsg = ChatMessage(
            role = ChatMessage.Role.USER,
            content = text,
            timestamp = System.currentTimeMillis()
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
                timestamp = System.currentTimeMillis()
            )
            _messages.update { it + errMsg }
            return
        }
        processMessage(text)
    }

    /** Whether the currently selected backend can serve a request. */
    private fun isBackendReady(): Boolean =
        if (AiPreferences.isLocalBackend()) ModelRuntime.isModelLoaded() else cloudRuntime.isReady()

    /** Routes generation to the local llama.cpp runtime or the cloud Gemini runtime. */
    private suspend fun generate(input: String, temperature: Float, maxTokens: Int): String =
        if (AiPreferences.isLocalBackend() && ModelRuntime.isModelLoaded()) {
            // On-device: cap generated tokens so we never exceed the (small) local context
            // and keep latency/memory bounded. maxTokens here is the full context budget.
            modelRuntime.generate(input, temperature, maxTokens = LOCAL_MAX_GEN_TOKENS)
        } else {
            cloudRuntime.generate(input, temperature, maxTokens = maxTokens.coerceIn(256, 8192))
        }

    private fun processMessage(userInput: String) {
        scope.launch {
            try {
                _state.value = AiAgentState.THINKING
                _activity.value = "Reading your request…"

                val project = ProjectManager.getInstance().currentProject
                val analysis = if (project != null && AiPreferences.isAutoReadEnabled()) {
                    _activity.value = "Analyzing project '${project.name}'…"
                    projectAnalyzer.analyzeProject(project)
                } else null

                val systemPrompt = promptBuilder.buildSystemPrompt(analysis)
                val conversationHistory = contextManager.getRecentHistory()
                val userPrompt = promptBuilder.buildUserPrompt(userInput, analysis)

                val modelInput = StringBuilder(
                    promptBuilder.assembleFullPrompt(
                        systemPrompt = systemPrompt,
                        conversationHistory = conversationHistory,
                        userPrompt = userPrompt
                    )
                )

                _state.value = AiAgentState.USING_TOOLS

                val maxRounds = AiPreferences.getMaxToolCalls()
                var toolResult: String? = null
                var iteration = 0

                val temperature = AiPreferences.getTemperature()
                val maxTokens = AiPreferences.getMaxContext()

                while (iteration < maxRounds) {
                    _activity.value = "Thinking… (round ${iteration + 1}/$maxRounds)"
                    val response = generate(
                        truncateForLocalBackend(modelInput.toString()),
                        temperature = temperature,
                        maxTokens = maxTokens
                    )
                    if (response.startsWith("Error")) {
                        toolResult = response
                        break
                    }
                    val toolCalls = toolEngine.parseToolCalls(response)
                    if (toolCalls.isEmpty()) {
                        toolResult = response
                        break
                    }
                    for (toolCall in toolCalls) {
                        _activity.value = describeToolActivity(toolCall)
                        val result = toolEngine.executeTool(toolCall)
                        _activity.value = describeToolActivityDone(toolCall)
                        modelInput.append("\nTool result: $result\n")
                        contextManager.addToolCall(toolCall.name, toolCall.args, result)
                    }
                    val pending = toolEngine.getPendingChanges()
                    if (pending.isNotEmpty()) {
                        _activity.value = describePendingChanges(pending)
                        val outcomes = projectModifier.applyChanges(pending)
                        _activity.value = describeAppliedChanges(pending, outcomes)
                        toolEngine.clearPendingChanges()
                        emitChangeCards(outcomes)
                        val summary = outcomes.joinToString("\n") { r ->
                            when (r) {
                                is org.catrobat.catroid.ai.modify.ProjectModifier.ModificationResult.Success -> "OK: ${r.message}"
                                is org.catrobat.catroid.ai.modify.ProjectModifier.ModificationResult.Failure -> "FAIL: ${r.error}"
                            }
                        }
                        modelInput.append("\n## Applied project changes:\n$summary\n")
                    }
                    iteration++
                }

                val finalResponse = toolResult ?: generate(
                    truncateForLocalBackend(modelInput.toString()),
                    temperature = temperature,
                    maxTokens = maxTokens
                )

                kotlinx.coroutines.delay(50)

                _state.value = AiAgentState.RESPONDING

                val aiMsg = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = finalResponse,
                    timestamp = System.currentTimeMillis()
                )
                _messages.update { it + aiMsg }
                contextManager.addMessage(userInput, finalResponse)

                val leftover = toolEngine.getPendingChanges()
                if (leftover.isNotEmpty()) {
                    val outcomes = projectModifier.applyChanges(leftover)
                    toolEngine.clearPendingChanges()
                    emitChangeCards(outcomes)
                }

                _activity.value = ""
                _state.value = AiAgentState.IDLE
            } catch (e: Exception) {
                _activity.value = ""
                _state.value = AiAgentState.ERROR
                val errMsg = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = "Error: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
                _messages.update { it + errMsg }
            }
        }
    }

    /**
     * Human-friendly description of what the agent is currently doing with a tool,
     * e.g. "Reading object Scene1/Object1" instead of the raw tool-call syntax.
     * Shown in the expandable "Thinking" row.
     */
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
        return when (toolCall.name) {
            "readObject" -> "Reading object $path"
            "readScene" -> "Reading scene ${scene ?: ""}"
            "readScript" -> "Reading script #${a["index"] ?: "?"} of $path"
            "listObjects" -> "Reading objects in ${scene ?: ""}"
            "listScenes" -> "Reading scene list"
            "projectInfo", "projectInventory" -> "Reading project structure"
            "listLooks" -> if (path.isNotEmpty()) "Reading looks of $path" else "Reading all looks"
            "listSounds" -> if (path.isNotEmpty()) "Reading sounds of $path" else "Reading all sounds"
            "listVariables" -> "Reading variables"
            "listBroadcasts" -> "Reading broadcast messages"
            "codeAnalysis" -> "Analyzing code"
            "searchVariable" -> "Searching variable '${a["name"] ?: ""}'"
            "searchList" -> "Searching list '${a["name"] ?: ""}'"
            "searchBroadcast" -> "Searching broadcast '${a["name"] ?: ""}'"
            "searchFiles" -> "Searching files '${a["pattern"] ?: ""}'"
            "readFile" -> "Reading file ${a["path"] ?: ""}"
            "writeFile" -> "Writing file ${a["path"] ?: ""}"
            "createObject" -> "Creating object '${a["name"] ?: ""}' in ${scene ?: ""}"
            "deleteObject" -> "Deleting object '${a["name"] ?: ""}'"
            "createScene" -> "Creating scene '${a["name"] ?: ""}'"
            "deleteScene" -> "Deleting scene '${a["name"] ?: ""}'"
            "createVariable" -> "Creating variable '${a["name"] ?: ""}'"
            "deleteVariable" -> "Deleting variable '${a["name"] ?: ""}'"
            "buildScript" -> "Writing a script on $path"
            "replaceScript" -> "Replacing script #${a["index"] ?: "?"} of $path"
            "appendScript" -> "Adding a script to $path"
            "deleteScript" -> "Deleting script #${a["index"] ?: "?"} of $path"
            "listProjects" -> "Reading project list"
            "openProject" -> "Opening project '${a["name"] ?: ""}'"
            "remember" -> "Saving to memory '${a["key"] ?: ""}'"
            "recall" -> "Recalling memory '${a["query"] ?: ""}'"
            "forget" -> "Forgetting memory '${a["key"] ?: ""}'"
            else -> "Running ${toolCall.name}"
        }
    }

    /** Past-tense variant shown briefly after a tool finishes. */
    private fun describeToolActivityDone(toolCall: ToolCall): String =
        describeToolActivity(toolCall).let { desc ->
            when {
                desc.startsWith("Reading") -> desc.replaceFirst("Reading", "Read")
                desc.startsWith("Writing") -> desc.replaceFirst("Writing", "Wrote")
                desc.startsWith("Searching") -> desc.replaceFirst("Searching", "Searched")
                desc.startsWith("Analyzing") -> desc.replaceFirst("Analyzing", "Analyzed")
                desc.startsWith("Creating") -> desc.replaceFirst("Creating", "Created")
                desc.startsWith("Deleting") -> desc.replaceFirst("Deleting", "Deleted")
                desc.startsWith("Replacing") -> desc.replaceFirst("Replacing", "Replaced")
                desc.startsWith("Adding") -> desc.replaceFirst("Adding", "Added")
                desc.startsWith("Opening") -> desc.replaceFirst("Opening", "Opened")
                desc.startsWith("Saving") -> desc.replaceFirst("Saving", "Saved")
                desc.startsWith("Recalling") -> desc.replaceFirst("Recalling", "Recalled")
                desc.startsWith("Forgetting") -> desc.replaceFirst("Forgetting", "Forgot")
                else -> "$desc — done"
            }
        }

    /**
     * Human-friendly description of the concrete project changes about to be applied,
     * e.g. "Creating object 'Bird' in 'Scene1'" or "Writing a script on Scene1/Bird".
     * Shown (and tap-expandable) in the "Thinking" row so writes/creates are visible,
     * not just a "N change(s)" count.
     */
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
        }
    }

    /**
     * Same present-tense description as [describePendingChanges] but with the concrete
     * brick diff appended once the change has been applied, e.g.
     * "Replacing script #1 of Scene1/Bird  +1 -1" (1 brick added, 1 removed in place).
     * [outcomes] is 1:1 with [changes] (see ProjectModifier.applyChanges).
     */
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

    /** Compact "+added -removed" brick counter, empty when nothing changed. */
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
                timestamp = System.currentTimeMillis(),
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

    companion object {
        /** Max tokens generated per round on the local (on-device) backend. */
        private const val LOCAL_MAX_GEN_TOKENS = 512

        /**
         * Hard character limit for prompts sent to the local (llama.cpp) backend.
         * Qwen 0.5B Q5 with n_ctx=2048 is roughly 2048 * ~3.5 chars/token ≈ 7168 chars.
         * We leave generous room for generated tokens.
         */
        private const val LOCAL_PROMPT_CHAR_LIMIT = 4000

        /**
         * If the local backend is active and [input] exceeds [LOCAL_PROMPT_CHAR_LIMIT],
         * keep only the TAIL of the string so the most recent context is preserved and
         * the JNI tokenizer doesn't overflow n_ctx (which causes a native crash).
         */
        private fun truncateForLocalBackend(input: String): String {
            if (!AiPreferences.isLocalBackend()) return input
            if (input.length <= LOCAL_PROMPT_CHAR_LIMIT) return input
            val truncated = input.takeLast(LOCAL_PROMPT_CHAR_LIMIT)
            // Try to start at a newline boundary so we don't cut mid-sentence.
            val nlIdx = truncated.indexOf('\n')
            return if (nlIdx > 0) truncated.substring(nlIdx + 1) else truncated
        }

        @JvmStatic
        val instance: AiAgentManager by lazy { AiAgentManager() }
    }
}
