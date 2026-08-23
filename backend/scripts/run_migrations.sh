#!/usr/bin/env bash
# Применение SQL-миграций к PostgreSQL.
#
# Скрипт идемпотентен: применяет только те миграции из каталога $MIGRATIONS_DIR,
# которые ещё не были применены. Отслеживание ведётся в таблице schema_migrations,
# которая создаётся скриптом автоматически.
#
# Если задан SEED_FILE, после миграций выполняется файл данных (идемпотентно
# повторное выполнение на его усмотрение, например через TRUNCATE в начале).
#
# Параметры задаются переменными окружения (см. значения по умолчанию):
#   PGHOST, PGPORT, PGUSER, PGPASSWORD, PGDATABASE, MIGRATIONS_DIR, SEED_FILE
#   ADMIN_EMAIL, ADMIN_PASSWORD_HASH - для создания администратора
set -euo pipefail

PGHOST="${PGHOST:-service-postgres}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-user}"
PGPASSWORD="${PGPASSWORD:-password}"
PGDATABASE="${PGDATABASE:-ya_brander_db_1}"
MIGRATIONS_DIR="${MIGRATIONS_DIR:-postgresql/migrations/db-1}"
SEED_FILE="${SEED_FILE:-}"

export PGPASSWORD

# Гарантируем существование журнала применённых миграций.
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 \
  -c 'CREATE TABLE IF NOT EXISTS schema_migrations (
        version    TEXT PRIMARY KEY,
        applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
      );'

shopt -s nullglob
for file in "$MIGRATIONS_DIR"/*.sql; do
  version="$(basename "$file")"

  # Значение $version подставляется напрямую из имени файла в каталоге миграций,
  # то есть управляется репозиторием и безопасно для прямой интерполяции в SQL.
  # (psql-переменные вида :'version' в командах -c не интерполируются.)
  applied="$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -tAc \
    "SELECT 1 FROM schema_migrations WHERE version = '$version'")"

  if [[ "$applied" == "1" ]]; then
    echo "Skip: $version (already applied)"
    continue
  fi

  echo "Apply: $version"

  # Для миграции с администратором устанавливаем параметры через SET
  if [[ "$version" == "0004_add_admin.sql" ]]; then
    echo "Applying admin migration with ADMIN_EMAIL=${ADMIN_EMAIL:-not set}"

    # Устанавливаем параметры сессии через SET перед выполнением миграции
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 \
      -c "SET my.admin_email = '${ADMIN_EMAIL:-}'" \
      -c "SET my.admin_password_hash = '${ADMIN_PASSWORD_HASH:-}'" \
      -f "$file"
  else
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 -f "$file"
  fi

  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 \
    -c "INSERT INTO schema_migrations (version) VALUES ('$version')"
done

# Применяем сид-данные, если указаны.
if [[ -n "$SEED_FILE" ]]; then
  echo "Seed: $SEED_FILE"
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 -f "$SEED_FILE"
fi

echo "All migrations up to date."