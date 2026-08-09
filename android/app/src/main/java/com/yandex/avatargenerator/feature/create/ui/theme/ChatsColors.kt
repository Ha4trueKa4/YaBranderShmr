package com.yandex.avatargenerator.feature.create.ui.theme

import androidx.compose.ui.graphics.Brush
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Цветовые алиасы экрана чатов — только поверх [AppColors], своей палитры у фичи нет
 * (правило `ARCHITECTURE.md`: никаких `Color(0xFF...)` внутри фичи).
 */
internal object ChatsColors {

    /**
     * Градиенты превью чата: настоящей картинки проекта бэкенд пока не отдаёт,
     * поэтому плитка заливается градиентом, выбранным по id чата
     * (см. [com.yandex.avatargenerator.feature.create.ui.components.ChatThumbnail]).
     *
     * В макете это три произвольных градиента; здесь они пересобраны из токенов
     * палитры, чтобы превью оставались в фирменных цветах и не разъезжались с
     * приложением при правке [AppColors].
     */
    val ThumbnailGradients: List<Brush> = listOf(
        Brush.horizontalGradient(listOf(AppColors.Error, AppColors.Accent)),
        Brush.horizontalGradient(listOf(AppColors.Success, AppColors.AccentCyan)),
        Brush.horizontalGradient(listOf(AppColors.Accent, AppColors.AccentCyan)),
    )
}
