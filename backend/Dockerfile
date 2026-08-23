# ---- стадия сборки: используем готовый userver-образ ----
FROM ghcr.io/userver-framework/ubuntu-22.04-userver-pg:v3.0 AS build

WORKDIR /build

# Копируем редко меняющиеся файлы ПЕРВЫМИ, чтобы кэш Docker-слоёв переиспользовался
# при правках только в src/ (иначе любое изменение исходников сбрасывает весь кэш cmake).
COPY CMakeLists.txt ./
COPY configs ./configs
COPY Makefile ./
COPY docker-compose.yml ./
COPY requirements.txt ./

# Исходники копируем ПОСЛЕДНИМИ — они меняются чаще всего.
COPY src ./src
COPY sql ./sql

# Собираем Release-версию бинарника ya_brander
RUN cmake -B build -DCMAKE_BUILD_TYPE=Release \
    && cmake --build build -j$(nproc) --target ya_brander

# ---- стадия рантайма ----
FROM ubuntu:22.04

RUN apt-get update && apt-get install -y --no-install-recommends \
        libstdc++6 libpq5 libldap-2.5-0 ca-certificates \
        libboost-atomic1.74.0 \
        libboost-iostreams1.74.0 \
        libboost-locale1.74.0 \
        libboost-filesystem1.74.0 \
        libboost-program-options1.74.0 \
        libboost-stacktrace1.74.0 \
        libcurl4 \
        libc-ares2 \
        libev4 \
        libnghttp2-14 \
        libcrypto++8 \
        libjemalloc2 \
        libre2-9 \
        libfmt8 \
        libcctz2 \
        libyaml-cpp0.7 \
        libhiredis0.14 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /build/build/ya_brander /usr/local/bin/ya_brander
COPY --from=build /build/configs /etc/ya_brander

RUN ldd /usr/local/bin/ya_brander 2>&1 | tee /tmp/ldd.log && \
    if grep -q "not found" /tmp/ldd.log; then cat /tmp/ldd.log; exit 1; else echo "All dependencies satisfied"; fi

EXPOSE 8080
CMD ["ya_brander", "--config", "/etc/ya_brander/static_config.yaml", "--config_vars", "/etc/ya_brander/config_vars.docker.yaml"]
