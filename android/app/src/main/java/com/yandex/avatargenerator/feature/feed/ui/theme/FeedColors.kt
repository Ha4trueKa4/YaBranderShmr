package com.yandex.avatargenerator.feature.feed.ui.theme

import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Цвета экрана ленты (тёмная тема).
 *
 * Псевдонимы токенов из общей палитры [AppColors] — чтобы код ленты остался
 * читаемым (FeedColors.*), а значения жили в едином месте и не расходились.
 *
 * TODO: когда появится app-wide MaterialTheme color scheme — брать цвета из
 * MaterialTheme.colorScheme и удалить этот объект.
 */
internal object FeedColors {
    val Background = AppColors.Background
    val Surface = AppColors.Surface
    val SurfaceVariant = AppColors.SurfaceVariant
    val Border = AppColors.Border

    val TextPrimary = AppColors.TextPrimary
    val TextSecondary = AppColors.TextSecondary
    val TextMuted = AppColors.TextMuted

    val Accent = AppColors.Accent
    val AccentCyan = AppColors.AccentCyan
}
