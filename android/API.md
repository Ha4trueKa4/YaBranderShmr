# API и сетевой слой

Как Android-клиент общается с бэкендом. Контракт описан в `../swagger.yml`,
но расходится с реальностью — что из него действительно поднято, видно в
`../backend/configs/static_config.yaml` и в разделе «Состояние стенда».
Здесь — что уже реализовано, что только описано типами, и где какие грабли.

## Подключение

| Параметр | Значение |
| --- | --- |
| Base URL | `http://158.160.187.118:8080/api/v1/` |
| Авторизация | `Authorization: Bearer <access_token>` |
| Формат | JSON, поля в `snake_case` |

Base URL задаётся через `buildConfigField("String", "API_BASE_URL", ...)` в
`app/build.gradle.kts` и читается из `ApiConfig.BaseUrl`. В коде фич
`BuildConfig` не трогаем. Локальный бэкенд подставляется без правки файла:

```
./gradlew :app:installDebug -PapiBaseUrl=http://10.0.2.2:8080/api/v1/
```

Три момента, на которых легко потерять время:

- **Завершающий слэш в base URL обязателен.** Без него Ktor заменит последний
  сегмент пути: `.../api/v1` + `auth/login` → `.../api/auth/login`.
- **`localhost` на эмуляторе — это сам девайс.** Бэкенд на машине разработчика
  доступен по `http://10.0.2.2:8080`, на физическом устройстве — по адресу
  машины в локальной сети.
- **HTTP без TLS разрешён только в debug.** Android с API 28 режет cleartext,
  поэтому в `app/src/debug/AndroidManifest.xml` стоит `usesCleartextTraffic`.
  В release-сборке этого разрешения нет, а стенд работает по HTTP — значит
  release с сетью сейчас не соберётся рабочим. Пока у бэкенда нет HTTPS,
  проверяем только debug.

## Устройство сетевого слоя

```
core/network/
  ApiConfig.kt            base URL и таймауты
  ApiJson.kt              общая настройка Json
  HttpClientFactory.kt    сборка единственного HttpClient
  AuthTokenProvider.kt    чтение токена (нужно клиенту)
  AuthTokenStore.kt       чтение + запись (нужно репозиторию авторизации)
  InMemoryAuthTokenStore.kt
  ApiCall.kt              apiCall { } — маппинг ошибок Ktor в доменные
  NetworkException.kt     разбор тела ошибки в ValidationErrorDto
  HttpMethod.kt           какие методы можно повторять
  Throwable.kt            классификация исключений
  dto/                    ApiErrorDto, ValidationErrorDto
core/domain/
  NetworkException.kt     доменная ошибка сети
core/di/NetworkModule.kt  HttpClient, Json и токен-хранилище в Koin
```

Правила: репозиторий фичи инъектит готовый `HttpClient`, свой не собирает;
каждый вызов заворачивается в `apiCall { }`; DTO не поднимаются выше `data/`.

### Ошибки

`apiCall { }` сводит всё к `NetworkException`:

| Ситуация | Тип |
| --- | --- |
| нет сети, DNS, хост недоступен | `NoConnection` |
| истёк таймаут | `Timeout` |
| 401 | `Unauthorized` |
| 403 | `Forbidden` |
| 404 | `NotFound` |
| прочие 4xx | `ClientError(code)` |
| 5xx | `ServerError(code)` |
| тело не разобралось | `Serialization` |
| остальное | `Unknown` |

Исходный `ResponseException` сохраняется в `cause` — оттуда достаётся тело
ошибки (`ApiErrorDto`, `ValidationErrorDto`) там, где фиче важен не только
статус.

### Повторы

Повторяются **только `GET` и `HEAD`** — на 5xx и на обрыв соединения, до двух
раз с экспоненциальной паузой. `POST` не повторяется сознательно: шлюз может
ответить 5xx уже после того, как бэкенд принял запрос, и ретрай `POST
/projects/{id}/generate` создал бы вторую генерацию.

## Реализовано

Проверено на живом стенде: `AuthApiIntegrationTest` гоняет настоящий
`AuthRepositoryImpl` по сети. По умолчанию тест пропускается — в CI ему в сеть
не место:

```
API_INTEGRATION=1 ./gradlew :app:testDebugUnitTest --tests '*AuthApiIntegrationTest'
```

### `POST /auth/register`

`AuthRepository.register(email, password, nickname)` → `AuthSession`.

| Ответ | Поведение |
| --- | --- |
| 201 | `AuthSession`, токен сохраняется в `AuthTokenStore` |
| 400 | `AuthException.RegistrationRejected(field)` — значение отверг сервер |
| 409 | `AuthException.RegistrationConflict(field)` — email или никнейм заняты |

Поле формы берётся из `ValidationError.code`, у обоих статусов тело одинаковое:

| Код | Поле |
| --- | --- |
| `INVALID_EMAIL`, `EMAIL_EXISTS` | `Field.Email` |
| `INVALID_PASSWORD` | `Field.Password` |
| `INVALID_NICKNAME`, `NICKNAME_EXISTS` | `Field.Nickname` |
| незнакомый код | `Field.Unknown` |

Правила валидации живут на бэкенде и на клиенте **не продублированы** — иначе
разъедутся при первом же его изменении. Сейчас там: пароль 8–128 символов
обязательно с буквой и цифрой, никнейм 3–64 символа, email по regex и до 128
символов. Сообщения сервера английские (`"Password must be 8-128 chars…"`),
показывать их пользователю нельзя — экран сам подбирает текст по `field`.

### `POST /auth/login`

`AuthRepository.login(email, password)` → `AuthSession`.

| Ответ | Поведение |
| --- | --- |
| 200 | `AuthSession`, токен сохраняется в `AuthTokenStore` |
| 401 | `AuthException.InvalidCredentials` — неверная пара логин/пароль, а не «сессия протухла»; токен не трогаем |

`AuthRepository.logout()` — локальный сброс токена, ручки на бэкенде нет.

Токен кладёт в хранилище сам репозиторий: дальше `HttpClient` подставляет
заголовок на каждом запросе, и вызывающему коду не нужно об этом помнить.
Хранится он пока в памяти процесса (`InMemoryAuthTokenStore`) — перезапуск
приложения разлогинивает.

### Нормализация email

Репозиторий приводит email к нижнему регистру и обрезает пробелы, никнейм —
только обрезает. Пароль не трогаем: пробелы в нём значимы.

Это не косметика. Бэкенд ищет пользователя точным сравнением
(`WHERE email = $1`) и при регистрации адрес не нормализует. Без приведения
`Ivan@mail.ru` и `ivan@mail.ru` — два разных аккаунта, а вход «тем же» адресом
даёт 401. Автокапитализация на клавиатуре телефона делает это регулярно.

## Состояние стенда

Проверено вручную 13.08.2026 на `158.160.187.118:8080`. Данные быстро
устаревают — перед тем как закладываться на ручку, стоит перепроверить.

| Ручка | Состояние |
| --- | --- |
| `POST /auth/register`, `POST /auth/login` | работают |
| `GET /ping`, `GET /check` | работают, вне `/api/v1` |
| `POST /projects` | **500** — падает на заглушке сохранения SVG в S3 |
| `POST`/`DELETE /users/{id}/subscribe` | **500**, похоже не накатана таблица `subscriptions` |
| `GET`/`PATCH`/`DELETE /projects/{id}`, `POST /projects/{id}/generate` | зарегистрированы, отдают 404 и 401 корректно, но проверить нечем: id живого проекта не получить |
| `GET /users/{id}` | **ручки нет** — есть в swagger, но не в коде бэкенда |
| ключ LLM | протух (`UNAUTHENTICATED`), генерация не заработает |

Списочных ручек нет вообще: ни ленты, ни «моих проектов». `GET /api/v1/projects`
отдаёт 405, доступен только `POST`. Экран фида наполнять пока нечем.

## Описано типами, не реализовано

DTO готовы, репозиториев и вызовов пока нет. Писать их вслепую по swagger
смысла мало — контракт ответов не подтверждён, см. состояние стенда выше.

| Метод | Запрос | Ответ | DTO |
| --- | --- | --- | --- |
| `GET /users/{id}` | — | `UserProfile` + `projects` | `feature/profile/data/dto/UserProfileResponseDto` |
| `POST /projects` | `name`, `description` | `Project` | `feature/project/data/dto/CreateProjectRequestDto`, `ProjectDto` |
| `GET /projects/{id}` | — | `Project` | `ProjectDto` |
| `PATCH /projects/{id}` | `is_public?`, `data?` | тела нет | `UpdateProjectRequestDto` |
| `DELETE /projects/{id}` | — | тела нет | — |
| `POST /projects/{id}/generate` | `prompt` | `ProjectData` | `GenerateProjectRequestDto`, `ProjectDataDto` |

Авторизация: `Bearer` обязателен для `POST /projects`, `PATCH`, `DELETE` и
`generate`; для `GET /users/{id}` и `GET /projects/{id}` — опционален.

Частичное обновление в `PATCH` работает за счёт `explicitNulls = false` в
`ApiJson`: поля со значением `null` не попадают в JSON, а не уезжают как
`"is_public": null`, который бэкенд мог бы принять за сброс значения.

## Известные расхождения в контракте

- **Swagger описывает больше, чем есть.** `GET /users/{id}` объявлен в
  контракте, но хендлера на бэкенде нет. Источник истины по факту — не swagger,
  а `backend/configs/static_config.yaml`: там перечислены реально
  зарегистрированные пути.
- **`name` против `nickname`.** `AuthResponse.user` отдаёт `nickname`,
  а `GET /users/{id}` в swagger — `name`. Пока ручки нет, вопрос открыт;
  до ответа бэкенда держим поля раздельно.
- **Битый токен даёт 403, а не 401.** То есть `NetworkException.Forbidden`.
  Авто-разлогин по 401 на него не сработает — учесть, когда будет обработка
  протухшей сессии.
- **Email регистрозависим на бэкенде.** Клиент это компенсирует (см. выше), но
  через веб и iOS дубли по регистру всё ещё создаются. Бэкенду нужен
  `lower(email)` в схеме.
- **`code` двух разных типов.** В схеме `Error` это число, в `ValidationError` —
  строка (`EMAIL_EXISTS`). Одним классом не разобрать, поэтому `ApiErrorDto` и
  `ValidationErrorDto` — разные типы.
- **`required` почти нигде не проставлен.** Обязательные поля есть только в
  `RegisterRequest`, `LoginRequest` и `AuthResponse` — только их DTO
  не-nullable. В остальных схемах поля формально опциональны, поэтому в Kotlin
  они nullable: приукрашивать контракт значениями по умолчанию не стоит, иначе
  отсутствие поля замаскируется нулём.
- **Нет обновления токена.** `refresh_token` в контракте отсутствует, токен
  назван долгоживущим. Когда он всё-таки протухнет, клиент получит 401 —
  обработка (разлогин и уход на экран входа) появится вместе с экранами.
