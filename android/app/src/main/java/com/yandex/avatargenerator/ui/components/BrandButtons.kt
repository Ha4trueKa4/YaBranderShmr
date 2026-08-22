package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button as Material3Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Общие размеры кнопок дизайн-системы — на них завязывают отступы соседних элементов. */
object BrandButtonDefaults {
    val Height = 48.dp

    /** Компактная высота — для кнопок внутри строк списка (Follow/Unfollow и т.п.). */
    val CompactHeight = 36.dp
}

/**
 * Основная кнопка призыва к действию (см. "01 · Buttons" в Component Sandbox).
 * @param fullWidth растянуть на всю доступную ширину — вариант "Get Started (Full-Width)".
 * @param compact уменьшенная высота/паддинги — для кнопки внутри строки списка.
 * @param leadingIcon значок перед текстом; цвет наследуется от `contentColor` кнопки.
 */
@Composable
fun BrandPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Material3Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(if (compact) BrandButtonDefaults.CompactHeight else BrandButtonDefaults.Height),
        shape = RoundedCornerShape(if (compact) AppShape.s else AppShape.l),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Accent,
            contentColor = AppColors.OnAccent
        ),
        contentPadding = PaddingValues(horizontal = if (compact) AppSpacing.m else AppSpacing.l)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }
        Text(
            text = text,
            color = AppColors.OnAccent,
            fontSize = if (compact) 14.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Нейтральная (tonal) кнопка: заливка [AppColors.Surface] с бордером
 * [AppColors.Border] и приглушённым текстом — для второстепенных действий
 * равного веса рядом с [BrandPrimaryButton] (например, пара «Download PNG /
 * Download SVG»), где акцентный outline [BrandSecondaryButton] перетягивал бы
 * внимание.
 *
 * @param leadingIcon значок перед текстом; цвет наследуется от `contentColor`.
 */
@Composable
fun BrandTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Material3Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(BrandButtonDefaults.Height),
        shape = RoundedCornerShape(AppShape.l),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Surface,
            contentColor = AppColors.TextSecondary
        ),
        border = BorderStroke(1.dp, AppColors.Border),
        contentPadding = PaddingValues(horizontal = AppSpacing.l)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }
        Text(text = text, color = AppColors.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Второстепенная (outline) кнопка — "Secondary Action" в макете.
 * @param compact уменьшенная высота/паддинги — для кнопки внутри строки списка.
 */
@Composable
fun BrandSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(if (compact) BrandButtonDefaults.CompactHeight else BrandButtonDefaults.Height),
        shape = RoundedCornerShape(if (compact) AppShape.s else AppShape.l),
        border = BorderStroke(1.5.dp, AppColors.Accent),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppColors.Accent
        ),
        contentPadding = PaddingValues(horizontal = if (compact) AppSpacing.m else AppSpacing.l)
    ) {
        Text(
            text = text,
            color = AppColors.Accent,
            fontSize = if (compact) 14.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Деструктивная (outline) кнопка — «Выйти» и подобные опасные действия: красный
 * бордер и текст [AppColors.Error] с едва заметной красной заливкой.
 *
 * @param leadingIcon значок перед текстом; цвет наследуется от `contentColor`.
 */
@Composable
fun BrandDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(BrandButtonDefaults.Height),
        shape = RoundedCornerShape(AppShape.l),
        border = BorderStroke(1.5.dp, AppColors.Error),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.Error.copy(alpha = 0.08f),
            contentColor = AppColors.Error
        ),
        contentPadding = PaddingValues(horizontal = AppSpacing.l)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }
        Text(text = text, color = AppColors.Error, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun BrandButtonsPreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandPrimaryButton(text = "Get Started", onClick = {})
            BrandPrimaryButton(text = "Get Started (Full-Width)", onClick = {}, fullWidth = true)
            BrandPrimaryButton(
                text = "New project",
                onClick = {},
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            BrandSecondaryButton(text = "Secondary Action", onClick = {})
            BrandTonalButton(
                text = "Download SVG",
                onClick = {},
                fullWidth = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_publish),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            BrandPrimaryButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
