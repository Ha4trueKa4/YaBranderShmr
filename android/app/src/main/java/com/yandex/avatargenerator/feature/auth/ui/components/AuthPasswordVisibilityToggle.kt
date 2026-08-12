package com.yandex.avatargenerator.feature.auth.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Переключатель видимости пароля — иконка "eye" из макета.
 * Общий для экранов входа и регистрации.
 */
@Composable
internal fun AuthPasswordVisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onToggle, modifier = modifier.size(20.dp)) {
        Icon(
            imageVector = if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            contentDescription = null,
            tint = AppColors.TextMuted
        )
    }
}
