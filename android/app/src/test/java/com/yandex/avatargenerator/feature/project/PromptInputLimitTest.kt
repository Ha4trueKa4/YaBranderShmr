package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.ui.MaxPromptBytes
import com.yandex.avatargenerator.feature.project.ui.limitPromptBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptInputLimitTest {

    @Test
    fun `текст короче лимита возвращается как есть`() {
        val text = "Violet logo, please"

        assertSame(text, limitPromptBytes(text))
    }

    @Test
    fun `лимит считается в байтах, а не в символах`() {
        // 1500 латинских символов — ровно 1500 байт, влезают целиком.
        val latin = "a".repeat(MaxPromptBytes)

        assertEquals(MaxPromptBytes, limitPromptBytes(latin).length)
    }

    @Test
    fun `кириллицы влезает вдвое меньше`() {
        val cyrillic = "я".repeat(MaxPromptBytes)

        val limited = limitPromptBytes(cyrillic)

        assertEquals(MaxPromptBytes / 2, limited.length)
        assertEquals(MaxPromptBytes, limited.toByteArray().size)
    }

    @Test
    fun `суррогатная пара не разрезается пополам`() {
        // Эмодзи — 4 байта и два char'а. При лимите в 5 байт после трёх байт
        // текста он не влезает целиком, значит не должен влезть вовсе.
        val limited = limitPromptBytes("абв🙂", maxBytes = 7)

        assertEquals("абв", limited)
        assertTrue(limited.toByteArray().size <= 7)
    }

    @Test
    fun `многобайтный символ на границе отбрасывается целиком`() {
        assertEquals("ая", limitPromptBytes("аяя", maxBytes = 4))
    }

    @Test
    fun `пустой текст остаётся пустым`() {
        assertEquals("", limitPromptBytes(""))
    }
}
