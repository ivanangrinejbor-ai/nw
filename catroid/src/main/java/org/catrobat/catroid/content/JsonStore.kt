package org.catrobat.catroid.content

import org.json.JSONArray
import org.json.JSONObject

/** Shared JSON values used by JSON bricks during one stage run. */
object JsonStore {
    private val values = HashMap<String, Any>()

    @Synchronized fun parse(name: String, text: String): Boolean = try {
        values[name] = if (text.trimStart().startsWith("[")) JSONArray(text) else JSONObject(text)
        true
    } catch (_: Exception) { false }

    @Synchronized fun stringify(name: String): String = when (val value = values[name]) {
        is JSONObject -> value.toString()
        is JSONArray -> value.toString()
        else -> ""
    }

    @Synchronized fun clear(name: String) { values.remove(name) }

    @Synchronized fun hasKey(name: String, key: String): Boolean = (values[name] as? JSONObject)?.has(key) == true

    @Synchronized fun get(name: String, key: String): Any? = normalize((values[name] as? JSONObject)?.opt(key))

    @Synchronized fun set(name: String, key: String, value: Any?) {
        val objectValue = values.getOrPut(name) { JSONObject() } as JSONObject
        objectValue.put(key, value ?: JSONObject.NULL)
    }

    @Synchronized fun arrayLength(name: String): Int = (values[name] as? JSONArray)?.length() ?: 0

    @Synchronized fun arrayGet(name: String, index: Int): Any? = normalize((values[name] as? JSONArray)?.opt(index))

    @Synchronized fun put(name: String, value: Any) { values[name] = value }

    @Synchronized fun clearAll() { values.clear() }

    private fun normalize(value: Any?): Any? = if (value == JSONObject.NULL) null else value
}
