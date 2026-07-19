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

    val modelManager = ModelManager
    val modelRuntime = ModelRuntime
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
        if (!isEnabled() || !modelRuntime.isModelLoaded()) {
            if (!modelRuntime.isModelLoaded()) {
                val errMsg = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = "AI Agent model not loaded. Go to Settings → AI Agent to download and load a model first.",
                    timestamp = System.currentTimeMillis()
                )
                _messages.update { it + errMsg }
            }
            return
        }
        val userMsg = ChatMessage(
            role = ChatMessage.Role.USER,
            content = text,
            timestamp = System.currentTimeMillis()
        )
        _messages.update { it + userMsg }
        processMessage(text)
    }

    private fun processMessage(userInput: String) {
        scope.launch {
            try {
                _state.value = AiAgentState.THINKING
                // Minimum thinking delay so the UI can show the animation
                kotlinx.coroutines.delay(300)

                val project = ProjectManager.getInstance().currentProject
                val analysis = if (project != null && AiPreferences.isAutoReadEnabled()) {
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
                    val response = modelRuntime.generate(
                        modelInput.toString(),
                        temperature = temperature,
                        maxTokens = maxTokens.coerceAtMost(512)
                    )
                    // If response is an error message, break immediately
                    if (response.startsWith("Error:")) {
                        toolResult = response
                        break
                    }
                    val toolCalls = toolEngine.parseToolCalls(response)
                    if (toolCalls.isEmpty()) {
                        toolResult = response
                        break
                    }
                    for (toolCall in toolCalls) {
                        val result = toolEngine.executeTool(toolCall)
                        modelInput.append("\nTool result: $result\n")
                        contextManager.addToolCall(toolCall.name, toolCall.args, result)
                    }
                    iteration++
                }

                val finalResponse = toolResult ?: modelRuntime.generate(
                    modelInput.toString(),
                    temperature = temperature,
                    maxTokens = maxTokens.coerceAtMost(512)
                )

                // Minimum delay before showing response so UI can animate
                kotlinx.coroutines.delay(200)

                _state.value = AiAgentState.RESPONDING

                val aiMsg = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = finalResponse,
                    timestamp = System.currentTimeMillis()
                )
                _messages.update { it + aiMsg }
                contextManager.addMessage(userInput, finalResponse)

                if (AiPreferences.isAutoModifyEnabled()) {
                    val changes = toolEngine.getPendingChanges()
                    if (changes.isNotEmpty() && AiPreferences.isConfirmEnabled()) {
                        toolEngine.storePendingChanges(changes)
                    } else if (changes.isNotEmpty()) {
                        projectModifier.applyChanges(changes)
                    }
                }

                _state.value = AiAgentState.IDLE
            } catch (e: Exception) {
                _state.value = AiAgentState.ERROR
                val errMsg = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = "Error: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
                _messages.update { it + errMsg }
                modelManager.unloadModel()
            }
        }
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
