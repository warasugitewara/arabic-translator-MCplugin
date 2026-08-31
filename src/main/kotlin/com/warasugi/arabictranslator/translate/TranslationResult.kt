/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.translate

/**
 * One finished translation.
 *
 * @param text          the translated text as returned by [provider]
 * @param romanization  Latin transliteration, or `null` when disabled
 * @param provider      id of the backend that produced [text] (`deepl`, `mymemory`, …)
 */
data class TranslationResult(
    val text: String,
    val romanization: String?,
    val provider: String,
)
