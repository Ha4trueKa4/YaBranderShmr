package com.yandex.avatargenerator.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

private val IconCircleSize = 64.dp
private val IconSize = 28.dp

/**
 * Пустое состояние экрана: опциональная иконка в круге, заголовок, подпись и действие.
 *
 * Кнопка показывается, только когда заданы и [actionLabel], и [onAction] — экран может
 * осознанно не дублировать действие, которое и так на виду (например, плавающую кнопку).
 */
@Composable
fun BrandEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes iconRes: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (iconRes != null) {
            StateIcon(iconRes = iconRes)
            Spacer(modifier = Modifier.height(AppSpacing.l))
        }
        Text(text = title, style = AppTypography.heading3, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = subtitle,
                style = AppTypography.body.copy(color = AppColors.TextSecondary),
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(AppSpacing.l))
            BrandPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Состояние ошибки загрузки: заголовок и повторная попытка второстепенной кнопкой. */
@Composable
fun BrandErrorState(
    title: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = AppTypography.heading3, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(AppSpacing.l))
        BrandSecondaryButton(text = retryLabel, onClick = onRetry)
    }
}

@Composable
private fun StateIcon(@DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(IconCircleSize)
            .brandSurface(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(IconSize)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun BrandStatesPreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            BrandEmptyState(
                title = "No chats yet",
                subtitle = "Start a new project to create your first avatar"
            )
            BrandEmptyState(
                title = "No subscriptions yet",
                subtitle = "Follow creators to see their work here",
                iconRes = R.drawable.ic_user_plus,
                actionLabel = "Explore all projects",
                onAction = {}
            )
            BrandErrorState(title = "Couldn't load feed", retryLabel = "Retry", onRetry = {})
        }
    }
}
