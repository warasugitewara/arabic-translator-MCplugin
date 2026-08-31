/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.util

/** Cheap script detection used to skip messages that are already translated. */
object Scripts {

    /**
     * `true` when at least [threshold] of the letters in [text] are Arabic script.
     *
     * Re-translating Arabic into Arabic burns quota and produces noise, so the
     * chat listener uses this to leave such messages alone.
     */
    fun isMostlyArabic(text: String, threshold: Double = 0.5): Boolean =
        isMostly(text, threshold) { Character.UnicodeScript.of(it) == Character.UnicodeScript.ARABIC }

    private inline fun isMostly(text: String, threshold: Double, predicate: (Int) -> Boolean): Boolean {
        var letters = 0
        var matches = 0
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (Character.isLetter(codePoint)) {
                letters++
                if (predicate(codePoint)) matches++
            }
            index += Character.charCount(codePoint)
        }
        return letters > 0 && matches.toDouble() / letters >= threshold
    }
}
