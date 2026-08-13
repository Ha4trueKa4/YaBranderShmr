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
    """Registers a fresh user and returns {"id": ..., "token": ...}."""
    response = await service_client.post(
        "/api/v1/auth/register", json=make_user(prefix)
    )
    assert response.status == 201
    data = response.json()
    return {"id": data["user"]["id"], "token": data["access_token"]}


def auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


# ==============================================================
# ПОДПИСКА
# СЦЕНАРИИ 1-5
# POST /users/{id}/subscribe
# ==============================================================
async def test_subscribe_success(service_client):
    """1. Успешная подписка -> 200"""
    subscriber = await register(service_client, "subscriber")
    target = await register(service_client, "target")

    response = await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )
    assert response.status == 200


async def test_subscribe_unauthorized(service_client):
    """2. Подписка без авторизации -> 401"""
    target = await register(service_client, "target")

    response = await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe"
    )
    assert response.status == 401


async def test_subscribe_to_self(service_client):
    """3. Подписка на самого себя -> 400"""
    user = await register(service_client, "self")

    response = await service_client.post(
        f"/api/v1/users/{user['id']}/subscribe",
        headers=auth_headers(user["token"]),
    )
    assert response.status == 400


async def test_subscribe_nonexistent_user(service_client):
    """4. Подписка на несуществующего пользователя -> 404"""
    subscriber = await register(service_client, "subscriber")
    fake_id = str(uuid.uuid4())

    response = await service_client.post(
        f"/api/v1/users/{fake_id}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )
    assert response.status == 404


async def test_subscribe_twice_is_idempotent(service_client):
    """5. Повторная подписка на того же пользователя -> 200 (без конфликта)"""
    subscriber = await register(service_client, "subscriber")
    target = await register(service_client, "target")

    first = await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )
    second = await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )
    assert first.status == 200
    assert second.status == 200


# ==============================================================
# ОТПИСКА
# СЦЕНАРИИ 6-8
# DELETE /users/{id}/subscribe
# ==============================================================
async def test_unsubscribe_success(service_client):
    """6. Успешная отписка -> 200"""
    subscriber = await register(service_client, "subscriber")
    target = await register(service_client, "target")

    await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )
    response = await service_client.delete(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )
    assert response.status == 200


async def test_unsubscribe_unauthorized(service_client):
    """7. Отписка без авторизации -> 401"""
    target = await register(service_client, "target")

    response = await service_client.delete(
        f"/api/v1/users/{target['id']}/subscribe"
    )
    assert response.status == 401


async def test_unsubscribe_when_not_subscribed(service_client):
    """8. Отписка от пользователя, на которого не подписан -> 200 (no-op)"""
    subscriber = await register(service_client, "subscriber")
    target = await register(service_client, "target")

    response = await service_client.delete(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )
    assert response.status == 200


# ==============================================================
# ПОЛУЧЕНИЕ ПОДПИСЧИКОВ
# СЦЕНАРИИ 9-12
# GET /users/{id}/subscribers
# ==============================================================
async def test_get_subscribers_success(service_client):
    """9. Получение списка подписчиков -> 200"""
    target = await register(service_client, "target")
    sub1 = await register(service_client, "sub1")
    sub2 = await register(service_client, "sub2")

    await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(sub1["token"]),
    )
    await service_client.post(
        f"/api/v1/users/{target['id']}/subscribe",
        headers=auth_headers(sub2["token"]),
    )

    response = await service_client.get(
        f"/api/v1/users/{target['id']}/subscribers",
        headers=auth_headers(target["token"]),
    )
    assert response.status == 200
    data = response.json()
    assert len(data) == 2

    subscriber_ids = [u["id"] for u in data]
    assert sub1["id"] in subscriber_ids
    assert sub2["id"] in subscriber_ids
    assert "name" in data[0]


async def test_get_subscribers_empty(service_client):
    """10. Пустой список подписчиков -> 200 и пустой массив"""
    target = await register(service_client, "target")

    response = await service_client.get(
        f"/api/v1/users/{target['id']}/subscribers",
        headers=auth_headers(target["token"]),
    )
    assert response.status == 200
    assert response.json() == []


async def test_get_subscribers_not_found(service_client):
    """11. Запрос подписчиков несуществующего юзера -> 404"""
    user = await register(service_client, "user")
    fake_id = str(uuid.uuid4())

    response = await service_client.get(
        f"/api/v1/users/{fake_id}/subscribers",
        headers=auth_headers(user["token"]),
    )
    assert response.status == 404


async def test_get_subscribers_invalid_uuid(service_client):
    """12. Запрос подписчиков с невалидным UUID -> 400"""
    user = await register(service_client, "user")

    response = await service_client.get(
        "/api/v1/users/bad-uuid/subscribers",
        headers=auth_headers(user["token"]),
    )
    assert response.status == 400


# ==============================================================
# ПОЛУЧЕНИЕ ПОДПИСОК
# СЦЕНАРИИ 13-16
# GET /users/{id}/subscriptions
# ==============================================================
async def test_get_subscriptions_success(service_client):
    """13. Получение списка подписок (на кого подписан) -> 200"""
    subscriber = await register(service_client, "subscriber")
    target1 = await register(service_client, "target1")
    target2 = await register(service_client, "target2")

    await service_client.post(
        f"/api/v1/users/{target1['id']}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )
    await service_client.post(
        f"/api/v1/users/{target2['id']}/subscribe",
        headers=auth_headers(subscriber["token"]),
    )

    response = await service_client.get(
        f"/api/v1/users/{subscriber['id']}/subscriptions",
        headers=auth_headers(subscriber["token"]),
    )
    assert response.status == 200
    data = response.json()
    assert len(data) == 2

    subscription_ids = [u["id"] for u in data]
    assert target1["id"] in subscription_ids
    assert target2["id"] in subscription_ids
    assert "name" in data[0]


async def test_get_subscriptions_empty(service_client):
    """14. Пустой список подписок -> 200 и пустой массив"""
    subscriber = await register(service_client, "subscriber")

    response = await service_client.get(
        f"/api/v1/users/{subscriber['id']}/subscriptions",
        headers=auth_headers(subscriber["token"]),
    )
    assert response.status == 200
    assert response.json() == []


async def test_get_subscriptions_not_found(service_client):
    """15. Запрос подписок несуществующего юзера -> 404"""
    user = await register(service_client, "user")
    fake_id = str(uuid.uuid4())

    response = await service_client.get(
        f"/api/v1/users/{fake_id}/subscriptions",
        headers=auth_headers(user["token"]),
    )
    assert response.status == 404


async def test_get_subscriptions_invalid_uuid(service_client):
    """16. Запрос подписок с невалидным UUID -> 400"""
    user = await register(service_client, "user")

    response = await service_client.get(
        "/api/v1/users/bad-uuid/subscriptions",
        headers=auth_headers(user["token"]),
    )
    assert response.status == 400
