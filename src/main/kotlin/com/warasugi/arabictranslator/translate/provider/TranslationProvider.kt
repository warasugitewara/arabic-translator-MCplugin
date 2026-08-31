/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.translate.provider

/** A single text to translate. [sourceLanguage] is `null` when auto-detection is wanted. */
data class TranslationRequest(
    val text: String,
    val targetLanguage: String,
    val sourceLanguage: String?,
)

/**
 * A translation backend.
 *
 * Implementations are tried in the order configured under `providers.order`, so a
 * backend that is down, out of quota or unconfigured simply hands over to the next
 * one instead of dropping the message.
 */
interface TranslationProvider {

    /** Config id of this backend, e.g. `deepl`. */
    val id: String

    /** `false` when the backend is missing credentials and should be skipped. */
    val isConfigured: Boolean

    /**
     * Translates [request], suspending until the remote call completes.
     *
     * @throws TranslationException when the backend fails; the service then falls
     *         through to the next provider.
     */
    suspend fun translate(request: TranslationRequest): String
}

/** Thrown by a provider that could not deliver a translation. */
class TranslationException(
    message: String,
    /** `true` when retrying with the same provider is pointless (bad key, quota gone). */
    val fatal: Boolean = false,
    cause: Throwable? = null,
) : Exception(message, cause)
