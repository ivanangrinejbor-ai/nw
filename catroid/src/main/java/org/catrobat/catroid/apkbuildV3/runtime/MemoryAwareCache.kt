package org.catrobat.catroid.apkbuildV3.runtime

import android.util.Log
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Memory-aware LRU cache for the Light Template strategy.
 *
 * Features:
 * - Bounded by both maxEntries and a configurable memory budget.
 * - Automatically evicts least-recently-used entries when limits are exceeded.
 * - Uses WeakReference for values so the GC can reclaim memory under pressure.
 * - Tracks approximate memory usage (stale estimate, but good enough for guardrails).
 *
 * @param K  Key type (e.g., scene name or sprite ID)
 * @param V  Value type (e.g., preloaded scene data)
 * @param maxMemoryBytes  Maximum approximate memory usage before eviction starts
 * @param maxEntries  Maximum number of entries (hard limit)
 */
class MemoryAwareCache<K : Any, V : Any>(
    private val maxMemoryBytes: Long = DEFAULT_MAX_MEMORY,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES
) {
    private val tag = "MemoryAwareCache"
    private val map = Collections.synchronizedMap(
        object : LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, CacheEntry<V>>?): Boolean {
                if (size > maxEntries) {
                    Log.d(tag, "Evicting eldest entry (maxEntries=$maxEntries)")
                    return true
                }
                // Check memory budget on each insertion
                if (size > 1 && approximateMemoryBytes() > maxMemoryBytes) {
                    Log.d(tag, "Evicting entry (memory budget exceeded: " +
                            "${approximateMemoryBytes() / (1024 * 1024)} MB / ${maxMemoryBytes / (1024 * 1024)} MB)")
                    return true
                }
                return false
            }
        }
    )

    private val entrySizes = Collections.synchronizedMap(LinkedHashMap<K, Long>())

    /**
     * Retrieves a cached value, or null if not present / collected.
     */
    operator fun get(key: K): V? {
        val entry = map[key] ?: return null
        return entry.value.get()
    }

    /**
     * Stores a value in the cache with an estimated byte size.
     */
    fun put(key: K, value: V, estimatedSizeBytes: Long = 1024L * 1024L) {
        entrySizes[key] = estimatedSizeBytes
        map[key] = CacheEntry(WeakReference(value))
    }

    /**
     * Removes a specific entry from the cache.
     */
    fun remove(key: K) {
        map.remove(key)
        entrySizes.remove(key)
    }

    /**
     * Clears all cached entries.
     */
    fun clear() {
        map.clear()
        entrySizes.clear()
    }

    /**
     * Returns the current number of cached entries.
     */
    val size: Int get() = map.size

    /**
     * Returns a set of all cached keys (for inspection).
     */
    val keys: Set<K> get() = map.keys.toSet()

    /**
     * Approximate total memory usage of cached entries in bytes.
     */
    fun approximateMemoryBytes(): Long {
        return entrySizes.values.sum()
    }

    companion object {
        private const val DEFAULT_MAX_MEMORY = 64L * 1024 * 1024 // 64 MB
        private const val DEFAULT_MAX_ENTRIES = 50

        /**
         * Creates a cache with memory limit based on device RAM.
         */
        fun forDevice(totalRamMB: Long): MemoryAwareCache<String, Any> {
            val budget = when {
                totalRamMB < 1024 -> 32L * 1024 * 1024       // 32 MB for low-end
                totalRamMB < 2048 -> 64L * 1024 * 1024       // 64 MB for mid-range
                totalRamMB < 4096 -> 128L * 1024 * 1024      // 128 MB for high-end
                else -> 256L * 1024 * 1024                    // 256 MB for flagship
            }
            return MemoryAwareCache(maxMemoryBytes = budget)
        }
    }

    private data class CacheEntry<V : Any>(
        val value: WeakReference<V>
    )
}
