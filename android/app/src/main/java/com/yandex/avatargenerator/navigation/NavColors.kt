package com.yandex.avatargenerator.navigation

import androidx.compose.ui.graphics.Color

/**
 * Цвета навигационного скелета (нижняя панель, плейсхолдеры) в тёмной теме.
 *
 * Значения повторяют палитру экрана ленты (feature/feed `FeedColors`), чтобы
 * скелет визуально совпадал с основным экраном.
 *
 * TODO: временная локальная палитра. Когда команда согласует общую
 * дизайн-систему — заменить на app-wide MaterialTheme color scheme и удалить.
 */
internal object NavColors {
    val Background = Color(0xFF0F0F14)
    val Surface = Color(0xFF1A1A24)
    val Border = Color(0xFF27272A)

    val TextPrimary = Color(0xFFF4F4F5)
    val TextSecondary = Color(0xFFA1A1AA)

    /** Неактивные вкладки нижней панели (иконка + подпись). */
    val NavInactive = Color(0xFF52525B)

    /** Акцент — активная вкладка. */
    val Accent = Color(0xFF7C3AED)
}
