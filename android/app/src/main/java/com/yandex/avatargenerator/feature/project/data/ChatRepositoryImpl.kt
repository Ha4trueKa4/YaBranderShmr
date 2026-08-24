package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.project.data.dto.ChatHistoryResponseDto
import com.yandex.avatargenerator.feature.project.data.dto.ChatMessageDto
import com.yandex.avatargenerator.feature.project.data.dto.GenerateProjectRequestDto
import com.yandex.avatargenerator.feature.project.data.dto.GenerationStartedDto
import com.yandex.avatargenerator.feature.project.data.dto.GenerationStatusDto
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.VersionsRepository
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val ProjectsPath = "projects"
private const val ChatSegment = "chat"
private const val GenerateSegment = "generate"
private const val CancelSegment = "cancel"

private const val DoneStatus = "done"
private const val ErrorStatus = "error"
private const val CancelledStatus = "cancelled"

/** Сколько раз перечитать историю, потеряв задачу: запись в неё могла отстать. */
private const val FallbackAttempts = 2

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
 * тестами. На чтение свой запрос здесь один — за историей.
 *
 * Генерация асинхронная: `POST /generate` только ставит задачу и отвечает
 * `job_id`, а результат забирает опрос статус-ручки (long polling) — см.
 * [awaitGeneration]. Отмена ([cancelGeneration]) в этот опрос и приходит:
 * своего исхода она не отдаёт.
 *
 * **Деградация вместо ошибки.** `Result.failure` у [getChat] возвращается только
 * когда упал `GET /projects/{id}` — без него у экрана нет даже заголовка. Ошибки
 * истории и версий (403 у не-автора, 404, 5xx) съедаем: лента получится пустой,
 * но проект откроется на чтение.
 */
class ChatRepositoryImpl(
    private val client: HttpClient,
    private val projectRepository: ProjectRepository,
    private val versionsRepository: VersionsRepository,
    private val polling: GenerationPolling = GenerationPolling(),
) : ChatRepository {

    override suspend fun getChat(projectId: String): Result<ProjectChat> = coroutineScope {
        val projectAsync = async { projectRepository.getProject(projectId) }
        val historyAsync = async { loadHistory(projectId) }
        val versionsAsync = async { versionsRepository.getVersions(projectId) }

        projectAsync.await().map { project ->
            val history = historyAsync.await()
            ProjectChat(
                id = project.id,
                title = project.name,
                isPublic = project.isPublic,
                messages = buildChatTimeline(
                    history = history,
                    // Сервер отдаёт версии по убыванию номера — последняя первая.
                    latestVersionId = versionsAsync.await().getOrNull()?.firstOrNull()?.id,
                    latestVersionUrl = project.url,
                ),
            )
        }
    }

    /**
     * Своего таймаута у запроса больше нет: ручка отвечает 202 за миллисекунды,
     * общих 30 секунд хватает с запасом.
     *
     * Ретраев у вызова нет и быть не должно: `POST` не подпадает под политику
     * `isRetryable`, а повтор после 5xx шлюза создал бы вторую задачу и вторую
     * пару сообщений в истории. Трогать политику ради генерации нельзя.
     */
    override suspend fun startGeneration(
        projectId: String,
        prompt: String,
        model: String?,
    ): Result<String> = apiResult {
        val started = apiCall {
            client.post(generatePath(projectId)) {
                setBody(GenerateProjectRequestDto(prompt = prompt, model = model))
            }.body<GenerationStartedDto>()
        }
        // Без job_id опрашивать нечего: контракт разошёлся с бэкендом.
        started.jobId?.takeIf { it.isNotBlank() }
            ?: throw NetworkException.Serialization(IllegalStateException("generate: no job_id"))
    }

    /**
     * Опрашивает статус-ручку до общего дедлайна ([GenerationPolling.deadlineMillis]).
     *
     * Дедлайн держит [withTimeoutOrNull], а не подсчёт по часам: считать разницу
     * времени пришлось бы вручную в каждой ветке цикла. Оборванный им запрос не
     * страшен — задача на сервере от этого не останавливается.
     *
     * Таймаут и обрыв связи ожидание не прерывают: сеть могла моргнуть ровно на
     * этом опросе. А 404 значит, что бэкенд перезапустился и потерял задачу
     * (она живёт в памяти процесса) — тогда результат ищем в истории чата, а не
     * найдя, отдаём тот же 404 ошибкой: исход генерации остался неизвестен.
     */
    override suspend fun awaitGeneration(
        projectId: String,
        jobId: String,
    ): Result<GenerationOutcome> =
        withTimeoutOrNull(polling.deadlineMillis) { poll(projectId, jobId) }
            ?: Result.success(GenerationOutcome.TimedOut)

    private suspend fun poll(projectId: String, jobId: String): Result<GenerationOutcome> {
        while (true) {
            val status = apiResult {
                apiCall {
                    client.get(statusPath(projectId, jobId)) {
                        // Ручка держит соединение до 25 секунд — общих 30 мало.
                        timeout {
                            requestTimeoutMillis = polling.statusTimeoutMillis
                            socketTimeoutMillis = polling.statusTimeoutMillis
                        }
                    }.body<GenerationStatusDto>()
                }
            }

            val error = status.exceptionOrNull()
            if (error != null) {
                when (error) {
                    is NetworkException.Timeout,
                    is NetworkException.NoConnection,
                    -> delay(polling.guardMillis)

                    is NetworkException.NotFound -> return fromHistory(projectId, jobId, error)

                    else -> return Result.failure(error)
                }
            } else {
                val dto = status.getOrThrow()
                when (dto.status) {
                    DoneStatus -> return Result.success(GenerationOutcome.Done(dto.url))
                    ErrorStatus -> return Result.success(GenerationOutcome.Failed(dto.message))
                    CancelledStatus -> return Result.success(GenerationOutcome.Cancelled)
                    // pending, переходный cancelling и любой незнакомый статус —
                    // просто спрашиваем ещё раз.
                    else -> delay(polling.guardMillis)
                }
            }
        }
    }

    /**
     * Статус-ручка потеряла задачу — ищем результат в истории чата по `job_id`.
     *
     * Читаем историю, а не весь [getChat]: `job_id` есть только в DTO, а проект
     * и версии здесь ни при чём — экран всё равно перечитает ленту сам.
     *
     * Пара попыток с паузой: бэкенд мог перезапуститься ровно между генерацией и
     * записью её результата в историю.
     *
     * Не нашли — отдаём [notFound], а не [GenerationOutcome.Failed]: исхода
     * генерации мы так и не узнали (задача могла оборваться вместе с процессом,
     * а могла просто не читаться история), и записи об ошибке на сервере нет.
     * Экрану это различие важно — по `Failed` он перечитывает ленту, а тут
     * читать нечего.
     */
    /**
     * Ответ ручки не разбираем и не отдаём наружу: исход всё равно принесёт
     * идущий [awaitGeneration], а два источника правды на один скелетон
     * разъехались бы (`cancelling` в ответе на отмену — не финальный статус).
     * Здесь важно только, дошёл ли запрос до сервера.
     *
     * Ретрая нет по той же причине, что и у старта: `POST` не подпадает под
     * `isRetryable`. Повтор отмены, впрочем, безобиден — но и смысла в нём нет,
     * ожидание никуда не денется, а пользователь может нажать ещё раз.
     */
    override suspend fun cancelGeneration(projectId: String, jobId: String): Result<Unit> =
        apiResult {
            apiCall { client.post(cancelPath(projectId, jobId)) }
            Unit
        }

    private suspend fun fromHistory(
        projectId: String,
        jobId: String,
        notFound: NetworkException.NotFound,
    ): Result<GenerationOutcome> {
        repeat(FallbackAttempts) { attempt ->
            if (attempt > 0) delay(polling.guardMillis)
            loadHistory(projectId).findGenerationOutcome(jobId)?.let { return Result.success(it) }
        }
        return Result.failure(notFound)
    }

    /** Пустая история и недоступная история для экрана одинаковы: см. KDoc класса. */
    private suspend fun loadHistory(projectId: String): List<ChatMessageDto> =
        apiResult {
            apiCall { client.get(chatPath(projectId)).body<ChatHistoryResponseDto>() }
        }.getOrNull()?.chatHistory.orEmpty()

    private fun chatPath(projectId: String): String =
        projectPath(projectId, ChatSegment)

    private fun generatePath(projectId: String): String =
        projectPath(projectId, GenerateSegment)

    private fun statusPath(projectId: String, jobId: String): String =
        "${generatePath(projectId)}/${jobId.encodeURLPathPart()}"

    private fun cancelPath(projectId: String, jobId: String): String =
        "${statusPath(projectId, jobId)}/$CancelSegment"

    private fun projectPath(projectId: String, segment: String): String =
        "$ProjectsPath/${projectId.encodeURLPathPart()}/$segment"
}
