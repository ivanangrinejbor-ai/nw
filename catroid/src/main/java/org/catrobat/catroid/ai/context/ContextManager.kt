package org.catrobat.catroid.ai.context

import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import java.util.Collections

object ContextManager {

    private const val MAX_HISTORY_SIZE = 50
    private const val MAX_TOOL_HISTORY = 100

    private val conversationHistory = Collections.synchronizedList(mutableListOf<ConversationEntry>())
    private val toolCallHistory = Collections.synchronizedList(mutableListOf<ToolCallEntry>())

    private val cachedScenes = Collections.synchronizedMap(mutableMapOf<String, Scene>())
    private val cachedObjects = Collections.synchronizedMap(mutableMapOf<String, MutableMap<String, Sprite>>())
    private val analyzedProjects = Collections.synchronizedSet(mutableSetOf<String>())

    data class ConversationEntry(
        val userMessage: String,
        val aiResponse: String,
        val timestamp: Long
    )

    data class ToolCallEntry(
        val toolName: String,
        val args: Map<String, String>,
        val result: String,
        val timestamp: Long
    )

    fun addMessage(userMessage: String, aiResponse: String) {
        synchronized(conversationHistory) {
            conversationHistory.add(
                ConversationEntry(userMessage, aiResponse, System.currentTimeMillis())
            )
            if (conversationHistory.size > MAX_HISTORY_SIZE) {
                conversationHistory.removeAt(0)
            }
        }
    }

    fun addToolCall(name: String, args: Map<String, String>, result: String) {
        synchronized(toolCallHistory) {
            toolCallHistory.add(ToolCallEntry(name, args, result, System.currentTimeMillis()))
            if (toolCallHistory.size > MAX_TOOL_HISTORY) {
                toolCallHistory.removeAt(0)
            }
        }
    }

    fun getRecentHistory(count: Int = 10): List<ConversationEntry> {
        synchronized(conversationHistory) {
            return conversationHistory.takeLast(count).toList()
        }
    }

    fun getRecentToolCalls(count: Int = 20): List<ToolCallEntry> {
        synchronized(toolCallHistory) {
            return toolCallHistory.takeLast(count).toList()
        }
    }

    fun cacheScene(scene: Scene) {
        cachedScenes[scene.name] = scene
    }

    fun getCachedScene(name: String): Scene? = cachedScenes[name]

    fun cacheObject(sceneName: String, sprite: Sprite) {
        cachedObjects.getOrPut(sceneName) { Collections.synchronizedMap(mutableMapOf()) }[sprite.name] = sprite
    }

    fun getCachedObject(sceneName: String, objectName: String): Sprite? {
        return cachedObjects[sceneName]?.get(objectName)
    }

    fun isProjectAnalyzed(projectName: String): Boolean = analyzedProjects.contains(projectName)

    fun markProjectAnalyzed(projectName: String) {
        analyzedProjects.add(projectName)
        if (analyzedProjects.size > 100) {
            analyzedProjects.clear()
        }
    }

    fun clear() {
        conversationHistory.clear()
        toolCallHistory.clear()
        cachedScenes.clear()
        synchronized(cachedObjects) { cachedObjects.clear() }
    }

    fun invalidateProjectCache() {
        cachedScenes.clear()
        synchronized(cachedObjects) { cachedObjects.clear() }
        analyzedProjects.clear()
    }

    fun clearAnalysis() {
        analyzedProjects.clear()
    }

    fun getConversationSummary(): String {
        synchronized(conversationHistory) {
            if (conversationHistory.isEmpty()) return "No conversation history."
            return conversationHistory.takeLast(5).joinToString("\n") { entry ->
                "User: ${entry.userMessage.take(200)}\nAI: ${entry.aiResponse.take(200)}"
            }
        }
    }
}
