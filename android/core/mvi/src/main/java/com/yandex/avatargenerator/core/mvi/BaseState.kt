package com.yandex.avatargenerator.core.mvi

/**
 * Состояние экрана целиком: один immutable `data class` со всеми полями,
 * включая флаги загрузки и ошибки. Отдельных потоков состояния у экрана нет.
 */
interface BaseState
