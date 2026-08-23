#!/usr/bin/env bash

# Exit on any error and treat unset variables as errors
set -euo pipefail

if ! command -v redis-server > /dev/null 2>&1 && ! command -v valkey-server > /dev/null 2>&1; then
    # Нужен для testsuite-плагина pytest_userver.plugins.redis (functional-тесты
    # ProjectManagerComponent): он сам поднимает эфемерный redis-server+sentinel
    # локально в контейнере, но бинарников нет в базовом образе userver-pg.
    # apt-get update может упасть из-за отвалившегося стороннего репозитория в
    # базовом образе (напр. mariadb) — это не должно мешать install: остальные
    # репозитории (в т.ч. основной ubuntu) к этому моменту уже обновились.
    apt-get update || true
    apt-get install -y --no-install-recommends redis-server redis-tools
fi

DIR_UID="$(stat -c '%u' .)"

if ! id -u user > /dev/null 2> /dev/null; then
    if [ "$DIR_UID" = "0" ]; then
        useradd --create-home --no-user-group user
    else
        useradd --create-home --no-user-group --uid $DIR_UID user
    fi
elif [ "$DIR_UID" != "0" ]; then
    usermod -u $DIR_UID user
fi

HOME=/home/user sudo -E -u user "$@"
