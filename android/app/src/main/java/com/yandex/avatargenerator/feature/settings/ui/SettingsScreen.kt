package com.yandex.avatargenerator.feature.settings.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.settings.domain.model.AppLanguage
import com.yandex.avatargenerator.feature.settings.domain.model.ThemeMode
import com.yandex.avatargenerator.feature.settings.ui.components.SettingsCard
import com.yandex.avatargenerator.feature.settings.ui.components.SettingsRow
import com.yandex.avatargenerator.feature.settings.ui.components.SettingsSectionHeader
import com.yandex.avatargenerator.feature.settings.ui.components.SettingsThemeSelector
import com.yandex.avatargenerator.ui.components.BrandBottomSheet
import com.yandex.avatargenerator.ui.components.BrandDestructiveButton
import com.yandex.avatargenerator.ui.components.BrandSelectableOption
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран настроек: аккаунт (никнейм/email), оформление (тема + язык), о приложении
 * (условия, политика, версия) и разлогин. Открывается из шапки своего профиля.
 *
 * Навигация отдана наружу коллбэками; ViewModel — из Koin. Смена темы/языка
 * применяется вживую на корне Compose (`MainActivity`), без пересоздания Activity.
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
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    SettingsEvent.LoggedOut -> onLoggedOut()
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

@Composable
private fun SettingsLanguageSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    BrandBottomSheet(
        title = stringResource(R.string.settings_language),
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            AppLanguage.entries.forEach { language ->
                BrandSelectableOption(
                    title = stringResource(language.labelRes()),
                    selected = language == selected,
                    onClick = { onSelect(language) },
                )
            }
        }
    }
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.RUSSIAN -> R.string.settings_language_ru
    AppLanguage.ENGLISH -> R.string.settings_language_en
}
