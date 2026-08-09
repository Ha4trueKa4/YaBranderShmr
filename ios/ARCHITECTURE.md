# Архитектура iOS-приложения

Единые правила структуры и стиля кода для всех фич в проекте.

## Проект

Один Xcode-проект `BrandkitAI.xcodeproj` с единственным таргетом `BrandkitAI`,
без модулей и SPM-пакетов. Bundle ID — `com.yandexSHMR2026IOSDev.BrandkitAI`,
deployment target — iOS 18.0, UI — только SwiftUI, без UIKit-экранов.

Группы в проекте синхронизированы с файловой системой
(`fileSystemSynchronizedGroups`): новый файл достаточно положить в нужную
папку — в `project.pbxproj` его добавлять не нужно и **нельзя** (это создаёт
конфликты в мердж-реквестах).

## Структура

```
BrandkitAI/
  App/          — точка входа и корневой вью
  Navigation/   — маршруты, роутер, разделы таб-бара
  Core/         — общее для всех фич: сессия, расширения
  Features/     — фичи, по папке на каждую
  Assets.xcassets
```

## Структура фичи

```
Features/<Имя>/
  View/
    <Имя>View.swift        — экран
    Components/            — куски экрана, переиспользуемые внутри фичи
  ViewModel/
    <Имя>ViewModel.swift   — состояние экрана и его действия
  <Имя>ListItem.swift      — модели фичи (в корне папки фичи)
```

Пример: `Features/Auth/`, `Features/Chat/`, `Features/Main/`.

Мелкая фича из одного экрана-заглушки может лежать одним файлом
(`Features/Project/Editor/ProjectEditorView.swift`) — подпапки заводятся, когда
появляется вью-модель или второй компонент.

### View

- Вью хранит только то, что нужно SwiftUI для отрисовки. Ввод, валидация,
  форматирование и обращения к сервисам живут во вью-модели.
- Вью-модель — `@State private var viewModel`, для биндингов в начале `body`:
  `@Bindable var viewModel = viewModel`.
- Зависимости приходят через `init` (`SignInView(session:)`), а не создаются
  внутри вью.
- Экран не знает о навигации: он объявляет колбэки (`onGenerate`, `onOpenChat`)
  с пустыми значениями по умолчанию, а подключает их роутер.
- У каждого вью и компонента — `#Preview`. Превью компонента показывает
  состояния (`isEnabled: true/false`, выбран/не выбран).

### ViewModel

- `@MainActor @Observable final class <Имя>ViewModel` — без `ObservableObject`
  и `@Published`.
- Свойства формы — обычные `var`; вычисляемые правила (`canSubmit`) — рядом,
  чтобы вью не считал их сам.
- Данные, которые вью только читает, — `private(set)`.
- Загрузка вызывается из `.task { viewModel.load() }`. `load()` должен быть
  идемпотентным (`guard messages.isEmpty else { return }`), иначе при каждом
  возврате на экран будет перезапрос.
- Экран, который открывает следующий, собирает его модель у себя
  (`makeSignUpViewModel()`), а не тянет зависимости через вью.

### Модели и моки

- Модель фичи — `struct`, `Identifiable`, без импорта SwiftUI: цвета, иконки и
  готовые строки вида `"Sarah Chen · 1 hr ago"` — это представление, а не данные.
  В модели — `author: String` и `date: Date`, склейка и формат — во вью-модели.
- Заглушки — `static let mocks` в `extension` рядом с моделью, одна точка
  правды. Место будущего запроса помечается `// TODO:`.

## Навигация

- Маршруты — `Navigation/AppRoute.swift` (`enum AppRoute: Hashable`).
  Ассоциированные значения кладём сразу (`projectEditor(projectID: UUID)`),
  даже если экран их пока не использует — тогда подключение данных не потребует
  менять enum.
- Разделы — `Navigation/MainTab.swift`: `title` и `icon` живут в самом enum.
- Состояние навигации — `Navigation/AppRouter.swift`: выбранный раздел и
  **отдельный стек на каждый раздел**, чтобы разделы помнили свою глубину.
  Роутер кладётся в `.environment(router)` и достаётся
  `@Environment(AppRouter.self)`.
- Сборка «маршрут → экран» — единственное место, `Features/Main/AppRouteView.swift`.
  Там же подставляются колбэки экранов. Ни один экран не создаёт
  `NavigationLink` на другой экран сам.
- Таб-бар навешен на корень каждого стека через `safeAreaInset`, поэтому
  пушнутый экран перекрывает его физически — без флагов видимости.

Локальные переходы внутри фичи (лист → форма) допустимо делать через
`navigationDestination(isPresented:)` с флагом во вью-модели, но переход между
фичами всегда идёт через роутер.

## Сессия

- `Core/Session/SessionStore.swift` — единственный источник правды об
  авторизации: `state` (`unknown` / `signedOut` / `signedIn`) и методы
  `restore/signIn/register/signOut`. Экраны состояние не проверяют, они зовут
  методы; на изменение реагирует `RootView`.
- Токен — только в Keychain (`Core/Session/TokenStore.swift`), не в
  `UserDefaults`. «Залогинен» = «токен есть», отдельного флага нет.
- `SessionStore` раздаётся через `.environment(session)`.

## Дизайн-система

- Цвета — только из `Assets.xcassets` через сгенерированные символы:
  `Color(.background)`, `Color(.tabBarInactive)`, `Color(.startGradient)`.
  Литералы `Color(red:green:blue:)` в коде фич запрещены — нет нужного цвета,
  заводим colorset.
- Акцент — `AccentColor` из ассетов, берётся как `.tint` / `Color.accentColor`.
  Не прописываем `.tint(.purple)` по экранам.
- Системные цвета для подложек и разделителей — семантические
  (`Color(.secondarySystemBackground)`, `Color(.separator)`).
- Шрифты — семантические (`.headline`, `.subheadline`, `.footnote`), чтобы
  работал Dynamic Type. `.font(.system(size:))` — только там, где размер
  действительно фиксирован (иконка), и это исключение.
- Фон экрана — `.brandBackground()` (`Core/Extensions/BrandBackground.swift`).
- Тёмная тема задаётся один раз в `RootView` (`.preferredColorScheme(.dark)`),
  в экранах не дублируется. В `#Preview` — можно и нужно.
- Общие примитивы (`PrimaryButton`, `AuthField`, `AuthScreenLayout`) лежат в
  `Components` своей фичи; как только компонент понадобился второй фиче — он
  переезжает в общую папку компонентов, а не копируется.

## Совместимость

Deployment target — iOS 18, поэтому API iOS 26 (`glassEffect`,
`sharedBackgroundVisibility`, `.glassProminent`) закрываются
`if #available(iOS 26.0, *)` с рабочим фолбэком. Повторяющиеся ветвления
выносятся в расширение — образец `Core/Extensions/ProminentStyle.swift`.

## Доступность

- Кликабельный элемент — `Button`, а не `.onTapGesture`: иначе нет ни состояния
  нажатия, ни трейта `.isButton` для VoiceOver.
- Кнопке-иконке обязателен `.accessibilityLabel`.
- Составная строка объединяется в один элемент
  (`.accessibilityElement(children: .combine)`), декоративная графика
  скрывается (`.accessibilityHidden(true)`).
- Выбранный элемент помечается `.accessibilityAddTraits(.isSelected)`.

## Именование

- Файл = один публичный тип того же имени.
- Экраны — `<Имя>View.swift`, модели экранов — `<Имя>ViewModel.swift`,
  элементы списков — `<Имя>Row` / `<Имя>Card`.
- Компонент называется по тому, что он делает, а не по экрану, где появился:
  `PrimaryButton`, `FormTextField`, `ScreenHeader`.
- Папка фичи — с заглавной буквы и по имени фичи, а не по имени типа
  (`Features/Chat`, не `Features/chatView`).
- Приватные части вью — вычисляемые свойства (`private var header`), чтобы
  `body` оставался читаемым.

## Комментарии

Пишем на русском и только там, где код не объясняет себя сам: почему выбрано
решение, что появится вместо заглушки (`// TODO:`), чем чревато изменение.
Пересказ кода комментарием не нужен.
