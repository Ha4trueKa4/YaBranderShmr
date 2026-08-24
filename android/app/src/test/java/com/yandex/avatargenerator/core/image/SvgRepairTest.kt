package com.yandex.avatargenerator.core.image

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Разметка в кейсах — из реальных превью, на которых падал androidsvg
 * (`SVGParseException: Invalid length unit specifier: seventy`).
 */
class SvgRepairTest {

    @Test
    fun `слово вместо длины заменяется нулём, отступы значения сохраняются`() {
        assertEquals(
            """<circle cx=" 0" cy="112" r="10"/>""",
            repairSvg("""<circle cx=" seventy" cy="112" r="10"/>"""),
        )
    }

    @Test
    fun `слово внутри пути заменяется нулём`() {
        assertEquals(
            """<path d="M 0 390h24v24H70z"/>""",
            repairSvg("""<path d="M seventy 390h24v24H70z"/>"""),
        )
    }

    @Test
    fun `валидная разметка не меняется`() {
        val markup = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
              <rect x="10.5" y="-4" width="100%" height="2em" stroke-width=".5"/>
              <text x="10 20 30" font-size="12px" fill-opacity="0.4">Ку</text>
              <polygon points="1,2 3,4 5,6"/>
              <path d="M250 105c-42 0-76 34-76 76zm1e3 2L4 5"/>
            </svg>
        """.trimIndent()
        assertEquals(markup, repairSvg(markup))
    }

    @Test
    fun `команды пути из нескольких букв не трогаем`() {
        val markup = """<path d="M405 91h12v28h-12zM397 99h28v12h-28z"/>"""
        assertEquals(markup, repairSvg(markup))
    }

    @Test
    fun `прозрачность чинится единицей, а не нулём`() {
        assertEquals(
            """<g opacity="1" stroke-opacity="1"/>""",
            repairSvg("""<g opacity="half" stroke-opacity="inherit"/>"""),
        )
    }

    @Test
    fun `stroke-width не путается с width`() {
        assertEquals(
            """<rect width="10" stroke-width="0"/>""",
            repairSvg("""<rect width="10" stroke-width="thin"/>"""),
        )
    }

    @Test
    fun `чинится только мусорный токен списка`() {
        assertEquals(
            """<text x="10 0 30"/>""",
            repairSvg("""<text x="10 twenty 30"/>"""),
        )
    }
}
