package org.catrobat.catroid.apkbuildV3.runtime

import android.util.Log
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.LinkedHashMap

class MemoryAwareCache<K : Any, V : Any>(
    private val maxMemoryBytes: Long = DEFAULT_MAX_MEMORY,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES
) {
    private val tag = "MemoryAwareCache"
    private val map = Collections.synchronizedMap(
        object : LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, CacheEntry<V>>?): Boolean {
                if (size > maxEntries) {
                    return true
                }
                if (size > 1 && approximateMemoryBytes() > maxMemoryBytes) {
                    return true
                }
                return false
            }
        }
    )

    private val entrySizes = Collections.synchronizedMap(LinkedHashMap<K, Long>())

    operator fun get(key: K): V? {
        val entry = map[key] ?: return null
        return entry.value.get()
    }

    fun put(key: K, value: V, estimatedSizeBytes: Long = 1024L * 1024L) {
        entrySizes[key] = estimatedSizeBytes
        map[key] = CacheEntry(WeakReference(value))
    }

    fun remove(key: K) {
        map.remove(key)
        entrySizes.remove(key)
    }

    fun clear() {
        map.clear()
        entrySizes.clear()
    }

    val size: Int get() = map.size

    val keys: Set<K> get() = map.keys.toSet()

    fun approximateMemoryBytes(): Long {
        return entrySizes.values.sum()
    }

    companion object {
        private const val DEFAULT_MAX_MEMORY = 64L * 1024 * 1024
        private const val DEFAULT_MAX_ENTRIES = 50

        fun forDevice(totalRamMB: Long): MemoryAwareCache<String, Any> {
            val budget = when {
                totalRamMB < 1024 -> 32L * 1024 * 1024
                totalRamMB < 2048 -> 64L * 1024 * 1024
                totalRamMB < 4096 -> 128L * 1024 * 1024
                else -> 256L * 1024 * 1024
            }
            return MemoryAwareCache(maxMemoryBytes = budget)
        }
    }

    private data class CacheEntry<V : Any>(
        val value: WeakReference<V>
    )
}
