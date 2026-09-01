/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.translate

import com.warasugi.arabictranslator.language.LanguageProfile
import com.warasugi.arabictranslator.translate.provider.TranslationException
import com.warasugi.arabictranslator.translate.provider.TranslationProvider
import com.warasugi.arabictranslator.translate.provider.TranslationRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Turns chat lines into translations, in front of a chain of [TranslationProvider]s.
 *
 * Three things happen here that the previous version did not do:
 *
 *  * **Caching** - repeated lines never reach the network ([TranslationCache]).
 *  * **Request coalescing** - if ten players spam the same word at once, exactly one
 *    HTTP request goes out and every caller awaits the same [Deferred].
 *  * **Failover** - a provider that is down, unconfigured or out of quota is skipped
 *    and the next one in the chain answers, instead of the message being dropped.
 */
class TranslationService(
    private val providers: List<TranslationProvider>,
    private val cache: TranslationCache,
    private val sourceLanguage: String?,
    private val scope: CoroutineScope,
    private val logger: Logger,
    private val debug: Boolean,
    private val suspendMillis: Long = DEFAULT_SUSPEND_MILLIS,
) {

    private val inFlight = ConcurrentHashMap<String, Deferred<TranslationResult?>>()
    private val suspendedUntil = ConcurrentHashMap<String, Long>()

    /** `true` when at least one backend is usable; used to reject the enable command. */
    val hasUsableProvider: Boolean get() = providers.any { it.isConfigured }

    val providerIds: List<String> get() = providers.filter { it.isConfigured }.map { it.id }

    suspend fun translate(rawText: String, language: LanguageProfile): TranslationResult? {
        val text = rawText.trim()
        if (text.isEmpty()) return null

        // One cache and one in-flight map for every language; the key carries the
        // target so "hello" in Arabic never returns the Chinese entry.
        val key = "${language.code}\u0000$text"

        cache[key]?.let {
            if (debug) logger.info("Cache hit for ${language.id} \"$text\"")
            return it
        }

        val deferred = inFlight.computeIfAbsent(key) { scope.async { runProviders(text, language, key) } }
        return try {
            deferred.await()
        } finally {
            inFlight.remove(key, deferred)
        }
    }

    private suspend fun runProviders(
        text: String,
        language: LanguageProfile,
        cacheKey: String,
    ): TranslationResult? {
        val request = TranslationRequest(text, language.code, sourceLanguage)
        val failures = mutableListOf<String>()

        for (provider in providers) {
            if (!provider.isConfigured) continue
            if (isSuspended(provider)) {
                failures += "${provider.id}: temporarily suspended after an earlier failure"
                continue
            }

            try {
                val translated = provider.translate(request)
                if (translated.isBlank()) {
                    failures += "${provider.id}: empty translation"
                    continue
                }

                val result = TranslationResult(
                    text = translated,
                    romanization = language.romanizer?.romanize(translated)?.takeIf(String::isNotBlank),
                    provider = provider.id,
                )
                cache[cacheKey] = result
                if (debug) logger.info("Translated \"$text\" to ${language.id} via ${provider.id} -> \"$translated\"")
                return result
            } catch (e: CancellationException) {
                throw e
            } catch (e: TranslationException) {
                failures += "${provider.id}: ${e.message}"
                if (e.fatal) suspendProvider(provider, e.message)
            } catch (e: Exception) {
                failures += "${provider.id}: ${e.javaClass.simpleName} ${e.message.orEmpty()}"
                if (debug) logger.log(Level.WARNING, "${provider.id} threw while translating", e)
            }
        }

        logger.warning(
            "Translation to ${language.id} failed for \"${text.take(60)}\" - " +
                failures.joinToString("; ").ifEmpty { "no provider is configured" },
        )
        return null
    }

    private fun isSuspended(provider: TranslationProvider): Boolean {
        val until = suspendedUntil[provider.id] ?: return false
        if (until > System.currentTimeMillis()) return true
        suspendedUntil.remove(provider.id)
        return false
    }

    private fun suspendProvider(provider: TranslationProvider, reason: String?) {
        if (suspendedUntil.put(provider.id, System.currentTimeMillis() + suspendMillis) == null) {
            logger.warning(
                "Pausing provider '${provider.id}' for ${suspendMillis / 60_000} minutes: " +
                    "${reason.orEmpty()} - fix the config and reload the plugin to resume immediately",
            )
        }
    }

    /** Drops caches and un-suspends every provider; called on reload. */
    fun reset() {
        cache.clear()
        suspendedUntil.clear()
    }

    fun cacheStats(): TranslationCache.Stats = cache.stats()

    private companion object {
        const val DEFAULT_SUSPEND_MILLIS = 15L * 60L * 1000L
    }
}
