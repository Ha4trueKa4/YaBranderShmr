package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.PublicProject

/**
 * Источник данных экрана публичного проекта. Интерфейс в `domain/`, реализации —
 * в `data/` (пока только [com.yandex.avatargenerator.feature.project.data
 * .MockPublicProjectRepository]); сетевая реализация добавится без изменения UI,
 * когда бэкенд отдаст автора и подборку работ.
 *
 * Методы возвращают [Result] и не бросают (кроме отмены корутины): разбор
 * успеха/ошибки — на стороне ViewModel.
 */
interface PublicProjectRepository {

    /** @return публичный проект по [projectId] со всеми данными витрины. */
    suspend fun getPublicProject(projectId: String): Result<PublicProject>
}
