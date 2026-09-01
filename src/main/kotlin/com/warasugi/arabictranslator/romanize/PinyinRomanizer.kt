/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.romanize

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

/** How tones are written in the pinyin output. */
enum class ToneStyle {
    /** Diacritics: `nǐ hǎo`. Prettiest, and Minecraft's font has the glyphs. */
    MARKS,

    /** Trailing digits: `ni3 hao3`. Easiest to type and to search for. */
    NUMBERS,

    /** No tone information at all: `ni hao`. */
    NONE,
    ;

    companion object {
        fun fromString(value: String?): ToneStyle =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: MARKS
    }
}

/**
 * Hanzi -> Hanyu Pinyin, backed by pinyin4j.
 *
 * The 1.x plugin carried a hand-written `when` over roughly 200 characters and
 * dropped everything else, so most real sentences came out half-empty. pinyin4j
 * ships the full Unihan reading table and covers traditional characters too.
 *
 * Readings are cached per character: chat is repetitive and a cache hit is far
 * cheaper than pinyin4j's dictionary lookup on the chat path.
 */
class PinyinRomanizer(toneStyle: ToneStyle = ToneStyle.MARKS) : Romanizer {

    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = when (toneStyle) {
            ToneStyle.MARKS -> HanyuPinyinToneType.WITH_TONE_MARK
            ToneStyle.NUMBERS -> HanyuPinyinToneType.WITH_TONE_NUMBER
            ToneStyle.NONE -> HanyuPinyinToneType.WITHOUT_TONE
        }
        // WITH_TONE_MARK is only legal together with WITH_U_UNICODE (ü), and using
        // the real character everywhere keeps `lǜ` from turning into `lv`.
        vCharType = HanyuPinyinVCharType.WITH_U_UNICODE
    }

    private val readings = HashMap<Char, String?>(512)

    override fun romanize(text: String): String {
        val out = StringBuilder(text.length * 3)
        var previousWasSyllable = false

        for (char in text) {
            val syllable = reading(char)
            if (syllable != null) {
                if (out.isNotEmpty() && out.last() != ' ') out.append(' ')
                out.append(syllable)
                previousWasSyllable = true
            } else {
                // Latin words, digits and punctuation are kept as typed; a space is
                // inserted only where a syllable would otherwise run into them.
                if (previousWasSyllable && !char.isWhitespace() && char !in CLOSING_PUNCTUATION) out.append(' ')
                out.append(char)
                previousWasSyllable = false
            }
        }
        return out.toString().trim()
    }

    /**
     * First reading of [char], or `null` when it is not a Han character.
     *
     * pinyin4j returns every reading of a polyphone (`长` is both `cháng` and
     * `zhǎng`) without any context to choose from, so the most common one - which
     * pinyin4j lists first - is used.
     */
    private fun reading(char: Char): String? = synchronized(readings) {
        readings.getOrPut(char) {
            runCatching { PinyinHelper.toHanyuPinyinStringArray(char, format) }
                .getOrNull()
                ?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let(::fixThirdTone)
        }
    }

    /**
     * Repairs pinyin4j's third-tone vowels.
     *
     * pinyin4j writes the third tone with a breve (`nĭ hăo`) where Hanyu Pinyin
     * uses a caron (`nǐ hǎo`). It gets `ǚ` right, so only the five base vowels
     * need swapping.
     */
    private fun fixThirdTone(syllable: String): String {
        if (syllable.none { it in THIRD_TONE_BREVES }) return syllable
        return buildString(syllable.length) {
            syllable.forEach { append(THIRD_TONE_CARONS[it] ?: it) }
        }
    }

    private companion object {
        /** Punctuation that hugs the preceding syllable rather than starting a word. */
        val CLOSING_PUNCTUATION = setOf(',', '.', '!', '?', ':', ';', ')', ']', '}', '，', '。', '！', '？', '、')

        val THIRD_TONE_CARONS = mapOf(
            '\u0103' to '\u01CE', // a breve -> a caron
            '\u0115' to '\u011B', // e
            '\u012D' to '\u01D0', // i
            '\u014F' to '\u01D2', // o
            '\u016D' to '\u01D4', // u
        )

        val THIRD_TONE_BREVES = THIRD_TONE_CARONS.keys
    }
}
