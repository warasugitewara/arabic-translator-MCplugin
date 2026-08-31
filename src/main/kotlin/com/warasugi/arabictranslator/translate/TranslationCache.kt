/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.translate

/**
 * Bounded LRU cache with a time-to-live, guarding the translation API quota.
 *
 * Chat repeats itself a lot ("hi", "gg", "lol"), so a hit here is a request that
 * never leaves the server. Entries are evicted by least-recent *use*, not by
 * insertion order, and stale entries are dropped lazily on read.
 *
 * All methods are thread safe: chat events arrive on Paper's async chat threads
 * while commands touch the cache from the main thread.
 */
class TranslationCache(
    private val maxEntries: Int,
    private val ttlMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private data class Entry(val value: TranslationResult, val expiresAt: Long)

    private val entries = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean =
            size > maxEntries
    }

    private var hits = 0L
    private var misses = 0L

    operator fun get(key: String): TranslationResult? = synchronized(entries) {
        val entry = entries[key]
        when {
            entry == null -> null
            entry.expiresAt <= clock() -> {
                entries.remove(key)
                null
            }

            else -> entry.value
        }.also { if (it == null) misses++ else hits++ }
    }

    operator fun set(key: String, value: TranslationResult) {
        if (maxEntries <= 0) return
        synchronized(entries) { entries[key] = Entry(value, clock() + ttlMillis) }
    }

    fun clear(): Unit = synchronized(entries) {
        entries.clear()
        hits = 0
        misses = 0
    }

    val size: Int get() = synchronized(entries) { entries.size }

    fun stats(): Stats = synchronized(entries) { Stats(entries.size, maxEntries, hits, misses) }

    data class Stats(val size: Int, val capacity: Int, val hits: Long, val misses: Long) {
        val hitRate: Double get() = if (hits + misses == 0L) 0.0 else hits.toDouble() / (hits + misses)
    }
}
