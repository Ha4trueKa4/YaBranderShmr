package com.yandex.avatargenerator.feature.feed.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Цвета экрана ленты по макету (тёмная тема).
 *
 * TODO: временно локально для фичи Feed. Когда команда согласует общую
 * дизайн-систему — перенести в app-wide MaterialTheme (color scheme).
 */
internal object FeedColors {
    val Background = Color(0xFF0F0F14)
    val Surface = Color(0xFF1A1A24)
    val SurfaceVariant = Color(0xFF22222F)
    val Border = Color(0xFF27272A)

    val TextPrimary = Color(0xFFF4F4F5)
    val TextSecondary = Color(0xFFA1A1AA)
    val TextMuted = Color(0xFF52525B)

    val Accent = Color(0xFF7C3AED)
    val AccentCyan = Color(0xFF06B6D4)

    // Плейсхолдеры скелетона загрузки (shimmer).
    val ShimmerBase = Color(0xFF27272A)
    val ShimmerHighlight = Color(0xFF3F3F46)
}
