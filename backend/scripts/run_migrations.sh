#!/usr/bin/env bash
# Применение SQL-миграций к PostgreSQL.
#
# Скрипт идемпотентен: применяет только те миграции из каталога $MIGRATIONS_DIR,
# которые ещё не были применены. Отслеживание ведётся в таблице schema_migrations,
# которая создаётся скриптом автоматически.
#
# Параметры задаются переменными окружения (см. значения по умолчанию):
#   PGHOST, PGPORT, PGUSER, PGPASSWORD, PGDATABASE, MIGRATIONS_DIR
set -euo pipefail

PGHOST="${PGHOST:-service-postgres}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-user}"
PGPASSWORD="${PGPASSWORD:-password}"
PGDATABASE="${PGDATABASE:-ya_brander_db_1}"
MIGRATIONS_DIR="${MIGRATIONS_DIR:-postgresql/migrations}"

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

  applied="$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -tAc \
    "SELECT 1 FROM schema_migrations WHERE version = '$version'")"

  if [[ "$applied" == "1" ]]; then
    echo "Skip: $version (already applied)"
    continue
  fi

  echo "Apply: $version"
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 -f "$file"

  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
    -c "INSERT INTO schema_migrations (version) VALUES ('$version')"
done

echo "All migrations up to date."