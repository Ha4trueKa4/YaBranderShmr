package com.yandex.avatargenerator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Палитра одной темы; в коде читается через [AppColors]. */
@Immutable
internal data class AppColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentText: Color,
    val accentSoft: Color,
    val accentWarm: Color,
    val onAccent: Color,
    val onAccentSoft: Color,
    val onAccentWarm: Color,
    val success: Color,
    val error: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    /** Тёмная ли это тема — для редких мест, где цвет зависит не от токена, а от самой темы. */
    val isDark: Boolean,
)

/**
 * Фирменная палитра: коралл `#F8604A` — основной акцент, мягкий коралл `#FE9693` —
 * его вторая ступень, жёлтый `#FFD64F` — тёплый акцент (награды, «pro», подсветка).
 * Гамма целиком тёплая, фиолетового в теме нет. Нейтрали построены на
 * чернильном `#222A3A`.
 *
 * ВАЖНО: при изменении `background` синхронно правь `@color/window_background`
 * в `values-night/colors.xml` — это фон окна до отрисовки Compose.
 */
internal val DarkAppColorScheme = AppColorScheme(
    background = Color(0xFF222A3A),
    surface = Color(0xFF2B3446),
    surfaceVariant = Color(0xFF354055),
    border = Color(0xFF3E4A61),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFCFCDDD),
    textMuted = Color(0xFF8B93A5),
    accent = Color(0xFFF8604A),
    // Фирменный коралл к фону проходит (4.6:1), но на карточках и плашках уже нет
    // — берём его же на ступень светлее, чтобы AA держался на всех трёх подложках.
    accentText = Color(0xFFFF8A75),
    accentSoft = Color(0xFFFE9693),
    accentWarm = Color(0xFFFFD64F),
    onAccent = Color(0xFFFFFFFF),
    onAccentSoft = Color(0xFF222A3A),
    onAccentWarm = Color(0xFF222A3A),
    success = Color(0xFF3ECF8E),
    // Уведён в чистый красный: коралловый акцент занял всю тёплую гамму, и
    // ошибка совпадающего оттенка перестала читаться как ошибка.
    error = Color(0xFFFF6369),
    shimmerBase = Color(0xFF2B3446),
    shimmerHighlight = Color(0xFF3E4A61),
    isDark = true,
)

/**
 * Та же шкала, развёрнутая: нейтрали намеренно обесцвечены — крупные плоскости
 * остаются серыми, цвет живёт только в акцентах. Мягкий акцент и семантика взяты
 * на ступень темнее, иначе не держат контраст на светлом фоне.
 *
 * Коралл здесь раздвоен. `accent` — фирменный `#F8604A`, но только заливкой под
 * белым текстом: по светлому фону он даёт 2.8:1 и как текст нечитаем. Текст,
 * который должен быть акцентным, берёт `accentText` (4.7:1).
 *
 * Все текстовые токены проходят WCAG AA (4.5:1) к трём подложкам сразу —
 * `background`, `surface` и `surfaceVariant`; минимум — accentText на
 * surfaceVariant, 4.55:1.
 *
 * ВАЖНО: при изменении `background` синхронно правь `@color/window_background`
 * в `values/colors.xml`.
 */
internal val LightAppColorScheme = AppColorScheme(
    background = Color(0xFFF4F4F6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEBEBEF),
    border = Color(0xFFCFCDDD),
    textPrimary = Color(0xFF222A3A),
    textSecondary = Color(0xFF575E6E),
    textMuted = Color(0xFF626976),
    accent = Color(0xFFF8604A),
    accentText = Color(0xFFC2381F),
    accentSoft = Color(0xFFFB7C69),
    accentWarm = Color(0xFFFFC812),
    onAccent = Color(0xFFFFFFFF),
    onAccentSoft = Color(0xFF222A3A),
    onAccentWarm = Color(0xFF222A3A),
    success = Color(0xFF12946A),
    error = Color(0xFFD03340),
    shimmerBase = Color(0xFFEBEBEF),
    shimmerHighlight = Color(0xFFF6F6F8),
    isDark = false,
)

internal val LocalAppColorScheme = staticCompositionLocalOf { DarkAppColorScheme }

/**
 * Канонические цветовые токены приложения — единый источник палитры для всех фич.
 *
 * Значения зависят от активной темы, поэтому читаются только внутри
 * `@Composable`-функций. Нужен цвет в обычном коде (`remember { }`, статический
 * `object`) — вычисли его в composable и передай параметром.
 */
internal object AppColors {
    val Background: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.background
    val Surface: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surface
    val SurfaceVariant: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceVariant
    val Border: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.border

    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.textPrimary
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.textSecondary

    /** Приглушённый текст / неактивные элементы (например, вкладки нав-панели). */
    val TextMuted: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.textMuted

    /** Основной фирменный акцент — коралл. */
    val Accent: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.accent

    /**
     * Акцент для текста и иконок поверх фона. Отдельный токен, потому что
     * заливочный [Accent] на светлой теме не добирает контраста для чтения.
     */
    val AccentText: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.accentText

    /** Мягкая ступень акцента; вторая точка градиентов и парных состояний. */
    val AccentSoft: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.accentSoft

    /** Тёплый акцент — жёлтый; выделения, награды, «pro»-метки. */
    val AccentWarm: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.accentWarm

    /** Цвет содержимого поверх заливки Accent (кнопки, беджи). */
    val OnAccent: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onAccent

    /** Цвет содержимого поверх заливки AccentSoft. */
    val OnAccentSoft: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onAccentSoft

    /** Цвет содержимого поверх заливки AccentWarm. */
    val OnAccentWarm: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onAccentWarm

    val Success: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.success
    val Error: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.error

    /**
     * Диагональный градиент Accent → AccentSoft — аватары, превью-плашки
     * проектов. Обе точки лежат на соседних ступенях коралла: переход читается как
     * лёгкий объём, а не как двухцветная заливка.
     */
    val AccentGradient: Brush
        @Composable @ReadOnlyComposable get() = Brush.linearGradient(listOf(Accent, AccentSoft))

    /** Активна ли тёмная тема (учитывает ручное переключение). */
    val IsDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.isDark

    // Плейсхолдеры скелетона загрузки (shimmer).
    val ShimmerBase: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.shimmerBase
    val ShimmerHighlight: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.shimmerHighlight
}
