/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.romanize

/**
 * Renders translated text in the Latin alphabet so players who cannot read the
 * target script still get a pronunciation to go on.
 *
 * Implementations must be thread safe and side-effect free: translations are
 * romanised on Paper's async chat threads.
 */
fun interface Romanizer {
    fun romanize(text: String): String
}
