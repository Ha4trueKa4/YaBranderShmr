# Применение миграций на сервере вручную

Инструкция для случая, когда БД на виртуальной машине Yandex Cloud были подняты
**вручную** (не через CI-деплой), и нужно накатить миграции. Здесь помечено, где
выполнять команду: **[ЛОКАЛЬНО]** (ваш компьютер) или **[НА СЕРВЕРЕ]** (SSH на ВМ).

> Если БД поднимались **через CI-деплой** — миграции применяются автоматически
> при каждом деплое, ручных действий не требуется.

## Условные обозначения

- `APP_DIR=~/team-05-project-konstruktor-avatarok-dlya-rabochikh-chatov-vizitok-i-profilei/backend`
  — каталог на сервере, куда CI кладёт `.env` и файлы миграций.
- Контейнеры БД: `service-postgres` (основная, `ya_brander_db_1`) и (если есть)
  `service-vector-postgres` (векторная, `vector_db`).
- Сеть: `ya-brander-net`. Учётка БД: `user` / `password`.
- Ключ SSH до ВМ: например `~/.ssh/id_ed25519` (подставьте свой путь и `user@IP`).

---

## Шаг 0. Логин на сервер

**[ЛОКАЛЬНО]** подключитесь к ВМ:

```bash
ssh -i ~/.ssh/id_ed25519 USER@SERVER_IP
```

Дальше команды, отмеченные **[НА СЕРВЕРЕ]**, выполняются уже внутри этой сессии.

---

## Шаг 1. Убедиться, что файлы миграций есть на сервере

CI-деплой кладёт файлы в `$APP_DIR/postgresql` и `$APP_DIR/scripts`. Если БД
поднимались вручную и CI ещё не деплоил — файлов может не быть.

**[НА СЕРВЕРЕ]** проверьте:

```bash
ls -la $APP_DIR/postgresql/migrations/db-1 $APP_DIR/scripts 2>/dev/null
```

Если каталоги/файлы есть — переходите к Шагу 2.

Если их нет — доставьте их с локальной машины (см. ниже).

---

## Шаг 1.1 (если файлов нет). Доставка файлов миграций

**[ЛОКАЛЬНО]** (не в сессии SSH, из корня репозитория):

```bash
# создать каталог на сервере
ssh -i ~/.ssh/id_ed25519 USER@SERVER_IP "mkdir -p $APP_DIR"

# передать postgresql/ и scripts/ tar-стримом
tar -C backend -czf - postgresql scripts \
  | ssh -i ~/.ssh/id_ed25519 USER@SERVER_IP "tar -xzf - -C $APP_DIR"
```

После этого на сервере в `$APP_DIR` появятся `postgresql/` и `scripts/`.

---

## Шаг 2. Проверить, что контейнеры БД подключены к сети

**[НА СЕРВЕРЕ]**:

```bash
docker ps --format '{{.Names}}' | grep -E 'service-postgres|service-vector-postgres'

# сеть должна существовать и контейнеры БД должны быть к ней подключены
docker network create ya-brander-net 2>/dev/null || true
docker network connect ya-brander-net service-postgres 2>/dev/null || true
# если векторная БД есть:
docker network connect ya-brander-net service-vector-postgres 2>/dev/null || true
```

---

## Шаг 3. ВАЖНО: проверить журнал `schema_migrations`

Если схема на сервере была создана **вручную** (например, из
`postgresql/schemas/db-1.sql`), таблицы уже существуют, а журнала
`schema_migrations` ещё нет. В этом случае первый запуск миграции упадёт с
`relation "users" already exists`.

**[НА СЕРВЕРЕ]** посмотрите, есть ли журнал и какие версии в нём:

```bash
docker exec service-postgres psql -U user -d ya_brander_db_1 -c "SELECT version FROM schema_migrations ORDER BY version;"
```

Варианты:

- **Журнал пуст / таблицы нет, но схема уже создана вручную** → нужно один раз
  «прописать» уже существующие миграции, чтобы скрипт их пропустил (иначе они
  упадут на «already exists»).

  **Важно:** сначала гарантируем, что таблица журнала существует (для ручных БД
  её может не быть), затем вставляем версию. Обе команды **[НА СЕРВЕРЕ]**:

  ```bash
  # 1) создать журнал (тот же DDL, что в run_migrations.sh)
  docker exec service-postgres psql -U user -d ya_brander_db_1 -v ON_ERROR_STOP=1 -c \
    "CREATE TABLE IF NOT EXISTS schema_migrations (version TEXT PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL DEFAULT now());"

  # 2) пометить уже существующую миграцию как применённую
  docker exec service-postgres psql -U user -d ya_brander_db_1 -v ON_ERROR_STOP=1 -c \
    "INSERT INTO schema_migrations (version) VALUES ('0001_initial_schema.sql') ON CONFLICT (version) DO NOTHING;"
  ```

  Если векторная БД существует и её схема тоже создана вручную — аналогично
  **[НА СЕРВЕРЕ]**:

  ```bash
  docker exec service-vector-postgres psql -U user -d vector_db -v ON_ERROR_STOP=1 -c \
    "INSERT INTO schema_migrations (version) VALUES ('0001_vector_examples.sql') ON CONFLICT (version) DO NOTHING;"
  ```

  > После этого скрипт применит только **новые** файлы миграций, а уже
  > существующие таблицы не тронет.

- **Журнал заполнен, всё сходится** → ничего делать не нужно, переходите к Шагу 4.

- **Ни таблиц, ни журнала (чистая БД)** → ничего прописывать не нужно, миграции
  создадут схему с нуля.

---

## Шаг 4. Применить миграции к основной БД

**[НА СЕРВЕРЕ]**. Разовый контейнер `postgres:18` выполняет
`run_migrations.sh` против `service-postgres`. Это ровно тот же шаг, что в CI.

> `SEED_FILE` здесь **не передаётся** — сид-файлы начинаются с `TRUNCATE` и
> стёрли бы реальные данные. Применяются только версионированные миграции.

```bash
docker run --rm --name ya-brander-migrate-manual --network ya-brander-net \
  -e PGHOST=service-postgres -e PGPORT=5432 \
  -e PGUSER=user -e PGPASSWORD=password -e PGDATABASE=ya_brander_db_1 \
  -e MIGRATIONS_DIR=/postgresql/migrations/db-1 \
  -v $APP_DIR/postgresql:/postgresql:ro \
  -v $APP_DIR/scripts:/scripts:ro \
  postgres:18 bash /scripts/run_migrations.sh
```

Ожидаемый вывод: `Apply: ...` для новых миграций и/или `Skip: ...` для уже
применённых, в конце — `All migrations up to date.`

---

## Шаг 5. Применить миграции к векторной БД (если она есть)

**[НА СЕРВЕРЕ]**. Важно: контейнер `service-vector-postgres` должен быть
построен на образе с расширением `vector` (например, `pgvector/pgvector:pg18`),
иначе миграция не сможет создать расширение и HNSW-индекс. Клиент (`postgres:18`)
для этого подходит.

```bash
docker run --rm --name ya-brander-migrate-vector-manual --network ya-brander-net \
  -e PGHOST=service-vector-postgres -e PGPORT=5432 \
  -e PGUSER=user -e PGPASSWORD=password -e PGDATABASE=vector_db \
  -e MIGRATIONS_DIR=/postgresql/migrations/vector-db \
  -v $APP_DIR/postgresql:/postgresql:ro \
  -v $APP_DIR/scripts:/scripts:ro \
  postgres:18 bash /scripts/run_migrations.sh
```

Если контейнера `service-vector-postgres` нет — пропустите этот шаг (векторный
поиск просто будет недоступен, пока векторная БД не поднята).

---

## Шаг 6. Проверить результат

**[НА СЕРВЕРЕ]**:

```bash
docker exec service-postgres psql -U user -d ya_brander_db_1 -c "SELECT version, applied_at FROM schema_migrations ORDER BY version;"

# список таблиц основной БД
docker exec service-postgres psql -U user -d ya_brander_db_1 -c "\dt"

# если векторная БД есть
docker exec service-vector-postgres psql -U user -d vector_db -c "SELECT version FROM schema_migrations ORDER BY version;"
```

---

## Шпаргалка «где что выполнять»

| Действие | Где выполнять |
|----------|---------------|
| Доставка `postgresql/` и `scripts/` на сервер (если нет) | **[ЛОКАЛЬНО]** (`tar ... \| ssh ...`) |
| Логин на ВМ | **[ЛОКАЛЬНО]** (`ssh ...`) |
| Проверка/создание сети, подключение контейнеров БД | **[НА СЕРВЕРЕ]** |
| Проверка/доинициализация журнала `schema_migrations` | **[НА СЕРВЕРЕ]** (`docker exec ... psql`) |
| Запуск миграций основной/векторной БД | **[НА СЕРВЕРЕ]** (`docker run ... postgres:18 bash /scripts/run_migrations.sh`) |
| Проверка результата | **[НА СЕРВЕРЕ]** (`docker exec ... psql -c "SELECT ..."`) |

Полное описание механизма миграций, правил добавления/изменения колонок и рисков —
в [`MIGRATIONS.md`](MIGRATIONS.md).