import pytest

VALID_USER = {
    "email": "testuser@example.com",
    "password": "StrongPassword123!",
    "nickname": "TestUser",
}


# ==============================================================
# ТЕСТЫ РЕГИСТРАЦИИ
# СЦЕНАРИИ 1-6
# /auth/register
# ==============================================================
async def test_register_success(service_client):
    """1. Успешная регистрация -> 201"""
    response = await service_client.post(
        "/api/v1/auth/register", json=VALID_USER
    )
    assert response.status == 201
    data = response.json()

    assert "access_token" in data
    assert data.get("token_type") == "Bearer"
    assert "user" in data
    assert "id" in data["user"]
    assert data["user"]["email"] == VALID_USER["email"]
    assert data["user"]["nickname"] == VALID_USER["nickname"]


async def test_register_duplicate_email(service_client):
    """2. Повторная регистрация с тем же email -> 409 EMAIL_EXISTS"""
    await service_client.post("/api/v1/auth/register", json=VALID_USER)

    duplicate_email_user = VALID_USER.copy()
    duplicate_email_user["nickname"] = "AnotherNick"

    response = await service_client.post(
        "/api/v1/auth/register", json=duplicate_email_user
    )

    assert response.status == 409
    data = response.json()
    assert data.get("code") == "EMAIL_EXISTS"
    assert data.get("field") == "email"


async def test_register_duplicate_nickname(service_client):
    """3. Повторная регистрация с тем же nickname -> 409 NICKNAME_EXISTS"""
    await service_client.post("/api/v1/auth/register", json=VALID_USER)

    duplicate_nick_user = VALID_USER.copy()
    duplicate_nick_user["email"] = "another@example.com"

    response = await service_client.post(
        "/api/v1/auth/register", json=duplicate_nick_user
    )

    assert response.status == 409
    data = response.json()
    assert data.get("code") == "NICKNAME_EXISTS"
    assert data.get("field") == "nickname"


async def test_register_invalid_email(service_client):
    """4. Невалидный email -> 400"""
    bad_user = VALID_USER.copy()
    bad_user["email"] = "not-an-email"

    response = await service_client.post(
        "/api/v1/auth/register", json=bad_user
    )
    assert response.status == 400
    assert response.json().get("code") == "INVALID_EMAIL"


async def test_register_weak_password(service_client):
    """5. Слабый пароль -> 400"""
    bad_user = VALID_USER.copy()
    bad_user["password"] = "weak"

    response = await service_client.post(
        "/api/v1/auth/register", json=bad_user
    )
    assert response.status == 400
    assert response.json().get("code") == "INVALID_PASSWORD"


async def test_register_short_nickname(service_client):
    """6. Слишком короткий nickname -> 400"""
    bad_user = VALID_USER.copy()
    bad_user["nickname"] = "ab"

    response = await service_client.post(
        "/api/v1/auth/register", json=bad_user
    )
    assert response.status == 400
    assert response.json().get("code") == "INVALID_NICKNAME"


# ==============================================================
# ВХОД В СИСТЕМУ
# СЦЕНАРИИ 7 - 10
# /auth/login
# ==============================================================
async def test_login_success(service_client):
    """7. Успешный логин -> 200"""
    await service_client.post("/api/v1/auth/register", json=VALID_USER)

    login_data = {
        "email": VALID_USER["email"],
        "password": VALID_USER["password"],
    }
    response = await service_client.post("/api/v1/auth/login", json=login_data)

    assert response.status == 200
    data = response.json()
    assert "access_token" in data
    assert data.get("token_type") == "Bearer"
    assert "user" in data
    assert "id" in data["user"]
    assert data["user"]["email"] == VALID_USER["email"]
    assert data["user"]["nickname"] == VALID_USER["nickname"]


async def test_login_wrong_password(service_client):
    """8. Неверный пароль -> 401"""
    await service_client.post("/api/v1/auth/register", json=VALID_USER)

    login_data = {
        "email": VALID_USER["email"],
        "password": "WrongPassword123!",
    }
    response = await service_client.post("/api/v1/auth/login", json=login_data)

    assert response.status == 401
    assert response.json().get("code") == "INVALID_CREDENTIALS"


async def test_login_nonexistent_email(service_client):
    """9. Неверный email -> 401"""
    await service_client.post("/api/v1/auth/register", json=VALID_USER)

    login_data = {
        "email": "ghost@example.com",
        "password": VALID_USER["password"],
    }
    response = await service_client.post("/api/v1/auth/login", json=login_data)

    assert response.status == 401
    assert response.json().get("code") == "INVALID_CREDENTIALS"


async def test_login_empty_password(service_client):
    """10. Пустой пароль -> 401"""
    login_data = {"email": VALID_USER["email"], "password": ""}
    response = await service_client.post("/api/v1/auth/login", json=login_data)

    assert response.status == 401
