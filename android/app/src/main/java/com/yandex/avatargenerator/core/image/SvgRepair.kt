package com.yandex.avatargenerator.core.image

/**
 * Чинит разметку SVG, которую отверг парсер androidsvg.
 *
 * Нейросеть-генератор изредка пишет в числовое место слово («cx=" seventy"»,
 * «d="M seventy 390h24v24H70z"»). Браузер и rsvg такое прощают — читают как 0 и
 * рисуют дальше, а androidsvg на длине бросает `SVGParseException` и роняет
 * разбор всего документа: превью пропадает целиком. Здесь повторяем щадящее
 * поведение браузера, чтобы уже залитые в S3 битые проекты всё-таки рисовались.
 *
 * Правится только заведомый мусор — токены, которые не число и не команда пути.
 * Валидная разметка через эту функцию проходит без изменений, но вызывать её
 * всё равно стоит лишь после провала обычного разбора (см. [RepairingSvgDecoder]).
 */
internal fun repairSvg(markup: String): String =
    markup
        .replace(NumericAttributeRegex) { match ->
            val (name, value) = match.destructured
            val fallback = if (name.endsWith("opacity")) "1" else "0"
            """$name="${repairNumberList(value, fallback)}""""
        }
        .replace(PathDataAttributeRegex) { match ->
            """d="${repairPathData(match.groupValues[1])}""""
        }

/** Нечисловые токены заменяем на [fallback], разделители и порядок сохраняем. */
private fun repairNumberList(value: String, fallback: String): String =
    value.replace(TokenRegex) { token ->
        if (NumberRegex.matches(token.value)) token.value else fallback
    }

/**
 * В `d` буквы допустимы только как команды пути. Слово из других букв — мусор,
 * он идёт на месте координаты, поэтому подставляем 0. Одиночную `e` не трогаем:
 * она часть числа в экспоненциальной записи (`1e3`).
 */
private fun repairPathData(value: String): String =
    value.replace(PathWordRegex) { word ->
        if (word.value.all { it in PathCommands }) word.value else "0"
    }

private const val PathCommands = "MmZzLlHhVvCcSsQqTtAa"

/**
 * Атрибуты, где androidsvg ждёт длину (или их список) и падает на чужом слове.
 * `points` в этот же список: парсер точек мусор переживает, но молча теряет
 * фигуру, а раз уж чиним документ — чиним целиком.
 */
private val NumericAttributes = listOf(
    "x", "y", "dx", "dy", "cx", "cy", "r", "rx", "ry", "fr",
    "x1", "y1", "x2", "y2", "width", "height", "points",
    "stroke-width", "stroke-dashoffset", "font-size", "offset",
    "opacity", "fill-opacity", "stroke-opacity",
)

// Отрицательный lookbehind — чтобы `width` не поймался внутри `stroke-width`.
private val NumericAttributeRegex = Regex(
    "(?<![\\w-])(" + NumericAttributes.joinToString("|") + ")\\s*=\\s*\"([^\"]*)\"",
)

private val PathDataAttributeRegex = Regex("(?<![\\w-])d\\s*=\\s*\"([^\"]*)\"")

private val TokenRegex = Regex("[^\\s,]+")

private val NumberRegex = Regex(
    "[-+]?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?(px|pt|pc|mm|cm|in|em|ex|%)?",
)

// Буква после цифры или точки — это экспонента числа, а не команда и не слово.
private val PathWordRegex = Regex("(?<![\\d.])[A-Za-z]+")
