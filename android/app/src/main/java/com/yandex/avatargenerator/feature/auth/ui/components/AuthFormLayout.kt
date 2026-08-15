package com.yandex.avatargenerator.feature.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing

/**
 * Общий каркас форм входа и регистрации: фон, центрирование по макету и
 * прокрутка при открытой клавиатуре.
 *
 * Без прокрутки на регистрации кнопка «создать аккаунт» уходит за клавиатуру:
 * Scaffold не отдаёт ime-инсет, а enableEdgeToEdge отключает ресайз окна.
 * heightIn(min = высота экрана) обязателен — иначе Arrangement.Center внутри
 * verticalScroll не работает и контент прижимается к верху.
 */
@Composable
internal fun AuthFormLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Порядок модификаторов принципиален:
                // imePadding() до verticalScroll — иначе отступ уедет внутрь
                // прокручиваемой области и вьюпорт не уменьшится;
                // heightIn(min = ...) после verticalScroll — иначе ограничение
                // применится к вьюпорту, а не к содержимому.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .heightIn(min = screenHeight)
                .padding(horizontal = AppSpacing.l),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
