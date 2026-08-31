package com.warasugi.arabictranslator.romanize

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArabicRomanizerTest {

    private val readable = ArabicRomanizer(RomanizationStyle.SIMPLE, insertShortVowels = true)
    private val literal = ArabicRomanizer(RomanizationStyle.SIMPLE, insertShortVowels = false)
    private val academic = ArabicRomanizer(RomanizationStyle.ACADEMIC, insertShortVowels = false)

    @Test
    fun `maps letters one by one without vowel insertion`() {
        assertEquals("mrhba", literal.romanize("مرحبا"))
    }

    @Test
    fun `opens consonant clusters so the result is pronounceable`() {
        assertEquals("marahba", readable.romanize("مرحبا"))
    }

    @Test
    fun `gives a bare two-consonant word a vowel to stand on`() {
        assertEquals("man", readable.romanize("من"))
    }

    @Test
    fun `assimilates the definite article before sun letters`() {
        assertEquals("ash-shams", readable.romanize("الشمس"))
    }

    @Test
    fun `keeps the definite article intact before moon letters`() {
        assertEquals("al-qamr", readable.romanize("القمر"))
    }

    @Test
    fun `honours explicit short vowels when the text is vocalised`() {
        assertEquals("marhaban", literal.romanize("مَرْحَبًا"))
    }

    @Test
    fun `geminates a consonant carrying shadda`() {
        assertEquals("al-lh", literal.romanize("اللّه"))
        assertEquals("al-lah", readable.romanize("اللّه"))
    }

    @Test
    fun `passes latin text digits and punctuation through untouched`() {
        assertEquals("Steve 42! <3", readable.romanize("Steve 42! <3"))
    }

    @Test
    fun `converts arabic indic digits and punctuation`() {
        assertEquals("2026, ok?", readable.romanize("٢٠٢٦، ok؟"))
    }

    @Test
    fun `academic style uses ala-lc diacritics`() {
        assertEquals("ṣbāḥ", academic.romanize("صباح"))
    }

    @Test
    fun `is stable for an empty or blank message`() {
        assertEquals("", readable.romanize(""))
        assertEquals("", readable.romanize("   "))
    }

    @Test
    fun `never returns arabic characters`() {
        val output = readable.romanize("السلام عليكم ورحمة الله وبركاته")
        assertTrue(output.none { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.ARABIC }, output)
    }
}
