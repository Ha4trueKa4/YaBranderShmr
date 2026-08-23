import json
import pathlib
import pytest

from testsuite.databases.pgsql import discover

pytest_plugins = [
    'pytest_userver.plugins.postgresql',
    'pytest_userver.plugins.redis',
]


@pytest.fixture(scope='session')
def service_env(redis_sentinels) -> dict:
    """Передаёт тестовому сервису свой SECDIST_CONFIG поверх унаследованного
    из окружения контейнера (там лежит секдист для локального dev-Redis без
    Sentinel — testsuite ждёт именно Sentinel-протокол, см. redis_sentinels).
    У userver переменная окружения (environment-secrets-key) имеет приоритет
    над файловым секдистом, поэтому патчить статик-конфиг через
    service_secdist_path бесполезно — секдист нужно передавать сюда."""
    secdist = {
        'redis_settings': {
            'ya_brander_jobs': {
                'password': '',
                'sentinels': [
                    {'host': redis_sentinels[0]['host'], 'port': redis_sentinels[0]['port']},
                ],
                # Тестовый Sentinel (testsuite/databases/redis) мониторит мастеров
                # под именами test_master0/test_master1 (см. redis_sentinel.conf.tpl) —
                # имя шарда здесь ОБЯЗАНО совпадать, иначе SENTINEL get-master-addr-by-name
                # не находит мастера и Redis-компонент сервиса зависает в переподключениях.
                'shards': [{'name': 'test_master0'}],
            },
        },
    }
    return {'SECDIST_CONFIG': json.dumps(secdist)}


@pytest.fixture(scope='session')
def service_source_dir():
    return pathlib.Path(__file__).parent.parent


@pytest.fixture(scope='session')
def initial_data_path(service_source_dir):
    return [service_source_dir / 'postgresql/data']


@pytest.fixture(scope='session')
def pgsql_local(service_source_dir, pgsql_local_create):
    databases = discover.find_schemas(
        'postgres-db-1',
        [service_source_dir.joinpath('postgresql/schemas')],
    )
    db1_only = {k: v for k, v in databases.items() if k == 'db-1'}
    return pgsql_local_create(list(db1_only.values()))


def _get_connection_string(conn_info):
    for attr in ('connection_string', 'dsn', 'uri'):
        if hasattr(conn_info, attr):
            value = getattr(conn_info, attr)
            if isinstance(value, str):
                return value
    for method_name in ('get_connection_string', 'get_dsn', 'get_uri'):
        if hasattr(conn_info, method_name):
            method = getattr(conn_info, method_name)
            if callable(method):
                try:
                    result = method()
                    if isinstance(result, str):
                        return result
                except TypeError:
                    pass
    try:
        return str(conn_info)
    except Exception:
        pass
    raise AttributeError(
        f"Не удалось получить строку подключения из {conn_info!r}")


@pytest.fixture(scope='session')
def userver_pg_config(pgsql_local):
    def _patch_config(config_yaml, config_vars):
        conn_str = _get_connection_string(pgsql_local['db-1'])
        config_vars['dbconnection'] = conn_str
        # dbconnection-vector нарочно НЕ переопределяем: у pgsql_local нет
        # pgvector (локальный тестовый Postgres его не поддерживает), а
        # значение по умолчанию из config_vars.testing.yaml указывает на
        # настоящий service-vector-postgres, который docker-compose уже
        # поднимает и мигрирует (migrate-vector) до старта тестов.

    return _patch_config


@pytest.fixture(autouse=True)
def mock_s3(mockserver):
    """Глобальный мок для Yandex S3. Перехватывает запросы от
    S3ObjectStorageComponent.

    Хранит объекты в памяти по ключу (путь после префикса бакета), а не
    отдаёт фиксированную строку на любой GET: иначе GenerateAI никогда не
    видит реальный kDefaultSvg только что созданного проекта и всегда идёт
    по edit-ветке промпта, даже для первой генерации.
    """
    prefix = '/ya-brander-svg/'
    storage: dict[str, bytes] = {}

    # Используем чистую строку без 'r' и '^', так как prefix=True
    @mockserver.handler(prefix, prefix=True)
    def _mock_s3_handler(request):
        key = request.path[len(prefix):]
        if request.method == 'PUT':
            # PUT ...?acl (смена ACL версии) не несёт тела и не должен
            # затирать уже сохранённые данные объекта.
            if 'acl' not in request.query:
                storage[key] = request.get_data()
            return mockserver.make_response(status=200)
        elif request.method == 'DELETE':
            storage.pop(key, None)
            return mockserver.make_response(status=204)
        elif request.method == 'GET':
            data = storage.get(key)
            if data is None:
                return mockserver.make_response(status=404)
            return mockserver.make_response(data, status=200)
        return mockserver.make_response(status=405)

    return _mock_s3_handler


@pytest.fixture(autouse=True)
def mock_yandex_embedding(mockserver):
    """Фейковый сервер Yandex-эмбеддингов (нужен VectorSearchComponent)"""

    @mockserver.json_handler("/yandex-embedding-mock")
    def _mock_embedding(request):
        return {"embedding": [0.0] * 256}

    return _mock_embedding


@pytest.fixture(autouse=True)
def mock_yandex_gpt(mockserver):
    """Фейковый сервер Yandex GPT"""

    @mockserver.json_handler("/yandex-gpt-mock")
    def _mock_generate(request):
        return {
            "output": [
                {"content": [{"text": '<svg><circle/></svg>'}]}
            ]
        }

    return _mock_generate
