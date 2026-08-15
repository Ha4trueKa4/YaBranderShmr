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

    response = await service_client.post(f"/api/v1/users/{target['id']}/subscribe")
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

    response = await service_client.delete(f"/api/v1/users/{target['id']}/subscribe")
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
