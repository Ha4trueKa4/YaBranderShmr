import pytest


# =====================================================================
# Фикстуры
# =====================================================================
@pytest.fixture
async def user_a(service_client):
    response = await service_client.post(
        "/api/v1/auth/register",
        json={
            "email": "anna@example.com",
            "password": "StrongPassword123!",
            "nickname": "designer_anna",
        },
    )
    return response.json()


@pytest.fixture
async def user_b(service_client):
    response = await service_client.post(
        "/api/v1/auth/register",
        json={
            "email": "ivan@example.com",
            "password": "StrongPassword123!",
            "nickname": "startup_ivan",
        },
    )
    return response.json()


@pytest.fixture
def auth_header_a(user_a):
    return {"Authorization": f"Bearer {user_a['access_token']}"}


@pytest.fixture
def auth_header_b(user_b):
    return {"Authorization": f"Bearer {user_b['access_token']}"}


@pytest.fixture
async def project_a(service_client, auth_header_a):
    """Создаёт проект для первого пользователя"""
    response = await service_client.post(
        "/api/v1/projects",
        json={"name": "Бренд кофейни", "description": "Минималистичный стиль"},
        headers=auth_header_a,
    )
    return response.json()


# ===================================================
# Создание проекта с авторизацией и без, 2 сценария
# ===================================================
async def test_create_project_success(
        service_client, auth_header_a, user_a
):
    """Успешное создание проекта с авторизацией -> 201"""
    project_desc = {
        "name": "Бренд кофейни",
        "description": "Минималистичный стиль, темные тона",
    }
    response = await service_client.post(
        "/api/v1/projects",
        json=project_desc,
        headers=auth_header_a,
    )

    assert response.status == 201
    data = response.json()
    assert "id" in data
    assert data["name"] == project_desc["name"]
    assert data["is_public"] is False

    assert "url" in data
    assert isinstance(data["url"], str)


async def test_create_project_unauthorized(service_client):
    """Создание без авторизации -> 401"""
    project_desc = {
        "name": "Бренд кофейни",
        "description": (
            "Минималистичный стиль, темные тона, для спешелти кофейни"
        ),
    }
    response = await service_client.post(
        "/api/v1/projects",
        json=project_desc,
    )
    assert response.status == 401


# ===================================================
# Получение проекта 3 сценария
# ===================================================
async def test_get_project_owner(
        service_client, project_a, auth_header_a, user_a
):
    """Владелец может получить свой проект -> 200"""
    project_id = project_a["id"]
    response = await service_client.get(
        f"/api/v1/projects/{project_id}",
        headers=auth_header_a,
    )

    assert response.status == 200
    data = response.json()
    assert data["id"] == project_id
    assert data["author_id"] == user_a["user"]["id"]
    assert data["name"] == "Бренд кофейни"
    assert "url" in data


async def test_get_project_forbidden_for_other(
        service_client, project_a, auth_header_b
):
    """Чужой приватный проект недоступен другому пользователю -> 403"""
    project_id = project_a["id"]
    response = await service_client.get(
        f"/api/v1/projects/{project_id}",
        headers=auth_header_b,
    )

    assert response.status == 403


async def test_get_project_not_found(service_client, auth_header_a):
    """Запрос несуществующего проекта -> 404"""
    fake_id = "00000000-0000-0000-0000-000000000000"
    response = await service_client.get(
        f"/api/v1/projects/{fake_id}",
        headers=auth_header_a,
    )
    assert response.status == 404


# ===================================================
# Обновляем данные проекта 2 сценария
# ===================================================
async def test_patch_project_success(
        service_client, project_a, auth_header_a
):
    """Владелец обновляет проект (сохраняет SVG) -> 200"""
    project_id = project_a["id"]

    new_svg = (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">'
        '<circle cx="250" cy="250" r="100"/></svg>'
    )
    project_update = {"is_public": True, "data": new_svg}

    patch_resp = await service_client.patch(
        f"/api/v1/projects/{project_id}",
        json=project_update,
        headers=auth_header_a,
    )
    assert patch_resp.status == 200

    get_resp = await service_client.get(
        f"/api/v1/projects/{project_id}",
        headers=auth_header_a,
    )
    updated_data = get_resp.json()

    assert updated_data["is_public"] is True
    assert "url" in updated_data


async def test_patch_project_forbidden(
        service_client, project_a, auth_header_b
):
    """Попытка обновить чужой проект -> 403"""
    project_id = project_a["id"]
    response = await service_client.patch(
        f"/api/v1/projects/{project_id}",
        json={"is_public": True},
        headers=auth_header_b,
    )
    assert response.status == 403


# ===================================================
# Удаление проекта 2 сценария
# ===================================================
async def test_delete_project_success(
        service_client, project_a, auth_header_a
):
    """Владелец успешно удаляет проект -> 200"""
    project_id = project_a["id"]

    del_resp = await service_client.delete(
        f"/api/v1/projects/{project_id}",
        headers=auth_header_a,
    )
    assert del_resp.status == 200

    get_resp = await service_client.get(
        f"/api/v1/projects/{project_id}",
        headers=auth_header_a,
    )
    assert get_resp.status == 404


async def test_delete_project_forbidden(
        service_client, project_a, auth_header_b
):
    """Попытка удалить чужой проект -> 403"""
    project_id = project_a["id"]
    response = await service_client.delete(
        f"/api/v1/projects/{project_id}",
        headers=auth_header_b,
    )
    assert response.status == 403


# ===================================================
# Генерация ИИ 2 сценария
# ===================================================
async def test_generate_ai_success(
        service_client, project_a, auth_header_a
):
    """Успешная генерация ИИ владельцем -> 200"""
    project_id = project_a["id"]
    prompt = {"prompt": "Добавь логотип с изображением кружки"}

    response = await service_client.post(
        f"/api/v1/projects/{project_id}/generate",
        json=prompt,
        headers=auth_header_a,
    )

    assert response.status == 200
    data = response.json()

    assert "url" in data
    assert isinstance(data["url"], str)


async def test_generate_ai_forbidden(
        service_client, project_a, auth_header_b
):
    """Попытка сгенерировать ИИ в чужом проекте -> 403"""
    project_id = project_a["id"]
    payload = {"prompt": "Сделай всё красным"}

    response = await service_client.post(
        f"/api/v1/projects/{project_id}/generate",
        json=payload,
        headers=auth_header_b,
    )

    assert response.status == 403
