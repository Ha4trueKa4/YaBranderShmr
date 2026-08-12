package com.yandex.avatargenerator.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Временная заглушка для экранов, которые реализуют другие участники команды
 * (создание проекта, профиль, экран проекта). Нужна, чтобы навигационный
 * скелет собирался и был кликабельным до появления реальных экранов.
 *
 * TODO: заменить на реальный экран по мере готовности соответствующей фичи.
 *
 * @param title заголовок экрана-заглушки.
 * @param subtitle необязательное пояснение (например, переданный аргумент).
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = AppColors.TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
