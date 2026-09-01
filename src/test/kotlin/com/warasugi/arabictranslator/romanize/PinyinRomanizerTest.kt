package com.warasugi.arabictranslator.romanize

import kotlin.test.Test
import kotlin.test.assertEquals

class PinyinRomanizerTest {

    private val marks = PinyinRomanizer(ToneStyle.MARKS)
    private val numbers = PinyinRomanizer(ToneStyle.NUMBERS)
    private val toneless = PinyinRomanizer(ToneStyle.NONE)

    @Test
    fun `reads simplified characters with tone marks`() {
        assertEquals("nǐ hǎo", marks.romanize("你好"))
    }

    @Test
    fun `reads traditional characters too`() {
        assertEquals("xiè xiè", marks.romanize("謝謝"))
    }

    @Test
    fun `writes tones as numbers when asked`() {
        assertEquals("ni3 hao3", numbers.romanize("你好"))
    }

    @Test
    fun `can drop tones entirely`() {
        assertEquals("ni hao", toneless.romanize("你好"))
    }

    @Test
    fun `keeps the u umlaut instead of falling back to v`() {
        assertEquals("lǜ", marks.romanize("绿"))
    }

    @Test
    fun `covers characters far outside the old lookup table`() {
        assertEquals("zuàn shí", marks.romanize("钻石"))
    }

    @Test
    fun `leaves latin text and digits alone`() {
        assertEquals("Steve 42", marks.romanize("Steve 42"))
    }

    @Test
    fun `keeps punctuation attached to the preceding syllable`() {
        assertEquals("nǐ hǎo, Steve!", marks.romanize("你好, Steve!"))
    }

    @Test
    fun `is stable for empty input`() {
        assertEquals("", marks.romanize(""))
    }
}
