package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.network.ApiConfig

/**
 * Тайминги опроса статуса генерации. Отдельным параметром
 * [ChatRepositoryImpl], а не константами по месту: тест на реальных значениях
 * ждал бы пять минут, а виртуальное время `runTest` здесь не помогает — движок
 * Ktor уходит на свой диспетчер, планировщик не видит задач и сам перематывает
 * часы на дедлайн.
 *
 * @property statusTimeoutMillis таймаут одного long-poll запроса.
 * @property guardMillis пауза между опросами — предохранитель от горячего цикла.
 * @property deadlineMillis общий дедлайн ожидания.
 */
data class GenerationPolling(
    val statusTimeoutMillis: Long = ApiConfig.StatusPollTimeoutMillis,
    val guardMillis: Long = ApiConfig.StatusPollGuardMillis,
    val deadlineMillis: Long = ApiConfig.GenerationDeadlineMillis,
)
