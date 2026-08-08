import pytest

from testsuite.databases import pgsql


# Start the tests via `make test-debug` or `make test-release`

@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_common_item_success(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '1', 'item_id': 'common_1'},
    )
    assert response.status == 200
    assert response.json() == []


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_prescription_with_recipe(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '1', 'item_id': 'prescription_62'},
    )
    assert response.status == 200
    assert response.json() == []


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_prescription_without_recipe(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '1', 'item_id': 'prescription_1'},
    )
    assert response.status == 200
    assert response.json() == [{"item_id": "prescription_1", "problem": "NO_PRESCRIPTION"}]


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_doctor_special_item_correct(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '61', 'item_id': 'special_16'},
    )
    assert response.status == 200
    assert response.json() == []


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_doctor_special_item_wrong_specialty(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '61', 'item_id': 'special_1'},
    )
    assert response.status == 200
    assert response.json() == [{"item_id": "special_1", "problem": "ITEM_SPECIAL_WRONG_SPECIFIC"}]


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_wrong_category(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '1', 'item_id': 'unknown_100'},
    )
    assert response.status == 200
    assert response.json() == [{"item_id": "unknown_100", "problem": "WRONG_CATEGORY"}]


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_incorrect_item_id(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '1', 'item_id': 'common_abc'},
    )
    assert response.status == 200
    assert response.json() == [{"item_id": "common_abc", "problem": "INCORRECT_ITEM_ID"}]


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_item_not_found(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '1', 'item_id': 'common_9999'},
    )
    assert response.status == 200
    assert response.json() == [{"item_id": "common_9999", "problem": "ITEM_NOT_FOUND"}]


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_no_user_common(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '999', 'item_id': 'common_1'},
    )
    assert response.status == 200
    assert response.json() == [{"item_id": "common_1", "problem": "NO_USER"}]


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_no_user_prescription(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '999', 'item_id': 'prescription_1'},
    )
    assert response.status == 200
    assert response.json() == [{"item_id": "prescription_1", "problem": "NO_USER_NO_PRESCRIPTION"}]


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_no_user_special(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '999', 'item_id': 'special_1'},
    )
    assert response.status == 200
    assert response.json() == [{"item_id": "special_1", "problem": "NO_USER_SPECIAL_ITEM"}]


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_regular_user_special_item(service_client):
    response = await service_client.get(
        '/check',
        params={'user_id': '1', 'item_id': 'special_1'},
    )
    assert response.status == 200
    assert response.json() == [{"item_id": "special_1", "problem": "ITEM_IS_SPECIAL"}]


@pytest.mark.pgsql('db-1', files=['initial_data.sql'])
async def test_mixed_items(service_client):
    response = await service_client.get(
        '/check',
        params={
            'user_id': '1',
            'item_id': ['common_1', 'special_1', 'unknown_999'],
        },
    )
    assert response.status == 200
    json = response.json()
    assert len(json) == 2
    problems = {item['item_id']: item['problem'] for item in json}
    assert problems['special_1'] == 'ITEM_IS_SPECIAL'
    assert problems['unknown_999'] == 'WRONG_CATEGORY'