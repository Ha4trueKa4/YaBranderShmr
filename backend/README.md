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