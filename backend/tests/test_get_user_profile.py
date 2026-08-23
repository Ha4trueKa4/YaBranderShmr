import uuid
import pytest


def make_user(prefix: str) -> dict:
    unique = uuid.uuid4().hex[:8]
    return {
        "email": f"{prefix}_{unique}@example.com",
        "password": "StrongPassword123!",
        "nickname": f"{prefix}_{unique}",
    }


async def register(service_client, prefix: str) -> dict:
    """Регистрирует пользователя и возвращает его id и токен"""
    response = await service_client.post(
        "/api/v1/auth/register", json=make_user(prefix)
    )
    assert response.status == 201
    data = response.json()
    return {
        "id": data["user"]["id"],
        "token": data["access_token"],
        "nickname": data["user"]["nickname"],
    }


def auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


async def create_project(
        service_client, token: str, name: str, is_public: bool = False
) -> str:
    """Создает проект и делает его публичным при необходимости"""
    resp = await service_client.post(
        "/api/v1/projects",
        json={"name": name, "description": "Test description"},
        headers=auth_headers(token),
    )
    assert resp.status == 201
    proj_id = resp.json()["id"]

    if is_public:
        patch_resp = await service_client.patch(
            f"/api/v1/projects/{proj_id}",
            json={"is_public": True, "data": "<svg></svg>"},
            headers=auth_headers(token),
        )
        assert patch_resp.status == 200

    return proj_id


# =====================================================================
# Тесты ручки GET /api/v1/users/{id}
# =====================================================================


async def test_get_profile_counters(service_client):
    """Проверка правильности счетчиков подписок и подписчиков -> 200"""
    target = await register(service_client, "target")
    follower_1 = await register(service_client, "follower_1")
    follower_2 = await register(service_client, "follower_2")
    following_1 = await register(service_client, "following_1")

    await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(follower_1["token"]),
    )
    await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(follower_2["token"]),
    )

    await service_client.post(
        f"/api/v1/users/{following_1['id']}/subscribe",
        headers=auth_headers(target["token"]),
    )

    response = await service_client.get(
        f"/api/v1/users/{target['id']}",
        headers=auth_headers(target["token"])
    )

    assert response.status == 200
    data = response.json()

    assert data["id"] == target["id"]
    assert data["name"] == target["nickname"]
    assert data["subscribers_count"] == 2
    assert data["subscriptions_count"] == 1


async def test_get_profile_projects_visibility_for_owner(service_client):
    """Владелец профиля видит и публичные, и приватные проекты -> 200"""
    owner = await register(service_client, "owner")

    await create_project(
        service_client, owner["token"], "Private Project", is_public=False
    )
    await create_project(
        service_client, owner["token"], "Public Project", is_public=True
    )

    response = await service_client.get(
        f"/api/v1/users/{owner['id']}", headers=auth_headers(owner["token"])
    )

    assert response.status == 200
    data = response.json()

    assert "projects" in data
    assert len(data["projects"]) == 2

    project_names = []
    for proj in data["projects"]:
        project_names.append(proj["name"])

        # Проверяем обязательные поля из Swagger'а
        assert "id" in proj
        assert "author_id" in proj
        assert "name" in proj
        assert "description" in proj
        assert "is_public" in proj
        assert "created_at" in proj

        # Проверяем опциональное поле url (появляется, если есть s3-версия)
        if "url" in proj:
            assert isinstance(proj["url"], str)
            assert len(proj["url"]) > 0

    assert "Private Project" in project_names
    assert "Public Project" in project_names


async def test_get_profile_projects_visibility_for_guest(service_client):
    """Гость видит только публичные проекты профиля со всеми полями -> 200"""
    owner = await register(service_client, "owner")
    guest = await register(service_client, "guest")

    await create_project(
        service_client, owner["token"], "Private Project", is_public=False
    )
    await create_project(
        service_client, owner["token"], "Public Project", is_public=True
    )

    response = await service_client.get(
        f"/api/v1/users/{owner['id']}", headers=auth_headers(guest["token"])
    )

    assert response.status == 200
    data = response.json()

    assert "projects" in data
    assert len(data["projects"]) == 1

    proj = data["projects"][0]
    assert proj["name"] == "Public Project"
    assert proj["is_public"] is True

    # Структурные проверки
    assert "id" in proj
    assert "author_id" in proj
    assert "description" in proj
    assert "created_at" in proj
    if "url" in proj:
        assert isinstance(proj["url"], str)


async def test_get_profile_not_found(service_client):
    """Запрос несуществующего пользователя -> 404"""
    user = await register(service_client, "user")
    fake_id = str(uuid.uuid4())

    response = await service_client.get(
        f"/api/v1/users/{fake_id}", headers=auth_headers(user["token"])
    )

    assert response.status == 404
    data = response.json()
    assert data["code"] == "USER_NOT_FOUND"


async def test_get_profile_invalid_uuid(service_client):
    """Запрос с невалидным UUID -> 400"""
    user = await register(service_client, "user")

    response = await service_client.get(
        "/api/v1/users/not-a-valid-uuid-123",
        headers=auth_headers(user["token"]),
    )

    assert response.status == 400
    data = response.json()
    assert data["code"] == "INVALID_UUID"


async def test_get_profile_counters_after_unsubscribe(service_client):
    """Проверка пересчета счетчиков после отписки -> 200"""
    target = await register(service_client, "target_unsub")
    follower = await register(service_client, "follower_unsub")

    await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(follower["token"]),
    )

    resp_target_before = await service_client.get(
        f"/api/v1/users/{target['id']}", headers=auth_headers(target["token"])
    )
    assert resp_target_before.json()["subscribers_count"] == 1

    resp_follower_before = await service_client.get(
        f"/api/v1/users/{follower['id']}",
        headers=auth_headers(follower["token"]),
    )
    assert resp_follower_before.json()["subscriptions_count"] == 1

    await service_client.delete(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(follower["token"]),
    )

    resp_target_after = await service_client.get(
        f"/api/v1/users/{target['id']}", headers=auth_headers(target["token"])
    )
    assert resp_target_after.status == 200
    assert resp_target_after.json()["subscribers_count"] == 0

    resp_follower_after = await service_client.get(
        f"/api/v1/users/{follower['id']}",
        headers=auth_headers(follower["token"]),
    )
    assert resp_follower_after.status == 200
    assert resp_follower_after.json()["subscriptions_count"] == 0


async def test_get_profile_is_subscribed_field(service_client):
    """Проверка опционального поля is_subscribed при разных условиях -> 200"""
    target = await register(service_client, "target_field")
    subscriber = await register(service_client, "sub_field")
    stranger = await register(service_client, "stranger_field")

    await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )

    resp_self = await service_client.get(
        f"/api/v1/users/{target['id']}",
        headers=auth_headers(target["token"]),
    )
    assert resp_self.status == 200
    assert "is_subscribed" not in resp_self.json()

    resp_sub = await service_client.get(
        f"/api/v1/users/{target['id']}",
        headers=auth_headers(subscriber["token"]),
    )
    assert resp_sub.status == 200
    assert resp_sub.json().get("is_subscribed") is True

    resp_stranger = await service_client.get(
        f"/api/v1/users/{target['id']}",
        headers=auth_headers(stranger["token"]),
    )
    assert resp_stranger.status == 200
    assert resp_stranger.json().get("is_subscribed") is False

    resp_unauth = await service_client.get(f"/api/v1/users/{target['id']}")
    assert resp_unauth.status == 200
    assert "is_subscribed" not in resp_unauth.json()
