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

        if (!isEnabled() || !cloudRuntime.isReady()) {
            val reason = if (!isEnabled()) {
                "AI Agent is disabled. Enable it in Settings → AI Agent."
            } else {
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

    private fun processMessage(userInput: String) {
        scope.launch {
            try {
                _state.value = AiAgentState.THINKING
                _activity.value = "Reading your request…"
                kotlinx.coroutines.delay(300)

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
                    val response = cloudRuntime.generate(
                        modelInput.toString(),
                        temperature = temperature,
                        maxTokens = maxTokens.coerceIn(256, 8192)
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
                        val argsPreview = toolCall.args.entries.joinToString(", ") { "${it.key}=${it.value}" }
                        _activity.value = "Calling ${toolCall.name}($argsPreview)…"
                        val result = toolEngine.executeTool(toolCall)
                        _activity.value = "${toolCall.name} → ${result.take(160)}"
                        modelInput.append("\nTool result: $result\n")
                        contextManager.addToolCall(toolCall.name, toolCall.args, result)
                    }
                    val pending = toolEngine.getPendingChanges()
                    if (pending.isNotEmpty()) {
                        _activity.value = "Applying ${pending.size} change(s) to the project…"
                        val outcomes = projectModifier.applyChanges(pending)
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

                val finalResponse = toolResult ?: cloudRuntime.generate(
                    modelInput.toString(),
                    temperature = temperature,
                    maxTokens = maxTokens.coerceIn(256, 8192)
                )

                kotlinx.coroutines.delay(200)

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
        @JvmStatic
        val instance: AiAgentManager by lazy { AiAgentManager() }
    }
}
