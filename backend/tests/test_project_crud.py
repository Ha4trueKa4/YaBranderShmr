import pytest


# =====================================================================
# Общие асинхронные хелперы (создание проекта и генерация теперь всегда
# асинхронны: 202 + job_id + long-poll GET .../generate/{job_id})
# =====================================================================
async def _poll_until_done(service_client, project_id, job_id, headers):
    """Поллинг GET .../generate/{job_id} до status != "pending".

    Возвращает финальный JSON опроса.
    """
    status = "pending"
    data = {}
    for _ in range(10):
        poll = await service_client.get(
            f"/api/v1/projects/{project_id}/generate/{job_id}",
            headers=headers,
        )
        assert poll.status == 200
        data = poll.json()
        status = data["status"]
        if status != "pending":
            break

    assert status == "done"
    return data


async def _generate_and_wait(service_client, project_id, headers, payload):
    """POST /generate (асинхронный job) + поллинг до done."""
    response = await service_client.post(
        f"/api/v1/projects/{project_id}/generate",
        json=payload,
        headers=headers,
    )
    assert response.status == 202
    job_id = response.json()["job_id"]
    return await _poll_until_done(service_client, project_id, job_id, headers)


async def _create_project_and_wait(service_client, headers, payload):
    """POST /projects (создание + асинхронная первичная генерация) + poll.

    Возвращает JSON ответа создания (id, author_id, name, description,
    is_public, created_at, url, job_id) — этого достаточно фикстурам/тестам,
    отдельный GET не нужен.
    """
    response = await service_client.post(
        "/api/v1/projects",
        json=payload,
        headers=headers,
    )
    assert response.status == 202
    data = response.json()
    await _poll_until_done(service_client, data["id"], data["job_id"], headers)
    return data


def _last_generation_call(mock_yandex_gpt):
    """Возвращает вызов основной генерации, пропустив вызов
    PromptPolisherComponent — он тоже бьёт в /yandex-gpt-mock (тот же
    api-url) перед реальной генерацией, поэтому на каждый успешный job
    в очереди мока оказывается ровно два вызова: сначала polish, потом
    сама генерация.
    """
    mock_yandex_gpt.next_call()  # PromptPolisherComponent.Polish*
    return mock_yandex_gpt.next_call()


def _drain_mock_calls(mock_yandex_gpt):
    """Сбрасывает все накопленные в очереди мока вызовы.

    Нужен тестам референсов, где перед проверяемым действием уже
    отработала фикстура-создатель проекта (project_a2/project_b) — она
    сама теперь запускает первичную генерацию (polish+generate), и эти
    вызовы остаются в очереди мока непрочитанными. Без очистки
    _last_generation_call() после проверяемого действия поймал бы именно
    эти чужие, более ранние вызовы вместо вызовов своего собственного
    создания проекта.
    """
    while mock_yandex_gpt.times_called:
        mock_yandex_gpt.next_call()


REFERENCE_SVG = (
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">'
    '<rect width="500" height="500"/></svg>'
)


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
    """Создаёт проект для первого пользователя (с первичной генерацией)"""
    return await _create_project_and_wait(
        service_client, auth_header_a,
        {
            "name": "Бренд кофейни",
            "description": "Минималистичный стиль",
            "prompt": "Нарисуй логотип кофейни",
        },
    )


@pytest.fixture
async def project_a2(service_client, auth_header_a):
    """Второй проект первого пользователя — для тестов референсов"""
    return await _create_project_and_wait(
        service_client, auth_header_a,
        {
            "name": "Второй проект",
            "description": "",
            "prompt": "Нарисуй что-нибудь простое",
        },
    )


@pytest.fixture
async def project_b(service_client, auth_header_b):
    """Проект второго пользователя — для тестов референсов"""
    return await _create_project_and_wait(
        service_client, auth_header_b,
        {
            "name": "Проект Ивана",
            "description": "",
            "prompt": "Нарисуй логотип стартапа",
        },
    )


# ===================================================
# Создание проекта: авторизация, обязательный prompt,
# опциональные name/description
# ===================================================
async def test_create_project_success(
        service_client, auth_header_a, user_a
):
    """Успешное создание проекта -> 202 + job_id, дальше poll до done"""
    project_desc = {
        "name": "Бренд кофейни",
        "description": "Минималистичный стиль, темные тона",
        "prompt": "Нарисуй логотип кофейни",
    }
    response = await service_client.post(
        "/api/v1/projects",
        json=project_desc,
        headers=auth_header_a,
    )

    assert response.status == 202
    data = response.json()
    assert "id" in data
    assert data["name"] == project_desc["name"]
    assert data["is_public"] is False
    assert "url" in data
    assert isinstance(data["url"], str)
    assert "job_id" in data

    final = await _poll_until_done(
        service_client, data["id"], data["job_id"], auth_header_a)
    assert isinstance(final["url"], str)


async def test_create_project_unauthorized(service_client):
    """Создание без авторизации -> 401"""
    project_desc = {
        "name": "Бренд кофейни",
        "description": (
            "Минималистичный стиль, темные тона, для спешелти кофейни"
        ),
        "prompt": "Нарисуй логотип",
    }
    response = await service_client.post(
        "/api/v1/projects",
        json=project_desc,
    )
    assert response.status == 401


async def test_create_project_prompt_required(
        service_client, auth_header_a
):
    """Без prompt создать проект нельзя (prompt обязателен) -> 400"""
    response = await service_client.post(
        "/api/v1/projects",
        json={"name": "Бренд кофейни"},
        headers=auth_header_a,
    )
    assert response.status == 400
    assert response.json()["message"] == "Prompt cannot be empty"


async def test_create_project_without_name_or_description(
        service_client, auth_header_a
):
    """name и description опциональны -> 202, создаётся с пустыми значениями"""
    response = await service_client.post(
        "/api/v1/projects",
        json={"prompt": "Нарисуй логотип"},
        headers=auth_header_a,
    )
    assert response.status == 202
    data = response.json()
    assert data["name"] == ""
    assert data["description"] == ""


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
# Генерация ИИ (правка) — 2 сценария
# ===================================================
async def test_generate_ai_edit_success(
        service_client, project_a, auth_header_a
):
    """Успешная правка владельцем -> 202 + job_id, дальше poll до done.

    project_a уже прошёл первичную генерацию при создании (см. фикстуру),
    поэтому этот вызов гарантированно бьёт по edit-ветке.
    """
    project_id = project_a["id"]
    prompt = {"prompt": "Добавь логотип с изображением кружки"}

    response = await service_client.post(
        f"/api/v1/projects/{project_id}/generate",
        json=prompt,
        headers=auth_header_a,
    )

    assert response.status == 202
    job_id = response.json()["job_id"]

    data = await _poll_until_done(
        service_client, project_id, job_id, auth_header_a)
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


# ===================================================
# История чата: prompt из POST /projects/POST /generate
# должен появляться в chat_history с правильными role/job_id,
# ответ ассистента — после завершения job'а
# ===================================================
async def test_chat_history_records_initial_generation(
        service_client, auth_header_a
):
    """prompt из POST /projects -> сразу в chat_history (role=user),
    после done -> ответ ассистента (role=assistant, version_id) с тем же job_id
    """
    create_resp = await service_client.post(
        "/api/v1/projects",
        json={"name": "Проект для чата", "prompt": "Нарисуй логотип кофейни"},
        headers=auth_header_a,
    )
    assert create_resp.status == 202
    data = create_resp.json()
    project_id = data["id"]
    job_id = data["job_id"]

    await _poll_until_done(service_client, project_id, job_id, auth_header_a)

    chat_resp = await service_client.get(
        f"/api/v1/projects/{project_id}/chat",
        headers=auth_header_a,
    )
    assert chat_resp.status == 200
    history = chat_resp.json()["chat_history"]

    assert len(history) == 2
    user_message, assistant_message = history

    assert user_message["role"] == "user"
    assert user_message["text"] == "Нарисуй логотип кофейни"
    assert user_message["job_id"] == job_id

    assert assistant_message["role"] == "assistant"
    assert assistant_message["job_id"] == job_id
    assert "version_id" in assistant_message
    assert "error" not in assistant_message


async def test_chat_history_records_edit_generation(
        service_client, project_a, auth_header_a
):
    """prompt из POST /generate добавляется в chat_history как второй обмен,
    не задваивая и не теряя первый (из первичной генерации project_a)
    """
    response = await service_client.post(
        f"/api/v1/projects/{project_a['id']}/generate",
        json={"prompt": "Сделай фон белым"},
        headers=auth_header_a,
    )
    assert response.status == 202
    edit_job_id = response.json()["job_id"]

    await _poll_until_done(
        service_client, project_a["id"], edit_job_id, auth_header_a)

    chat_resp = await service_client.get(
        f"/api/v1/projects/{project_a['id']}/chat",
        headers=auth_header_a,
    )
    assert chat_resp.status == 200
    history = chat_resp.json()["chat_history"]

    # 2 сообщения от первичной генерации (project_a) + 2 от этой правки
    assert len(history) == 4

    edit_user_message, edit_assistant_message = history[2], history[3]
    assert edit_user_message["role"] == "user"
    assert edit_user_message["text"] == "Сделай фон белым"
    assert edit_user_message["job_id"] == edit_job_id
    assert edit_assistant_message["role"] == "assistant"
    assert edit_assistant_message["job_id"] == edit_job_id
    assert "version_id" in edit_assistant_message


async def test_chat_history_forbidden_for_non_owner(
        service_client, project_a, auth_header_a, auth_header_b
):
    """История чата приватна даже для публичного проекта -> 403 не-автору"""
    patch_resp = await service_client.patch(
        f"/api/v1/projects/{project_a['id']}",
        json={"is_public": True},
        headers=auth_header_a,
    )
    assert patch_resp.status == 200

    response = await service_client.get(
        f"/api/v1/projects/{project_a['id']}/chat",
        headers=auth_header_b,
    )
    assert response.status == 403


async def test_chat_history_not_found(service_client, auth_header_a):
    """История чата несуществующего проекта -> 404"""
    fake_id = "00000000-0000-0000-0000-000000000000"
    response = await service_client.get(
        f"/api/v1/projects/{fake_id}/chat",
        headers=auth_header_a,
    )
    assert response.status == 404


# ===================================================
# Референсы (SVG других проектов) при первичной генерации —
# теперь принимаются только в POST /projects
# ===================================================
async def test_create_project_with_own_reference(
        service_client, project_a2, auth_header_a, mock_yandex_gpt
):
    """Референс на свой проект с реальным SVG -> попадает в промпт"""
    patch_resp = await service_client.patch(
        f"/api/v1/projects/{project_a2['id']}",
        json={"data": REFERENCE_SVG},
        headers=auth_header_a,
    )
    assert patch_resp.status == 200

    # project_a2 (фикстура) уже сам вызвал LLM при своей первичной генерации —
    # эти вызовы нужно убрать из очереди, иначе _last_generation_call ниже
    # поймает их вместо вызова для проекта, создаваемого этим тестом.
    _drain_mock_calls(mock_yandex_gpt)

    await _create_project_and_wait(
        service_client, auth_header_a,
        {
            "name": "Новый проект",
            "prompt": "Сделай логотип",
            "references": [project_a2["id"]],
        },
    )

    call = _last_generation_call(mock_yandex_gpt)
    assert "ПОЛЬЗОВАТЕЛЬСКИЙ РЕФЕРЕНС 1" in call["request"].json["input"]
    assert REFERENCE_SVG in call["request"].json["input"]


async def test_create_project_with_public_reference_from_other_user(
        service_client, project_b,
        auth_header_a, auth_header_b, mock_yandex_gpt
):
    """Референс на чужой ПУБЛИЧНЫЙ проект -> попадает в промпт"""
    patch_resp = await service_client.patch(
        f"/api/v1/projects/{project_b['id']}",
        json={"is_public": True, "data": REFERENCE_SVG},
        headers=auth_header_b,
    )
    assert patch_resp.status == 200

    # project_b (фикстура) уже сам вызвал LLM при своей первичной генерации —
    # убираем эти вызовы из очереди перед проверяемым действием (см.
    # test_create_project_with_own_reference).
    _drain_mock_calls(mock_yandex_gpt)

    await _create_project_and_wait(
        service_client, auth_header_a,
        {
            "name": "Новый проект",
            "prompt": "Сделай логотип",
            "references": [project_b["id"]],
        },
    )

    call = _last_generation_call(mock_yandex_gpt)
    assert "ПОЛЬЗОВАТЕЛЬСКИЙ РЕФЕРЕНС 1" in call["request"].json["input"]


async def test_create_project_reference_private_skipped(
        service_client, project_b,
        auth_header_a, auth_header_b, mock_yandex_gpt
):
    """Референс на чужой ПРИВАТНЫЙ проект -> молча пропускается, не ошибка"""
    patch_resp = await service_client.patch(
        f"/api/v1/projects/{project_b['id']}",
        json={"data": REFERENCE_SVG},
        headers=auth_header_b,
    )
    assert patch_resp.status == 200

    # project_b (фикстура) уже сам вызвал LLM при своей первичной генерации —
    # без очистки этот тест "проходил бы" по чужому вызову (project_b тоже
    # создавался без references), а не реально проверял бы пропуск
    # приватного референса. См. test_create_project_with_own_reference.
    _drain_mock_calls(mock_yandex_gpt)

    await _create_project_and_wait(
        service_client, auth_header_a,
        {
            "name": "Новый проект",
            "prompt": "Сделай логотип",
            "references": [project_b["id"]],
        },
    )

    call = _last_generation_call(mock_yandex_gpt)
    assert "ПОЛЬЗОВАТЕЛЬСКИЙ РЕФЕРЕНС" not in call["request"].json["input"]


async def test_create_project_reference_nonexistent_silently_skipped(
        service_client, auth_header_a, mock_yandex_gpt
):
    """Несуществующий id в references -> молча пропускается, не ошибка"""
    fake_id = "00000000-0000-0000-0000-000000000000"

    await _create_project_and_wait(
        service_client, auth_header_a,
        {
            "name": "Новый проект",
            "prompt": "Сделай логотип",
            "references": [fake_id],
        },
    )

    call = _last_generation_call(mock_yandex_gpt)
    assert "ПОЛЬЗОВАТЕЛЬСКИЙ РЕФЕРЕНС" not in call["request"].json["input"]


async def test_create_project_references_too_many_rejected(
        service_client, auth_header_a
):
    """Больше 2 id в references -> 400"""
    fake_ids = [
        "00000000-0000-0000-0000-000000000001",
        "00000000-0000-0000-0000-000000000002",
        "00000000-0000-0000-0000-000000000003",
    ]

    response = await service_client.post(
        "/api/v1/projects",
        json={
            "name": "Новый проект",
            "prompt": "Сделай логотип",
            "references": fake_ids,
        },
        headers=auth_header_a,
    )

    assert response.status == 400
    assert response.json()[
        "message"] == "references supports at most 2 project ids"
