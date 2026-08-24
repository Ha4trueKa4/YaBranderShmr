# Архитектура Android-приложения

Единые правила структуры и стиля кода для всех фич в модуле.

## Модули

Приложение целиком лежит в одном модуле `:app` — по фичам и слоям его не режем.
Отдельно вынесен только фреймворк-код без знания о продукте:

- `:core:mvi` — ядро MVI (`BaseIntent`, `BaseState`, `BaseEffect`, `BaseViewModel`,
  `CollectEffects`). Живёт отдельно, чтобы граница «база MVI ↔ фичи» держалась
  компилятором, а не уговором: из ядра нельзя случайно сходить в `feature/*`.

Пакет везде — `com.yandex.avatargenerator`.

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
- Свои цветовые алиасы — `ui/theme/<Имя>Colors.kt`, но только если там есть
  **собственные** значения поверх `AppColors` (см. `feature/create/ui/theme/ChatsColors.kt`
  с градиентами превью). Файл из одних алиасов на `AppColors` заводить не нужно —
  обращаемся к `AppColors` напрямую.

#### MVI

Новые экраны пишем на MVI: однонаправленный поток `Intent → ViewModel → State`
плюс канал одноразовых `Effect`. Ядро — модуль `:core:mvi` (`BaseIntent`, `BaseState`,
`BaseEffect`, `BaseViewModel`, `CollectEffects`), Compose в `BaseViewModel` не
попадает. Эталон — `feature/auth` (экраны входа и регистрации).

Файлы экрана: `<Имя>Intent.kt`, `<Имя>State.kt`, `<Имя>Effect.kt`,
`<Имя>ViewModel.kt`, `<Имя>Screen.kt`, `<Имя>Content.kt`.

Экранов в фиче может быть несколько — тогда каждый лежит в своём подпакете
`ui/<экран>/`, а в корне `ui/` остаётся только общее для всех экранов фичи
(`ui/components/`, общие ошибки, лимиты, `remember*`-хелперы для других фич):

```
feature/auth/ui/
  login/     — LoginIntent, LoginState, LoginEffect, LoginViewModel, LoginScreen, LoginContent
  register/  — то же для регистрации
  components/, AuthFormError.kt, AuthInputLimit.kt, ... — общее
```

Вход и регистрация — один флоу и одна фича `auth` (общие репозиторий, usecase'ы,
ошибки, компоненты), но разные экраны с разными `State`/`ViewModel`, поэтому
делятся подпакетами, а не отдельными фичами.

Правила:

1. `Intent` — намерение пользователя (не `android.content.Intent`):
   `sealed interface`, ветки `data object`/`data class`, имена по действию —
   `OnRetryClick`, `OnQueryChange(value)`.
2. `State` — один immutable `data class` на экран, все поля `val`, включая
   `isLoading` и ошибки. Нескольких потоков состояния у экрана не бывает.
3. `Effect` — только одноразовое: навигация, снэкбар, шаринг, внешнее
   приложение. **Навигация — эффект**: `Screen` ловит его и дёргает лямбду,
   переданную снаружи (граф по-прежнему в `AppNavHost`).
4. `<Имя>Content` принимает ровно `state` + `onIntent` (+ `modifier`). Никаких
   `onXxxClick`-лямбд и никакого бизнес-состояния в `remember` — только чисто
   визуальное (скролл, анимация). `Content` покрыт `@Preview`.
5. Вся логика — в `handleIntent` через исчерпывающий `when` без `else`: вызов
   usecase'а, `updateState`, `sendEffect`. Бизнес-логики ни в UI, ни в самой
   ViewModel нет.
6. `handleIntent` держит очередь интентов, пока не вернётся. Долгую работу
   (поход в сеть) ветка запускает отдельной корутиной в `viewModelScope` —
   иначе набранный за время запроса текст встанет в очередь и приедет пачкой.

Старые экраны на «VM с набором методов» переезжают по мере правок; смешивать
оба подхода внутри одного экрана нельзя.

### Тесты

Юнит-тесты (`app/src/test/`) повторяют структуру фичи: тест лежит в пакете того
слоя, который проверяет (`feature/auth/data/AuthRepositoryImplTest.kt`,
`feature/auth/ui/login/LoginViewModelTest.kt`). Тест-дубли (`Fake*`) — в отдельном
пакете `feature/<имя>/fake/`, чтобы не путать их с самими тестами.

На 2026-08-23 по этой схеме разложены `feature/auth`, `create`, `createproject`,
`feed`, `profile`, `project` (экран публичного проекта) и `subscription`;
остальные фичи лежат плоско и переезжают по мере правок.

## Дизайн-система

Общие токены и примитивы — в `com.yandex.avatargenerator.ui`:

- `ui/theme/AppColors.kt`, `AppTypography.kt`, `AppFont.kt`, `AppShape.kt`,
  `AppSpacing.kt`
- `ui/components/BrandButtons.kt` (`BrandPrimaryButton` со слотом `leadingIcon`,
  `BrandSecondaryButton`, `BrandButtonDefaults.Height` — на неё завязывают
  отступы соседних элементов), `BrandTextField.kt`, `BrandStates.kt`
  (`BrandEmptyState`, `BrandErrorState`), `BrandTextLink.kt` («текст + акцентное
  кликабельное слово»), `BrandAvatar.kt`, `BrandBadge.kt`, `BrandCard.kt`,
  `BrandChatBubble.kt`, `BrandVersionRow.kt`, `BrandSkeleton.kt`
  (переиспользуемые примитивы shimmer-плейсхолдеров)

**Правило:** в фичах не используем сырые `material3.Button`, захардкоженные
`Color(0xFF...)`, произвольные `.sp`/`.dp` для типографики и отступов — всё
из `ui/theme/*` и `ui/components/*`. Нет нужного токена/компонента —
добавляем в дизайн-систему, а не изобретаем в фиче.

Исключение — точечные значения без токена (например, размер конкретной иконки).

**Тема переключается автоматически** вслед за системной настройкой:
`AvatarGeneratorTheme` раздаёт `LightAppColorScheme`/`DarkAppColorScheme` через
CompositionLocal. Поэтому `AppColors.*` и `AppTypography.*` — `@Composable`-свойства
и читаются только внутри `@Composable`-функций. Нужен цвет в обычном коде
(`remember { }`, `Defaults`-объект) — объяви `@Composable`-свойство там же
(см. `OnboardingDefaults`) или передай параметром. Новый токен заводим сразу
в **обе** схемы.

**Не заводим в фиче свой shimmer, свою кнопку, своё empty/error-состояние.**
Если общему компоненту не хватает параметра — добавляем параметр ему
(так появился `leadingIcon` у `BrandPrimaryButton`), а не пишем рядом «почти
такой же» приватный вариант: копии начинают расходиться по стилю.

**Явный `Text(style = ...)` берём только из `AppTypography`.** Произвольный
`TextStyle` **заменяет** унаследованный от `MaterialTheme` стиль, а не дополняет
его, и вместе с ним теряется `InterFontFamily` — экран молча уезжает на системный
шрифт. Нужна вариация — `AppTypography.body.copy(fontWeight = ...)`.

**Тексты — только в `strings.xml`**, без строковых литералов в Kotlin
(исключение — `@Preview`, туда данные можно писать прямо в коде).

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

## Сеть

Контракт API, реализованные ручки и известные расхождения — в `API.md`.

Основа — в `core/network/`, один `HttpClient` (Ktor + OkHttp) на приложение,
регистрируется в `core/di/NetworkModule.kt`:

- `ApiConfig.kt` — базовый URL (из `BuildConfig.API_BASE_URL`) и таймауты.
- `HttpClientFactory.kt` — сборка клиента: `ContentNegotiation` + `ApiJson`,
  таймауты, ретраи на 5xx и обрывы связи, `Bearer`-заголовок, логирование
  только в debug.
- `ApiJson.kt` — общая настройка `Json` (`ignoreUnknownKeys` обязателен).
- `AuthTokenProvider.kt` / `AuthTokenStore.kt` — чтение и запись токена;
  клиент спрашивает его на каждом запросе, поэтому смена токена не требует
  пересоздания клиента.
- `ApiCall.kt` — `apiCall { }`, маппинг исключений Ktor в `NetworkException`.
- `dto/` — общие DTO (`ApiErrorDto`).

**Правила:**

- Репозиторий фичи инъектит готовый `HttpClient`, свой не собирает.
- Каждый вызов API заворачивается в `apiCall { }`, чтобы наружу летела только
  доменная `core/domain/NetworkException` — `domain/` и `ui/` не знают ни про
  Ktor, ни про HTTP-коды.
- DTO — в `feature/<имя>/data/dto/`, с суффиксом `Dto` и `@Serializable`.
  В `domain/` они не попадают: маппинг `Dto → доменная модель` живёт в `data/`.

## Именование

- Файл = один публичный класс/composable того же имени.
- Экраны: `<Имя>Screen.kt`. Usecase: `<Действие>UseCase.kt`.
- Приватные вспомогательные composable — с префиксом фичи/экрана
  (`FeedTabItem`), чтобы не путать с публичным API файла в диффе.
  Как только такой помощник понадобился второму экрану фичи — выносим его
  в `ui/components/` отдельным файлом с префиксом фичи (`AuthBrandMark.kt`),
  а не копируем под новым именем.
