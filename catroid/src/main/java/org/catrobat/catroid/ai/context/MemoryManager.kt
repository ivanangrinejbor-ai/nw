package org.catrobat.catroid.ai.context

import android.content.Context
import org.catrobat.catroid.ai.settings.AiPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object MemoryManager {

    private const val MEMORY_FILE = "ai_agent_memory.json"
    private var memoryFile: File? = null

    private val memories = mutableListOf<MemoryEntry>()

    data class MemoryEntry(
        val key: String,
        val content: String,
        val category: MemoryCategory,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class MemoryCategory {
        PROJECT_FACT,
        USER_PREFERENCE,
        CODE_PATTERN,
        TASK_CONTEXT,
        ERROR_PATTERN
    }

    fun init(appContext: Context) {
        memoryFile = File(appContext.filesDir, MEMORY_FILE)
        load()
    }

    fun remember(key: String, content: String, category: MemoryCategory = MemoryCategory.PROJECT_FACT) {
        memories.removeAll { it.key == key }
        memories.add(MemoryEntry(key, content, category))
        save()
    }

    fun recall(key: String): String? {
        return memories.find { it.key == key }?.content
    }

    fun recallByCategory(category: MemoryCategory): List<MemoryEntry> {
        return memories.filter { it.category == category }
    }

    fun search(query: String): List<MemoryEntry> {
        val q = query.lowercase()
        return memories.filter { it.key.lowercase().contains(q) || it.content.lowercase().contains(q) }
    }

    fun getSummary(): String {
        if (memories.isEmpty()) return "No stored memories."
        return memories.joinToString("\n") { "[${it.category.name}] ${it.key}: ${it.content.take(100)}" }
    }

    fun clear() {
        memories.clear()
        save()
    }

    private fun load() {
        try {
            val file = memoryFile ?: return
            if (!file.exists()) return
            val json = JSONObject(file.readText())
            val arr = json.getJSONArray("memories")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                memories.add(MemoryEntry(
                    key = obj.getString("key"),
                    content = obj.getString("content"),
                    category = MemoryCategory.valueOf(obj.optString("category", "PROJECT_FACT")),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                ))
            }
        } catch (_: Exception) {}
    }

    private fun save() {
        try {
            val file = memoryFile ?: return
            val arr = JSONArray()
            for (entry in memories) {
                arr.put(JSONObject().apply {
                    put("key", entry.key)
                    put("content", entry.content)
                    put("category", entry.category.name)
                    put("timestamp", entry.timestamp)
                })
            }
            file.writeText(JSONObject().put("memories", arr).toString())
        } catch (_: Exception) {}
    }
}
