package com.yandex.avatargenerator.feature.create.fake

import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import com.yandex.avatargenerator.feature.create.domain.model.Chat

/**
 * Репозиторий-подделка для тестов ViewModel.
 *
 * @param result что вернуть на очередной вызов — успех или ошибка;
 *  поле `var`, чтобы в тесте retry подменить ошибку успехом.
 */
internal class FakeChatsRepository(
    var result: Result<List<Chat>> = Result.success(emptyList()),
) : ChatsRepository {

    var callCount: Int = 0
        private set

    override suspend fun getChats(): Result<List<Chat>> {
        callCount++
        return result
    }
}
