package com.yandex.avatargenerator.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Канонические цветовые токены приложения (тёмная тема) по макету.
 *
 * Единый источник палитры для всех фич (навигационный скелет, лента и т.д.) —
 * ссылайтесь сюда, чтобы значения не расходились.
 *
 * TODO: когда команда согласует полноценную дизайн-систему — завести на этих
 * токенах app-wide MaterialTheme color scheme и раздавать через MaterialTheme.
 */
internal object AppColors {
    // ВАЖНО: при изменении Background синхронно правь @color/window_background
    // (colors.xml) — это же значение для windowBackground до отрисовки Compose.
    val Background = Color(0xFF0F0F14)
    val Surface = Color(0xFF1A1A24)
    val SurfaceVariant = Color(0xFF22222F)
    val Border = Color(0xFF27272A)

    val TextPrimary = Color(0xFFF4F4F5)
    val TextSecondary = Color(0xFFA1A1AA)

    /** Приглушённый текст / неактивные элементы (например, вкладки нав-панели). */
    val TextMuted = Color(0xFF52525B)

    val Accent = Color(0xFF7C3AED)
    val AccentCyan = Color(0xFF06B6D4)

    /** Цвет содержимого поверх заливки Accent (кнопки, беджи) — всегда белый по макету. */
    val OnAccent = Color.White

    val Success = Color(0xFF22C55E)
    val Error = Color(0xFFEF4444)

    /** Диагональный градиент Accent → AccentCyan — аватары, превью-плашки проектов. */
    val AccentGradient = Brush.linearGradient(listOf(Accent, AccentCyan))

    // Плейсхолдеры скелетона загрузки (shimmer).
    val ShimmerBase = Color(0xFF27272A)
    val ShimmerHighlight = Color(0xFF3F3F46)
}
