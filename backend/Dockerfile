# SYNTAX directive обязательна для BuildKit mounts
# syntax=docker/dockerfile:1

# ---- стадия сборки: берем образ из локального YCR ----
FROM cr.yandex/crpso93aahtuvujb7jlk/userver-pg:v3.0 AS build

WORKDIR /build

COPY CMakeLists.txt Makefile requirements.txt ./
COPY configs ./configs

# Кэшируем конфигурацию CMake
RUN --mount=type=cache,target=/build/build \
    cmake -B build -DCMAKE_BUILD_TYPE=Release

COPY src ./src

# Кэшируем скомпилированные .o файлы: пересобираются только изменённые файлы
RUN --mount=type=cache,target=/build/build \
    cmake --build build -j$(nproc) --target ya_brander && \
    cp /build/build/ya_brander /tmp/ya_brander

# ---- стадия рантайма: берем готовый образ с библиотеками ----
FROM cr.yandex/crpso93aahtuvujb7jlk/ya-brander-runtime-base:latest

WORKDIR /app

COPY --from=build /tmp/ya_brander /usr/local/bin/ya_brander
COPY --from=build /build/configs /etc/ya_brander

RUN ldd /usr/local/bin/ya_brander 2>&1 | tee /tmp/ldd.log && \
    if grep -q "not found" /tmp/ldd.log; then cat /tmp/ldd.log; exit 1; else echo "All dependencies satisfied"; fi

EXPOSE 8080
CMD ["ya_brander", "--config", "/etc/ya_brander/static_config.yaml", "--config_vars", "/etc/ya_brander/config_vars.docker.yaml"]