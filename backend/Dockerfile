# Тонкий рантайм-образ backend (C++/userver).
#
# Бинарник ya_brander НЕ собирается здесь. Он компилируется инкрементально в CI-раннере
# (.sourcecraft/build-backend-compile.yaml) с персистентным кэшем backend/build/ и попадает
# в build-контекст через CI-артефакт (backend/build/ya_brander). Этот Dockerfile лишь
# упаковывает готовый бинарник + конфиги в минимальный runtime на ubuntu:22.04.
#
# Требуемые файлы в build-контексте (backend/):
#   build/ya_brander   — собранный бинарник (из артефакта compile-задания);
#   configs/           — конфиги сервиса.
FROM ubuntu:22.04

# Рантайм-зависимости бинарника (ldd): базовые boost/curl/libpq и пр.
RUN apt-get update && apt-get install -y --no-install-recommends \
        libstdc++6 libpq5 libldap-2.5-0 ca-certificates \
        libboost-atomic1.74.0 \
        libboost-iostreams1.74.0 \
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
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Готовый бинарник и конфиги из build-контекста.
COPY build/ya_brander /usr/local/bin/ya_brander
COPY configs /etc/ya_brander

# Проверка, что все динамические зависимости удовлетворены.
RUN ldd /usr/local/bin/ya_brander 2>&1 | tee /tmp/ldd.log && \
    if grep -q "not found" /tmp/ldd.log; then cat /tmp/ldd.log; exit 1; else echo "All dependencies satisfied"; fi

EXPOSE 8080
CMD ["ya_brander", "--config", "/etc/ya_brander/static_config.yaml", "--config_vars", "/etc/ya_brander/config_vars.docker.yaml"]
