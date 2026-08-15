package com.yandex.avatargenerator.feature.auth.data

import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Профиль вошедшего пользователя между запусками приложения.
 *
 * Живёт отдельно от токена: токен нужен `HttpClient`, поэтому лежит в
 * `core/network/AuthTokenStore`, а id и никнейм — данные фичи `auth`, и `core/`
 * про них знать не должен. Шифрование здесь не нужно: это не секрет доступа, а
 * идентификаторы, которые и так видны в любом ответе API.
 *
 * Это не Repo API (он в `domain/`), а внутренний шов `data/`: за интерфейсом —
 * чтобы тест репозитория подставлял хранилище в памяти, как это уже сделано для
 * токена (`InMemoryAuthTokenStore`).
 */
interface AuthUserStore {

    /**
     * Сохранённый пользователь. `null` — либо ещё не прочитали, либо сессия
     * заведена до появления этого хранилища (см. `API.md`, «токен есть,
     * пользователя нет»).
     */
    val user: Flow<AuthUser?>

    suspend fun save(user: AuthUser)

    suspend fun clear()
}
