# Бэкенд (userver)

Сервис на C++/userver. Сборка и запуск — только через Docker: образ уже
содержит userver со всеми зависимостями, отдельная инициализация сабмодуля
не требуется.

## Требования
- Docker и docker-compose
- Если установлен только `docker compose` (v2), создайте `Makefile.local`:
  `DOCKER_COMPOSE = docker compose`

## Сборка и запуск
```bash
make docker-build-debug          # собрать
make docker-start-service-debug  # запустить (поднимет Postgres + сервис на :8080)
```

Проверка работоспособности:
```bash
curl http://localhost:8080/ping  # должен вернуть 200
```

## Тесты
```bash
make docker-test-debug
```

## Очистка данных БД
```bash
make docker-clean-data
```
Если команда падает с `Permission denied` на `./.pgdata` — файлы принадлежат
пользователю контейнера, удалите вручную: `sudo rm -rf ./.pgdata`.

---

## Настройка IDE

**VS Code:** установите расширение «Dev Containers», откройте директорию `backend/` и выберите «Reopen in Container».

**CLion:** откройте через JetBrains Gateway → Dev Containers → `.devcontainer/devcontainer.json`.

Сначала выполните один раз команду `make docker-build-debug` — она сгенерирует файл `build_debug/compile_commands.json`,
который необходим `clangd` для распознавания заголовочных файлов (includes) библиотеки `userver`.
Без этого директива `#include <userver/...>` будет подсвечиваться как неразрешённая (*unresolved*).

---

## Git в Dev Container

Dev Container монтирует только папку `backend/`, а `.git`
находится в корне монорепозитория — уровнем выше. Поэтому **внутри
контейнера git не работает** (вы увидите ошибку "not a git repository" при
старте — это нормально, её можно игнорировать). Все операции с git —
коммиты, ветки, push/pull — выполняйте из **обычного терминала на хосте**,
находясь в корне репозитория. Контейнер используйте только для сборки,
редактирования и запуска.

---

# Backend (userver)

C++/userver service. Build and run through Docker only: the image already
ships userver with all dependencies, so no submodule init is needed.

## Requirements
- Docker and docker-compose
- If you only have `docker compose` (v2), create `Makefile.local` with:
  `DOCKER_COMPOSE = docker compose`

## Build & run
```bash
make docker-build-debug          # build
make docker-start-service-debug  # run (starts Postgres + service on :8080)
```

Health check:
```bash
curl http://localhost:8080/ping  # should return 200
```

## Tests
```bash
make docker-test-debug
```

## Reset DB data
```bash
make docker-clean-data
```
If it fails with `Permission denied` on `./.pgdata`, the files are owned by the
container user — remove manually: `sudo rm -rf ./.pgdata`.

---

## IDE setup

VS Code: install the "Dev Containers" extension, open `backend/`, choose
"Reopen in Container". CLion: open via JetBrains Gateway → Dev Containers →
`.devcontainer/devcontainer.json`.

Run `make docker-build-debug` once first — it generates
`build_debug/compile_commands.json`, which clangd needs to resolve userver
includes. Without it, `#include <userver/...>` shows as unresolved.

--- 

## Git in the Dev Container

The Dev Container mounts only the `backend/` folder, while `.git`
lives at the monorepo root, one level above. So **git does not work inside
the container** (you'll see a "not a git repository" error on startup — this
is expected and safe to ignore). Do all git operations — commits, branches,
push/pull — from a **normal terminal on the host**, at the repo root. Use
the container only for building, editing, and running.