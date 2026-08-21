package com.yandex.avatargenerator.feature.settings.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalContext
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.settings.domain.model.AppLanguage
import com.yandex.avatargenerator.feature.settings.domain.model.ThemeMode
import com.yandex.avatargenerator.feature.settings.ui.components.SettingsCard
import com.yandex.avatargenerator.feature.settings.ui.components.SettingsRow
import com.yandex.avatargenerator.feature.settings.ui.components.SettingsSectionHeader
import com.yandex.avatargenerator.feature.settings.ui.components.SettingsThemeSelector
import com.yandex.avatargenerator.ui.components.BrandDestructiveButton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран настроек: аккаунт (никнейм/email), оформление (тема + язык), о приложении
 * (условия, политика, версия) и разлогин. Открывается из шапки своего профиля.
 *
 * Навигация отдана наружу коллбэками; ViewModel — из Koin. Смена языка требует
 * пересоздания Activity (перечитать ресурсы в новой локали) — делаем это по
 * событию [SettingsEvent.LanguageChanged].
 *
 * @param onBack закрыть экран (стрелка «назад»).
 * @param onLoggedOut разлогин завершён — увести на экран входа.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    SettingsEvent.LoggedOut -> onLoggedOut()
                    SettingsEvent.LanguageChanged -> context.findActivity()?.recreate()
                }
            }
        }
    }

    SettingsContent(
        state = state,
        onBack = onBack,
        onThemeSelect = viewModel::setThemeMode,
        onLanguageSelect = viewModel::setLanguage,
        onLogout = viewModel::logout,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onThemeSelect: (ThemeMode) -> Unit,
    onLanguageSelect: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var languageSheetVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        SettingsTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.m)
                .padding(bottom = AppSpacing.l),
        ) {
            SettingsSectionHeader(stringResource(R.string.settings_section_account))
            SettingsCard {
                SettingsRow(
                    iconRes = R.drawable.ic_user,
                    label = stringResource(R.string.settings_account_username),
                    valueText = state.nickname
                        .takeIf { it.isNotEmpty() }
                        ?.let { stringResource(R.string.profile_handle, it) },
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_mail,
                    label = stringResource(R.string.settings_account_email),
                    valueText = state.email.takeIf { it.isNotEmpty() },
                )
            }

            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
            SettingsCard {
                SettingsThemeSelector(
                    label = stringResource(R.string.settings_theme),
                    options = listOf(
                        stringResource(R.string.settings_theme_system),
                        stringResource(R.string.settings_theme_light),
                        stringResource(R.string.settings_theme_dark),
                    ),
                    selectedIndex = state.themeMode.ordinal,
                    onSelect = { index -> onThemeSelect(ThemeMode.entries[index]) },
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_globe,
                    label = stringResource(R.string.settings_language),
                    valueText = stringResource(state.language.labelRes()),
                    showChevron = true,
                    onClick = { languageSheetVisible = true },
                )
            }

            SettingsSectionHeader(stringResource(R.string.settings_section_about))
            SettingsCard {
                // Информационные строки: showChevron/onClick вернём, когда появятся
                // экраны или ссылки условий использования и политики конфиденциальности.
                SettingsRow(
                    iconRes = R.drawable.ic_file_text,
                    label = stringResource(R.string.settings_about_terms),
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_shield,
                    label = stringResource(R.string.settings_about_privacy),
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_info,
                    label = stringResource(R.string.settings_about_version),
                    valueText = state.appVersion.takeIf { it.isNotEmpty() },
                )
            }

            BrandDestructiveButton(
                text = stringResource(R.string.settings_logout),
                onClick = onLogout,
                fullWidth = true,
                modifier = Modifier.padding(top = AppSpacing.l),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_log_out),
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }

    if (languageSheetVisible) {
        SettingsLanguageSheet(
            selected = state.language,
            onSelect = { language ->
                languageSheetVisible = false
                onLanguageSelect(language)
            },
            onDismiss = { languageSheetVisible = false },
        )
    }
}

/** Шапка экрана: кнопка «назад» и заголовок с нижней границей. */
@Composable
private fun SettingsTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.settings_back_description),
                    tint = AppColors.TextPrimary,
                )
            }
            Text(text = stringResource(R.string.settings_title), style = AppTypography.heading3)
        }
        HorizontalDivider(thickness = 1.dp, color = AppColors.Border)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(thickness = 1.dp, color = AppColors.Border)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsLanguageSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppSpacing.l)) {
            Text(
                text = stringResource(R.string.settings_language),
                style = AppTypography.heading3,
                modifier = Modifier.padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
            )
            AppLanguage.entries.forEach { language ->
                LanguageOptionRow(
                    label = stringResource(language.labelRes()),
                    selected = language == selected,
                    onClick = { onSelect(language) },
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTypography.body.copy(
                color = if (selected) AppColors.Accent else AppColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.RUSSIAN -> R.string.settings_language_ru
    AppLanguage.ENGLISH -> R.string.settings_language_en
}

/** Разворачивает цепочку [ContextWrapper] до [Activity] — нужно для `recreate()`. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
