package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

private val PillMinHeight = 40.dp
private val SendButtonSize = 40.dp
private val SendIconSize = 20.dp
private val SendProgressStroke = 2.dp

/** Прозрачность неактивной кнопки: черновик пуст либо генерация ещё идёт. */
private const val DisabledAlpha = 0.4f

/**
 * Поле ввода сообщения внизу экрана проекта: «таблетка» с текстом и круглая
 * кнопка отправки.
 *
 * `BrandTextField` здесь не подходит — у него метка сверху и прямоугольная
 * форма; это другой компонент, а не вариация существующего.
 *
 * Низ безрамочный: фон [AppColors.Surface] уходит под системную навигацию, как
 * у [com.yandex.avatargenerator.navigation.AppBottomBar]. Поэтому инсет добавлен
 * *внутри* — отступ поверх `background`, а не в переданном `modifier`, иначе
 * заливка до края экрана не дошла бы.
 *
 * @param isSending идёт генерация по прошлому промпту — отправка заблокирована,
 *  на кнопке вместо стрелки крутится индикатор. Печатать при этом можно.
 */
@Composable
internal fun ChatInputBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSending: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface),
    ) {
        HorizontalDivider(thickness = 1.dp, color = AppColors.Border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.m),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            ChatInputDraftField(
                draft = draft,
                onDraftChange = onDraftChange,
                modifier = Modifier.weight(1f),
            )
            ChatInputSendButton(
                enabled = draft.isNotBlank() && !isSending,
                isSending = isSending,
                onClick = onSendClick,
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.safeDrawing),
        )
    }
}

@Composable
private fun ChatInputDraftField(
    draft: String,
    onDraftChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = PillMinHeight)
            .clip(RoundedCornerShape(AppShape.full))
            .background(AppColors.SurfaceVariant)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.xs),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (draft.isEmpty()) {
            Text(
                text = stringResource(R.string.project_input_placeholder),
                style = AppTypography.body.copy(color = AppColors.TextSecondary),
            )
        }
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            textStyle = AppTypography.body,
            cursorBrush = SolidColor(AppColors.Accent),
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChatInputSendButton(
    enabled: Boolean,
    isSending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (enabled) 1f else DisabledAlpha
    Box(
        modifier = modifier
            .size(SendButtonSize)
            .clip(CircleShape)
            .background(AppColors.Accent.copy(alpha = contentAlpha))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSending) {
            CircularProgressIndicator(
                color = AppColors.OnAccent,
                strokeWidth = SendProgressStroke,
                modifier = Modifier.size(SendIconSize),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = stringResource(R.string.project_send_description),
                tint = AppColors.OnAccent.copy(alpha = contentAlpha),
                modifier = Modifier.size(SendIconSize),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatInputBarEmptyPreview() {
    AvatarGeneratorTheme {
        ChatInputBar(draft = "", onDraftChange = {}, onSendClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatInputBarFilledPreview() {
    AvatarGeneratorTheme {
        ChatInputBar(
            draft = "Make the mark rounder and try a cyan accent",
            onDraftChange = {},
            onSendClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatInputBarSendingPreview() {
    AvatarGeneratorTheme {
        ChatInputBar(
            draft = "",
            onDraftChange = {},
            onSendClick = {},
            isSending = true,
        )
    }
}
