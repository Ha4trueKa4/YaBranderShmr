package com.yandex.avatargenerator.feature.auth.data

import com.yandex.avatargenerator.feature.auth.data.dto.AuthResponseDto
import com.yandex.avatargenerator.feature.auth.domain.model.AuthSession
import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser

/** DTO выше `data/` не поднимаются. */
internal fun AuthResponseDto.toDomain(): AuthSession = AuthSession(
    accessToken = accessToken,
    tokenType = tokenType,
    user = AuthUser(
        id = user.id,
        email = user.email,
        nickname = user.nickname,
    ),
)
