package com.warasugi.arabictranslator.translate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TranslationCacheTest {

    private fun result(text: String) = TranslationResult(text, null, "test")

    @Test
    fun `returns stored values`() {
        val cache = TranslationCache(maxEntries = 4, ttlMillis = 1_000)
        cache["hello"] = result("مرحبا")
        assertEquals("مرحبا", cache["hello"]?.text)
    }

    @Test
    fun `evicts the least recently used entry`() {
        val cache = TranslationCache(maxEntries = 2, ttlMillis = 60_000)
        cache["a"] = result("1")
        cache["b"] = result("2")
        cache["a"] // touch a, so b becomes the eviction candidate
        cache["c"] = result("3")

        assertEquals("1", cache["a"]?.text)
        assertNull(cache["b"])
        assertEquals("3", cache["c"]?.text)
    }

    @Test
    fun `drops entries once the ttl has passed`() {
        var now = 0L
        val cache = TranslationCache(maxEntries = 8, ttlMillis = 100) { now }
        cache["a"] = result("1")

        now = 99
        assertEquals("1", cache["a"]?.text)

        now = 100
        assertNull(cache["a"])
        assertEquals(0, cache.size)
    }

    @Test
    fun `a zero capacity cache stores nothing`() {
        val cache = TranslationCache(maxEntries = 0, ttlMillis = 1_000)
        cache["a"] = result("1")
        assertNull(cache["a"])
    }

    @Test
    fun `tracks hit and miss counts`() {
        val cache = TranslationCache(maxEntries = 4, ttlMillis = 60_000)
        cache["a"] = result("1")
        cache["a"]
        cache["b"]

        val stats = cache.stats()
        assertEquals(1, stats.hits)
        assertEquals(1, stats.misses)
        assertEquals(0.5, stats.hitRate)
    }
}
