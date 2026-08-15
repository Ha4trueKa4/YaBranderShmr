package com.yandex.avatargenerator.feature.createproject.domain

/**
 * Создание нового проекта. Интерфейс живёт в `domain/`, реализации — в `data/`
 * (пока только [com.yandex.avatargenerator.feature.createproject.data.MockCreateProjectRepository]);
 * при готовности бэкенда добавится сетевая реализация (`POST /projects` +
 * `POST /projects/{id}/generate`) без изменения UI.
 */
interface CreateProjectRepository {

    /**
     * Создаёт проект по названию и описанию и запускает первичную генерацию.
     *
     * @return id созданного проекта — он же projectId экрана проекта, куда
     *  экран переходит после успешного создания.
     */
    suspend fun createProject(name: String, description: String): String
}
