package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.project.data.dto.ChatHistoryResponseDto
import com.yandex.avatargenerator.feature.project.data.dto.ChatMessageDto
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.VersionsRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private const val ProjectsPath = "projects"
private const val ChatSegment = "chat"

/**
 * Диалог с ИИ через API. Экран собирается из трёх параллельных запросов:
 *
 * - `GET /projects/{id}` — название, видимость и `url` последней версии;
 * - `GET /projects/{id}/chat` — сами сообщения;
 * - `GET /projects/{id}/versions` — нужен ровно для одного: узнать `id`
 *   последней версии, чтобы понять, к какому пузырю относится `url` из проекта.
 *
 * Проект и версии берём через готовые [ProjectRepository] и [VersionsRepository],
 * а не собираем свои запросы: пути и разбор ответов у них уже написаны и покрыты
 * тестами. Свой запрос здесь один — за историей.
 *
 * **Деградация вместо ошибки.** `Result.failure` возвращается только когда упал
 * `GET /projects/{id}` — без него у экрана нет даже заголовка. Ошибки истории и
 * версий (403 у не-автора, 404, 5xx) съедаем: лента получится пустой, но проект
 * откроется на чтение.
 */
class ChatRepositoryImpl(
    private val client: HttpClient,
    private val projectRepository: ProjectRepository,
    private val versionsRepository: VersionsRepository,
) : ChatRepository {

    override suspend fun getChat(projectId: String): Result<ProjectChat> = coroutineScope {
        val projectAsync = async { projectRepository.getProject(projectId) }
        val historyAsync = async { loadHistory(projectId) }
        val versionsAsync = async { versionsRepository.getVersions(projectId) }

        projectAsync.await().map { project ->
            ProjectChat(
                id = project.id,
                title = project.name,
                isPublic = project.isPublic,
                messages = buildChatTimeline(
                    history = historyAsync.await(),
                    // Сервер отдаёт версии по убыванию номера — последняя первая.
                    latestVersionId = versionsAsync.await().getOrNull()?.firstOrNull()?.id,
                    latestVersionUrl = project.url,
                ),
            )
        }
    }

    /**
     * Пока не реализовано: генерация приезжает следующим PR вместе с
     * `POST /projects/{id}/generate` и своим таймаутом на две минуты. До тех пор
     * отправка честно не удаётся — ViewModel вернёт текст в поле ввода, а не
     * покажет в ленте сообщение, которого сервер не видел.
     */
    // TODO(PR 4): POST /projects/{id}/generate.
    override suspend fun sendMessage(projectId: String, text: String): Result<ChatMessage> =
        Result.failure(UnsupportedOperationException("POST /generate ещё не реализован"))

    /** Пустая история и недоступная история для экрана одинаковы: см. KDoc класса. */
    private suspend fun loadHistory(projectId: String): List<ChatMessageDto> =
        apiResult {
            apiCall { client.get(chatPath(projectId)).body<ChatHistoryResponseDto>() }
        }.getOrNull()?.chatHistory.orEmpty()

    private fun chatPath(projectId: String): String =
        "$ProjectsPath/${projectId.encodeURLPathPart()}/$ChatSegment"
}
