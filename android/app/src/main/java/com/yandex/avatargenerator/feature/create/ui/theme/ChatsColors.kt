package com.yandex.avatargenerator.feature.create.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.yandex.avatargenerator.ui.theme.AppColorScheme
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.LocalAppColorScheme

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
     * В макете это три произвольных градиента; здесь они заданы не цветами, а
     * выборкой токенов из палитры — так превью остаются в фирменных цветах и
     * следуют за темой, а сам список живёт вне композиции (размер берётся из него,
     * а не из отдельной константы).
     */
    private val ThumbnailGradientStops: List<(AppColorScheme) -> List<Color>> = listOf(
        { listOf(it.accent, it.accentSoft) },
        { listOf(it.accentSoft, it.accentWarm) },
        { listOf(it.accentWarm, it.accent) },
    )

    /**
     * Градиент превью для [chatId]: индекс детерминирован по id, поэтому у одного
     * и того же чата плитка всегда одного цвета. `Int.mod` (а не `%`) даёт
     * неотрицательный остаток при отрицательном хеше.
     *
     * `remember` по (chatId, палитра) — иначе [Brush] пересобирался бы на каждой
     * рекомпозиции в прокручиваемом списке чатов.
     */
    @Composable
    fun rememberThumbnailGradient(chatId: String): Brush {
        val colors = LocalAppColorScheme.current
        return remember(chatId, colors) {
            val stops = ThumbnailGradientStops[chatId.hashCode().mod(ThumbnailGradientStops.size)]
            Brush.horizontalGradient(stops(colors))
        }
    }
}
