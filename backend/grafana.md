# Дашборд Grafana

Этот документ объясняет пять панелей дашборда `ya_brander Overview`: откуда берётся каждая метрика, как считаются числа и что они означают на практике.

## Путь данных

Бэкенд сервис (userver) хранит счётчики и датчики в памяти и отдаёт их по адресу `GET /service/monitor?format=prometheus` на порту 8085. Контейнер Prometheus, описанный в конфигурации Docker Compose, опрашивает этот адрес каждые 15 секунд и сохраняет значения как временной ряд. Grafana затем читает данные из Prometheus и рисует панели ниже. Сервис сам ничего не отправляет; Prometheus всегда сам забирает данные.

## 1. Request rate (RPS), частота запросов

Метрика источник: `http_handler_rps`
Тип: счётчик (несмотря на название, значение только растёт; он считает каждый запрос, полученный обработчиком с момента запуска процесса)
Запрос: `sum(rate(http_handler_rps[1m])) by (http_handler)`

userver увеличивает этот счётчик один раз на каждый входящий запрос, отдельно для каждого обработчика. Поскольку обычный счётчик только растёт, Prometheus использует функцию `rate()`, чтобы превратить его в скорость: берётся последняя минута значений, разница делится на прошедшее время, и получается число запросов в секунду. Часть запроса `sum by (http_handler)` объединяет лишние метки, которые добавляет userver (`http_path`, `version`), чтобы каждый обработчик отображался одной линией, а не несколькими одинаковыми.

Что это значит: сколько трафика получает каждый эндпоинт прямо сейчас. Линия на нуле значит, что обработчик никто не вызывал последнюю минуту; растущая линия значит, что нагрузка увеличивается.

## 2. Error rate (5xx), частота ошибок

Метрика источник: `http_handler_reply_codes`
Тип: счётчик, отдельный ряд для каждого обработчика и каждого кода ответа
Запрос: `sum(rate(http_handler_reply_codes{http_code=~"5.."}[1m])) by (http_handler)`

Каждый раз, когда обработчик завершает запрос, userver увеличивает счётчик, привязанный к конкретному коду ответа. Фильтр `http_code=~"5.."` оставляет только коды от 500 до 599, то есть ошибки на стороне сервера. Как и выше, `rate()` превращает счётчик в число ошибок в секунду, а `sum by (http_handler)` объединяет все подходящие коды 5xx в одну линию на каждый обработчик.

Что это значит: количество серверных сбоев на каждом эндпоинте. Любое значение больше нуля здесь заслуживает внимания; всплеск обычно совпадает с деплоем, проблемой базы данных или сбоем внешнего API.

## 3. Latency p99, задержка p99

Метрика источник: `http_handler_timings`
Тип: датчик, процентиль уже посчитан внутри userver
Запрос: `http_handler_timings{percentile="p99"}`

userver хранит скользящее окно недавних длительностей запросов для каждого обработчика и периодически пересчитывает несколько процентилей внутри себя (p50, p90, p95, p99 и так далее), публикуя каждый как отдельное значение датчика с меткой `percentile`. Поскольку процентиль уже посчитан на стороне сервиса, Grafana просто читает значение `p99` напрямую; функция `rate()` здесь не нужна, так как это не счётчик.

Что это значит: 99 процентов запросов к этому обработчику завершаются быстрее, чем отображённое значение (в миллисекундах). Это гораздо более полезный сигнал, чем среднее значение, потому что он показывает опыт самых медленных реальных пользователей, а не скрывается за большинством быстрых запросов.

## 4. Postgres pool (active / busy), пул соединений Postgres

Метрики источники: `postgresql_connections_active`, `postgresql_connections_busy`
Тип: датчики
Запросы: `postgresql_connections_active` и `postgresql_connections_busy`

Драйвер Postgres внутри userver сообщает текущий размер пула соединений для каждой настроенной базы данных (метка `postgresql_database` отличает основную базу, `ya_brander_db_1`, от векторной базы, `vector_db`). "Active" (активные) это количество физических соединений, существующих в пуле прямо сейчас; "busy" (занятые) это сколько из них в данный момент выполняют запрос от имени одной из сопрограмм.

Что это значит: близка ли служба к исчерпанию соединений с базой данных. Если "busy" держится близко к "active" (или к настроенному пределу размера пула), запросы начинают вставать в очередь за свободным соединением, и стоит увеличить размер пула или проверить долгие запросы.

## 5. GC reclaimed orphans (total), собранные мусорным сборщиком сироты

Метрика источник: `gc_orphans_reclaimed_total` (метрика, добавленная вручную специально для этого проекта)
Тип: счётчик
Запрос: `gc_orphans_reclaimed_total`

Эта метрика не встроена в userver; она была зарегистрирована вручную в файле `garbage_collector_component.cpp` с помощью `utils::statistics::MetricTag<utils::statistics::RateCounter>`. Каждый раз, когда фоновая задача сборщика мусора запускается (`RunOnce`), она вызывает `VersionStorageComponent::CollectGarbage`, которая удаляет из S3 объекты сироты (версии, на которые больше никто не ссылается) и возвращает, сколько объектов было удалено. Это число сразу добавляется к счётчику вызовом `metrics_storage_.GetMetric(kGcOrphansReclaimedMetric).Add(...)`.

Сам сборщик мусора запускается только если переменная окружения `GC_ENABLED` установлена в `true`, и даже тогда только раз в настроенный интервал (по умолчанию каждые шесть часов), поэтому это число меняется редко и скачками, а не постоянно.

Что это значит: сколько объектов сирот в S3 собрал сборщик с момента запуска процесса. Панель показывает не скорость, а именно общее число, поскольку скорость для такого редкого и неравномерного процесса была бы обманчивой; общее число отвечает на более полезный вопрос: происходила ли очистка вообще.



***

# Grafana Dashboard

This document explains the five panels of the `ya_brander Overview` Grafana dashboard: where each metric comes from, how the numbers are calculated, and what they mean in practice.

## Pipeline

The backend service (userver) keeps counters and gauges in memory and exposes them at `GET /service/monitor?format=prometheus` on port 8085. The Prometheus container described in the Docker Compose configuration polls that address every 15 seconds and stores the values as a time series. Grafana then reads from Prometheus and draws the panels below. The service never pushes anything on its own; Prometheus always pulls the data.

## 1. Request rate (RPS)

Source metric: `http_handler_rps`
Type: counter (despite the name, it only grows; it counts every request a handler has ever received since the process started)
Query: `sum(rate(http_handler_rps[1m])) by (http_handler)`

userver increments this counter once per incoming request, per handler. Since a raw counter only climbs, Prometheus needs `rate()` to turn it into a speed: it looks at the last minute of samples and divides the difference by the elapsed time, giving requests per second. The `sum by (http_handler)` part collapses the extra labels userver attaches (`http_path`, `version`) so each handler appears as a single line instead of several duplicates.

What it means: how much traffic each endpoint is receiving right now. A flat line at zero means nobody called that handler in the last minute; a rising line means load is increasing.

## 2. Error rate (5xx)

Source metric: `http_handler_reply_codes`
Type: counter, one series per handler and per status code
Query: `sum(rate(http_handler_reply_codes{http_code=~"5.."}[1m])) by (http_handler)`

Every time a handler finishes a request, userver increments the counter tied to that particular status code. The filter `http_code=~"5.."` keeps only codes from 500 through 599, meaning server side errors. As above, `rate()` converts the counter into errors per second, and `sum by (http_handler)` merges every matching 5xx code into a single line per handler.

What it means: server side failures for each endpoint. Any value above zero deserves attention; a spike usually lines up with a deploy, a database problem, or a failing external API.

## 3. Latency p99

Source metric: `http_handler_timings`
Type: gauge, already computed as a percentile by userver
Query: `http_handler_timings{percentile="p99"}`

userver keeps a rolling window of recent request durations for each handler and periodically recalculates several percentiles on its own side (p50, p90, p95, p99 and so on), publishing each one as a separate gauge value tagged with a `percentile` label. Because the percentile is precomputed inside the service, Grafana simply reads the `p99` value directly; no `rate()` is needed since this is not a counter.

What it means: 99 percent of requests to that handler finish faster than the plotted value (in milliseconds). This is a much better signal than an average, since it shows the experience of the slowest real users instead of being hidden behind a majority of fast requests.

## 4. Postgres pool (active / busy)

Source metrics: `postgresql_connections_active`, `postgresql_connections_busy`
Type: gauges
Queries: `postgresql_connections_active` and `postgresql_connections_busy`

The Postgres driver inside userver reports the current size of its connection pool for each configured database (the `postgresql_database` label tells apart the main database, `ya_brander_db_1`, and the vector database, `vector_db`). "Active" is how many physical connections currently exist in the pool; "busy" is how many of those are executing a query for a coroutine at that exact moment.

What it means: whether the service is close to running out of database connections. If "busy" stays close to "active" (or to the configured pool size limit), requests are starting to queue for a free connection, and it may be time to raise the pool size or look into slow queries.

## 5. GC reclaimed orphans (total)

Source metric: `gc_orphans_reclaimed_total` (a custom metric added for this project)
Type: counter
Query: `gc_orphans_reclaimed_total`

This metric is not built into userver; it was registered by hand in `garbage_collector_component.cpp` using `utils::statistics::MetricTag<utils::statistics::RateCounter>`. Every time the background garbage collector task runs (`RunOnce`), it calls `VersionStorageComponent::CollectGarbage`, which deletes orphaned objects from S3 (versions nothing points to anymore) and returns how many it removed. That number is added to the counter right away through `metrics_storage_.GetMetric(kGcOrphansReclaimedMetric).Add(...)`.

The collector itself only runs when the `GC_ENABLED` environment variable is set to `true`, and even then only once every configured interval (six hours by default), so this number changes rarely and in occasional jumps rather than continuously.

What it means: how many orphaned S3 objects the collector has cleaned up since the process started. The panel shows the raw total rather than a rate, since a rate over such a slow and bursty process would be misleading; the total answers the more useful question of whether cleanup has been happening at all.