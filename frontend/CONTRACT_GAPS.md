# Пробелы API-контракта

Этот файл — рабочий список полей и endpoints, которых не хватает фронтенду во время вёрстки. После изменения `swagger.yml` типы нужно сгенерировать заново командой `pnpm ts:generate`, а соответствующий пункт здесь отметить как выполненный.

## Что нужно добавить в API

Список сверяется только со `swagger.yml` и сгенерированными из него типами. Поле или endpoint считается добавленным лишь после явного описания в OpenAPI: наличие такого значения в фактическом ответе backend само по себе не закрывает пункт.

Поля, необходимые карточкам, нужно добавить в `required`. Если значение может отсутствовать, его следует возвращать как `null` (`nullable: true` в OpenAPI 3.0), а не опускать поле из ответа.
- В `ProjectDescription` добавить: `updated_at: string` с форматом `date-time`, `status: "draft" | "published"`, `preview_url: string | null` с форматом `uri`, `published_at: string | null` с форматом `date-time`.
- Для публичных проектов описать схему автора, например `PublicProjectAuthor`: `id: string`, `name: string`, `username: string`, `avatar_url: string | null` с форматом `uri`; добавить `author: PublicProjectAuthor` в ответ с публичным проектом.
- Добавить `GET /projects` со схемой query-параметров: `cursor?: string`, `limit?: integer`, `visibility?: "public" | "private"`, `status?: "draft" | "published"`; ответ `ProjectListResponse`: `items: ProjectDescription[]`, `next_cursor: string | null`.
- Добавить `GET /feed/projects` с query-параметрами `cursor?: string`, `limit?: integer`; ответ `FeedProjectsResponse`: `items: FeedProject[]`, `next_cursor: string | null`. В `FeedProject` явно описать `id: string`, `name: string`, `preview_url: string | null` (`uri`), `published_at: string` (`date-time`), `author: PublicProjectAuthor`.
- Для публичного профиля явно описать один вариант в OpenAPI: либо `GET /users/{id}` возвращает гостю только публичные `projects`, либо добавить `GET /users/{id}/projects?visibility=public` с ответом `ProjectListResponse`.

`AuthResponse` менять не нужно: он уже возвращает обязательные `access_token`, `token_type` и `user` (`id`, `email`, `nickname`).

## Пользователь и профиль

### Уже есть

- `GET /users/{id}` возвращает `UserProfile` и список `projects`.
- В `UserProfile` описаны `id`, `name`, `subscribers_count`, `subscriptions_count` и `is_subscribed`.
- Есть `POST` и `DELETE /users/{id}/subscribe`.

### Данные для ProfileHeader

- Имя и username собственного профиля берутся из `authStore.getSession.user.nickname`.
- Количество проектов вычисляется из длины списка `projects`.
- Bio временно не отображается в интерфейсе.
- Счётчики подписчиков и подписок уже есть в `UserProfile` как `subscribers_count` и `subscriptions_count`.
- Состояние подписки уже есть в `UserProfile` как `is_subscribed`.

### Временное состояние во frontend

`ProfileHeader` пока принимает локальный `ProfileHeaderProfile`, а не `UserProfile`. На собственном профиле имя и username передаются из `authStore`, количество проектов — из полученного списка, а счётчики — из уже существующих полей `UserProfile`.

## Проекты

### Уже есть

- `ProjectDescription`: `id`, `author_id`, `name`, `description`, `is_public`, `created_at`.
- `Project` дополнительно возвращает SVG в поле `data`.
- Есть создание, чтение, обновление видимости, удаление и AI-генерация проекта.

### Не хватает для карточек проектов и списков

- `updated_at` — нужен для «last edited today» на собственном профиле; `created_at` не заменяет его.
- Явный статус проекта: например `draft | published`. Одного `is_public` недостаточно, чтобы отличить черновик от опубликованной работы.
- Данные для превью в списке: URL растрового preview либо гарантированно доступный SVG/preview-ключ. Загрузка полного editor `data` для каждой карточки будет избыточна.
- `author` как краткий объект пользователя (`id`, `name`, `username`, `avatar_url`) в публичных проектах — нужен Feed и карточкам чужого профиля.
- `published_at` — нужен для относительной даты публикации публичной работы.
- `GET /projects` для списка собственных проектов: с пагинацией, сортировкой и фильтром видимости/статуса.
- `GET /feed` для публичных проектов всех авторов: с пагинацией и данными автора.
- Для публичного профиля желательно определить, будет ли `GET /users/{id}` возвращать только публичные проекты гостю или нужен отдельный `GET /users/{id}/projects?visibility=public`.

### Карточки, уже ожидающие эти данные

- `OwnProjectCard`: `is_public`, статус, `updated_at`, preview.
- `FeedProjectCard`: preview, автор, его avatar и `published_at`.
- `PublicProjectCard`: preview, `description` и дата публикации.

## Лента публичных проектов (MVP)

Лента должна возвращать **проекты**, а не пользователей: одна карточка — одна публичная работа и её автор. Выбирать пользователей на клиенте и запрашивать их проекты отдельно нельзя: это приведёт к непредсказуемой выдаче, N+1-запросам и не позволит корректно сортировать все работы по свежести.

### Обязательный endpoint

`GET /feed/projects?cursor={cursor}&limit={limit}`

- Доступен неавторизованному пользователю; если передан access token, он используется только для будущего режима `following`.
- Возвращает только проекты, которые можно показать публично: `is_public: true` и опубликованный статус, если статус будет введён.
- Сортировка: `published_at DESC`, а для одинакового времени — стабильно по `id DESC`.
- `limit` — необязательный, с ограничением на стороне API; для UI достаточно значения по умолчанию 12 и максимума 24.
- Пагинация — cursor-based. Клиент передаёт `next_cursor` из предыдущего ответа, а не вычисляет offset или страницу сам.
- Когда следующих элементов нет, `next_cursor` возвращается как `null`. Пустая первая страница — `items: []` и `next_cursor: null`.

### Ответ `200`

```json
{
  "items": [
    {
      "id": "project_123",
      "name": "Orion Labs",
      "preview_url": "https://.../project_123.png",
      "published_at": "2026-08-14T10:30:00Z",
      "author": {
        "id": "user_456",
        "name": "Alex Rivera",
        "avatar_url": "https://.../user_456.png"
      }
    }
  ],
  "next_cursor": "opaque-cursor-or-null"
}
```

Обязательные поля элемента:

- `id` — для ключа списка и перехода к проекту;
- `name` — заголовок карточки;
- `preview_url` — готовое к отображению превью. Лента не должна получать полное поле `data` с SVG редактора ради карточки;
- `published_at` — дата, из которой frontend строит «2 дня назад»;
- `author.id`, `author.name`, `author.avatar_url` — автор и его аватар. `avatar_url` допускает `null`: тогда UI показывает инициалы из `name`.

Все даты — ISO 8601 в UTC. URL превью и аватара должны быть доступны клиенту без авторизации и иметь долгий срок жизни либо быть постоянными путями CDN.

### Вкладка Following — не блокирует MVP

Для первой версии достаточно реализовать `All`; кнопку `Following` можно оставить неактивной до появления требований. Когда подписки подключаются, endpoint можно расширить query-параметром `scope=all|following` (для `following` access token обязателен). Формат ответа, сортировка и курсор должны остаться теми же.

## Аутентификация

`AuthResponse` уже возвращает обязательный объект `user` с `id`, `email` и `nickname`; сгенерированные типы это учитывают. Экран авторизации пока использует моковый вход, а сессия хранит только `userId` и access token. При подключении login/register к API стоит сохранить и использовать доступные данные пользователя из ответа.

## Соглашения для будущих изменений

- Все даты возвращать ISO 8601 в UTC (`created_at`, `updated_at`, `published_at`).
- Для постраничных списков использовать единый формат: `items`, `next_cursor` (или `page`/`total` — выбрать один вариант для API).
- В публичных ответах не включать приватные проекты и их preview.
- Поля, необходимые для карточки списка, возвращать в самом списке — не требовать отдельный запрос на каждый проект или автора.
