import pathlib
import pytest

from testsuite.databases.pgsql import discover

pytest_plugins = ['pytest_userver.plugins.postgresql']


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
    raise AttributeError(f"Не удалось получить строку подключения из {conn_info!r}")


@pytest.fixture(scope='session')
def userver_pg_config(pgsql_local):
    def _patch_config(config_yaml, config_vars):
        conn_str = _get_connection_string(pgsql_local['db-1'])
        config_vars['dbconnection'] = conn_str
        config_vars['dbconnection-vector'] = conn_str

    return _patch_config


@pytest.fixture(autouse=True)
def mock_s3(mockserver):
    """Глобальный мок для Yandex S3. Перехватывает запросы от S3ObjectStorageComponent"""

    # Используем чистую строку без 'r' и '^', так как prefix=True
    @mockserver.handler('/ya-brander-svg/', prefix=True)
    def _mock_s3_handler(request):
        if request.method == 'PUT':
            return mockserver.make_response(status=200)
        elif request.method == 'DELETE':
            return mockserver.make_response(status=204)
        elif request.method == 'GET':
            return mockserver.make_response('<svg>mocked from s3</svg>', status=200)
        return mockserver.make_response(status=405)

    return _mock_s3_handler


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
