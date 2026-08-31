/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.romanize

/**
 * Output flavour for [ArabicRomanizer].
 *
 * [SIMPLE] sticks to plain ASCII, which every Minecraft font renders.
 * [ACADEMIC] uses the ALA-LC style diacritics (ḥ, ṣ, ā, ʿ …); prettier, but the
 * client needs a font with Latin Extended Additional coverage to show it.
 */
enum class RomanizationStyle {
    SIMPLE,
    ACADEMIC,
    ;

    companion object {
        fun fromString(value: String?): RomanizationStyle =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: SIMPLE
    }
}

/**
 * Rule-based Arabic -> Latin transliteration.
 *
 * Machine translation output is unvocalised, so a letter-for-letter mapping alone
 * produces unreadable consonant runs ("mrhba"). [insertShortVowels] therefore drops
 * a default `a` wherever three consonants would otherwise collide, which is enough
 * to make the result pronounceable ("marahba", "ash-shams") without inventing
 * vowels Arabic would not have there.
 *
 * It is a pronunciation aid, not a reversible transcription: without diacritics in
 * the source, no rule set can recover the original short vowels.
 *
 * The class is immutable and has no Bukkit dependencies, so it is safe to use
 * from any thread and is covered by unit tests.
 */
class ArabicRomanizer(
    private val style: RomanizationStyle = RomanizationStyle.SIMPLE,
    private val insertShortVowels: Boolean = true,
) {

    fun romanize(text: String): String {
        if (text.isEmpty()) return text

        val out = StringBuilder(text.length * 2)
        var index = 0
        while (index < text.length) {
            val start = index
            // Words are runs of Arabic letters; everything else is copied verbatim so
            // that Latin words, digits, emoji and colour codes survive untouched.
            if (isArabicLetter(text[index])) {
                while (index < text.length && isArabicLetter(text[index])) index++
                out.append(romanizeWord(text, start, index))
            } else {
                out.append(mapNonLetter(text[index]))
                index++
            }
        }
        return out.toString().replace(MULTI_SPACE, " ").trim()
    }

    // -- word level ---------------------------------------------------------

    private fun romanizeWord(source: CharSequence, fromIndex: Int, toIndex: Int): String {
        var cursor = fromIndex
        var ignoreShaddaAt = -1
        val prefix = StringBuilder()

        // Definite article: al-qamar (moon letters) vs. ash-shams (sun letters).
        if (toIndex - cursor > 2 && source[cursor] == ALEF && source[cursor + 1] == LAM) {
            val following = source[cursor + 2]
            val letter = LETTERS[following]
            if (letter != null && letter.kind == Kind.CONSONANT) {
                if (following in SUN_LETTERS) {
                    prefix.append('a').append(letter.text()).append('-')
                } else {
                    prefix.append("al-")
                }
                cursor += 2
                // A shadda on the assimilated letter only marks the assimilation we
                // just wrote out, so it must not double the consonant again.
                if (source.getOrNull(cursor + 1) == SHADDA) ignoreShaddaAt = cursor + 1
            }
        }

        val units = buildUnits(source, cursor, toIndex, ignoreShaddaAt)
        return prefix.append(joinUnits(units)).toString()
    }

    private fun buildUnits(
        source: CharSequence,
        fromIndex: Int,
        toIndex: Int,
        ignoreShaddaAt: Int = -1,
    ): List<Sound> {
        val units = ArrayList<Sound>(toIndex - fromIndex)
        for (i in fromIndex until toIndex) {
            when (val ch = source[i]) {
                TATWEEL, SUKUN -> {} // purely orthographic, contributes no sound
                SHADDA -> if (i != ignoreShaddaAt) units.lastOrNull()
                    ?.takeIf { it.kind == Kind.CONSONANT }
                    ?.let { units[units.lastIndex] = it.copy(text = it.text + it.text) }

                // The alif that props up a tanwin fatha is mute: -an, not -ana.
                ALEF -> if (source.getOrNull(i - 1) != TANWIN_FATH) {
                    LETTERS[ch]?.let { units += Sound(it.text(), it.kind) }
                }

                else -> LETTERS[ch]?.let { units += Sound(it.text(), it.kind) }
            }
        }
        return units
    }

    /**
     * Joins the sounds of one word, opening up consonant clusters on the way.
     *
     * A default `a` goes in only where it earns its place: between two consonants
     * that a third consonant follows (so `sh-m-s` stays "shams" rather than turning
     * into "shamas"), plus in bare two-consonant words that would otherwise carry no
     * vowel at all ("mn" -> "man").
     */
    private fun joinUnits(units: List<Sound>): String {
        val vowelless = units.size == 2 && units.none { it.kind == Kind.VOWEL }
        val sb = StringBuilder()
        for ((i, unit) in units.withIndex()) {
            sb.append(unit.text)
            if (!insertShortVowels || unit.kind != Kind.CONSONANT) continue
            if (units.getOrNull(i + 1)?.kind != Kind.CONSONANT) continue
            if (vowelless || units.getOrNull(i + 2)?.kind == Kind.CONSONANT) sb.append('a')
        }
        return sb.toString()
    }

    private fun mapNonLetter(ch: Char): String = when (ch) {
        in ARABIC_INDIC_DIGITS -> ('0' + (ch - ARABIC_INDIC_DIGITS.first)).toString()
        in EXTENDED_ARABIC_DIGITS -> ('0' + (ch - EXTENDED_ARABIC_DIGITS.first)).toString()
        '،' -> ","
        '؛' -> ";"
        '؟' -> "?"
        '٪' -> "%"
        '٫' -> "."
        '٬' -> ","
        '۔' -> "."
        '«', '»' -> "\""
        else -> ch.toString()
    }

    private fun Letter.text(): String = if (style == RomanizationStyle.ACADEMIC) academic else simple

    // -- data ---------------------------------------------------------------

    private enum class Kind { CONSONANT, VOWEL }

    private data class Sound(val text: String, val kind: Kind)

    private data class Letter(val simple: String, val academic: String, val kind: Kind)

    private companion object {
        const val ALEF = 'ا'
        const val LAM = 'ل'
        const val TATWEEL = 'ـ'
        const val SHADDA = 'ّ'
        const val TANWIN_FATH = 'ً'
        const val SUKUN = 'ْ'

        val MULTI_SPACE = Regex(" {2,}")
        val ARABIC_INDIC_DIGITS = '٠'..'٩'
        val EXTENDED_ARABIC_DIGITS = '۰'..'۹'

        /** Letters that assimilate the `l` of the definite article. */
        val SUN_LETTERS = charArrayOf(
            'ت', 'ث', 'د', 'ذ', 'ر', 'ز', 'س',
            'ش', 'ص', 'ض', 'ط', 'ظ', 'ل', 'ن',
        ).toSet()

        val LETTERS: Map<Char, Letter> = buildMap {
            fun consonant(ch: Char, simple: String, academic: String = simple) =
                put(ch, Letter(simple, academic, Kind.CONSONANT))

            fun vowel(ch: Char, simple: String, academic: String = simple) =
                put(ch, Letter(simple, academic, Kind.VOWEL))

            // Hamza carriers
            consonant('ء', "'", "ʾ")          // ء
            vowel('آ', "aa", "ā")             // آ
            vowel('أ', "a", "ʾa")             // أ
            consonant('ؤ', "'", "ʾ")          // ؤ
            vowel('إ', "i", "ʾi")             // إ
            consonant('ئ', "'", "ʾ")          // ئ

            // Core alphabet
            vowel('ا', "a", "ā")              // ا
            consonant('ب', "b")                    // ب
            vowel('ة', "a", "ah")                  // ة
            consonant('ت', "t")                    // ت
            consonant('ث', "th", "ṯ")         // ث
            consonant('ج', "j")                    // ج
            consonant('ح', "h", "ḥ")          // ح
            consonant('خ', "kh", "ẖ")         // خ
            consonant('د', "d")                    // د
            consonant('ذ', "dh", "ḏ")         // ذ
            consonant('ر', "r")                    // ر
            consonant('ز', "z")                    // ز
            consonant('س', "s")                    // س
            consonant('ش', "sh", "š")         // ش
            consonant('ص', "s", "ṣ")          // ص
            consonant('ض', "d", "ḍ")          // ض
            consonant('ط', "t", "ṭ")          // ط
            consonant('ظ', "z", "ẓ")          // ظ
            consonant('ع', "'", "ʿ")          // ع
            consonant('غ', "gh", "ġ")         // غ
            consonant('ف', "f")                    // ف
            consonant('ق', "q")                    // ق
            consonant('ك', "k")                    // ك
            consonant('ل', "l")                    // ل
            consonant('م', "m")                    // م
            consonant('ن', "n")                    // ن
            consonant('ه', "h")                    // ه
            consonant('و', "w")                    // و
            vowel('ى', "a", "ā")              // ى
            consonant('ي', "y")                    // ي

            // Short vowels and nunation
            vowel('ً', "an")                       // ً
            vowel('ٌ', "un")                       // ٌ
            vowel('ٍ', "in")                       // ٍ
            vowel('َ', "a")                        // َ
            vowel('ُ', "u")                        // ُ
            vowel('ِ', "i")                        // ِ
            vowel('ٰ', "a", "ā")              // ٰ

            // Letters borrowed by Persian/Urdu that DeepL occasionally emits
            consonant('پ', "p")                    // پ
            consonant('چ', "ch")                   // چ
            consonant('ژ', "zh", "ž")         // ژ
            consonant('ک', "k")                    // ک
            consonant('گ', "g")                    // گ
            consonant('ی', "y")                    // ی
            consonant('ہ', "h")                    // ہ
        }

        fun isArabicLetter(ch: Char): Boolean = LETTERS.containsKey(ch) ||
            ch == TATWEEL || ch == SHADDA || ch == SUKUN
    }
}
