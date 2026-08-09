# Архитектура Android-приложения

Единые правила структуры и стиля кода для всех фич в модуле.

## Модуль

Один Gradle-модуль `:app`, без мультимодульности. Пакет — `com.yandex.avatargenerator`.

## Структура фичи

```
feature/<имя>/
  data/     — реализации репозиториев, источники данных
  domain/   — repo API (интерфейсы), usecase'ы, доменные модели
  ui/       — Composable-экраны, ViewModel, ViewModelFactory
```

Пример: `feature/feed/`, `feature/onboarding/`, `feature/auth/`.

Плоская структура внутри слоя не обязательна: можно выносить usecase'ы,
модели, утилиты в подпапки (`domain/usecases/`, `domain/model/`, `domain/utils/`).

Экстеншены — по одному файлу на класс-получатель: все экстеншены `Int` в
`Int.kt`, все `String` — в `String.kt`, и т.д.

### data/

- Реализации репозиториев (`<Имя>RepositoryImpl`), источники данных.
  Интерфейс сюда не кладём — только implementation.
- Мок-данные — отдельный класс рядом с реальной реализацией
  (`OnboardingRepositoryImpl` + `MockOnboardingRepository`), не смешивать
  с боевой логикой.

### domain/

- Repo API — интерфейс репозитория (`<Имя>Repository`) в `domain/`,
  реализация в `data/` (пример: `onboarding/domain/OnboardingRepository.kt`
  + `onboarding/data/OnboardingPreferencesRepositoryImpl.kt`).
- Usecase'ы — по одному классу на файл, суффикс `UseCase`
  (`CompleteOnboardingUseCase.kt`).
- Доменные модели — в `domain/model/` (подпапка обязательна, если моделей
  больше одной).

**Направление зависимостей:** `domain/` не импортирует ничего из `data/`
и `ui/` (никакого Android/Compose/Ktor/DataStore) — только Kotlin/coroutines.
Без модульности компилятор это не проверит, держится на код-ревью.
`ui/` → `domain/` ← `data/`.

### ui/

- Экран — `<Имя>Screen.kt`, состояние — `<Имя>ViewModel.kt`
  (регистрируется в Koin-модуле фичи).
- Переиспользуемые куски экрана — `ui/components/`.
- Цвета берём из общей палитры `ui/theme/AppColors.kt` напрямую. Если фиче
  нужны свои цветовые алиасы — только файлом `ui/theme/<Имя>Colors.kt` как
  алиасы на `AppColors`, не отдельная палитра.

## Дизайн-система

Общие токены и примитивы — в `com.yandex.avatargenerator.ui`:

- `ui/theme/AppColors.kt`, `AppTypography.kt`, `AppFont.kt`, `AppShape.kt`,
  `AppSpacing.kt`
- `ui/components/BrandButtons.kt`, `BrandTextField.kt`, `BrandAvatar.kt`,
  `BrandBadge.kt`, `BrandCard.kt`, `BrandChatBubble.kt`, `BrandVersionRow.kt`,
  `BrandSkeleton.kt` (переиспользуемые примитивы shimmer-плейсхолдеров)

**Правило:** в фичах не используем сырые `material3.Button`, захардкоженные
`Color(0xFF...)`, произвольные `.sp`/`.dp` для типографики и отступов — всё
из `ui/theme/*` и `ui/components/*`. Нет нужного токена/компонента —
добавляем в дизайн-систему, а не изобретаем в фиче.

Исключение — точечные значения без токена (например, размер конкретной иконки).

## Навигация

- Маршруты — в `navigation/Destination.kt` (`sealed class Destination`),
  роуты строковые (без type-safe nav — сознательно, ради простоты).
- Граф — `navigation/AppNavHost.kt`. Неготовый экран — `PlaceholderScreen(title = "...")`,
  не пропускаем из графа.
- Готовый экран сразу подключаем в `AppNavHost` вместо плейсхолдера.

## DI

Используем Koin: ViewModel и репозитории регистрируются в Koin-модулях,
без ручных фабрик/провайдеров-синглтонов. Сеть — через Ktor. Общее для
всех фич (например, `DataStore<Preferences>`) — в `core/`, не дублируется
(см. `core/data/AppPreferences.kt`).

## Именование

- Файл = один публичный класс/composable того же имени.
- Экраны: `<Имя>Screen.kt`. Usecase: `<Действие>UseCase.kt`.
- Приватные вспомогательные composable — с префиксом фичи/экрана
  (`LoginBrandMark`), чтобы не путать с публичным API файла в диффе.
