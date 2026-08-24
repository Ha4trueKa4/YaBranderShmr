package com.yandex.avatargenerator.feature.project.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel

private const val GptOss120bId = "gpt-oss-120b/latest"
private const val Qwen3235bId = "qwen3-235b-a22b-fp8/latest"
private const val Gpt56LunaId = "gpt-5.6-luna"

/**
 * Человекочитаемое имя модели: в ответе `/models` его нет вообще — только `id`,
 * поэтому подписи выводит клиент.
 *
 * Известные модели (allow-list бэкенда) берём из `strings.xml` по правилу
 * дизайн-системы. Незнакомую — а завтра бэкенд может добавить любую — не
 * оставляем пустой строкой, а выводим из самого `id`: иначе чип и опция в
 * шторке молча опустели бы.
 */
@Composable
internal fun modelTitle(id: String): String = when (id) {
    GenerationModel.DefaultId -> stringResource(R.string.project_model_deepseek_v4_flash)
    GptOss120bId -> stringResource(R.string.project_model_gpt_oss_120b)
    Qwen3235bId -> stringResource(R.string.project_model_qwen3_235b)
    Gpt56LunaId -> stringResource(R.string.project_model_gpt_5_6_luna)
    else -> id.toFallbackTitle()
}

/**
 * Имя из `id`: берём первый сегмент (хвост вроде `/latest` — версия, а не имя),
 * разбиваем по дефисам и капитализируем части. Чисто числовые не трогаем, чтобы
 * не портить обозначения размера модели.
 */
private fun String.toFallbackTitle(): String = substringBefore('/')
    .split('-')
    .filter { it.isNotEmpty() }
    .joinToString(" ") { part ->
        if (part.all { it.isDigit() }) part else part.replaceFirstChar { it.uppercaseChar() }
    }
    .ifEmpty { this }
