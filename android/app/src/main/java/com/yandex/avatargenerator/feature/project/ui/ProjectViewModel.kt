package com.yandex.avatargenerator.feature.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.GenerationModelRepository
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.VersionsRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * ViewModel экрана проекта: грузит по [projectId] диалог с ИИ через
 * [ChatRepository], держит черновик ввода и отправляет промпты на генерацию.
 *
 * Название и видимость проекта приходят в том же ответе репозитория — своего
 * запроса за проектом ViewModel не делает: репозиторий всё равно ходит за ним,
 * чтобы взять ссылку на последнюю версию, и второй такой же запрос был бы лишним.
 *
 * Превью прошлых версий догружаются отдельно через [VersionsRepository]: история
 * чата отдаёт только `version_id`, а ссылка на картинку у каждой версии своя и
 * запрашивается по одной (`GET /projects/{id}/versions/{versionId}`) — и только
 * для тех пузырей, которые дошли до экрана (см. [onPreviewNeeded]).
 *
 * Модель генерации выбирается чипом над полем ввода: сам выбор читается из
 * [GenerationModelRepository] при открытии экрана, а справочник тянется лениво —
 * при первом открытии шторки (см. [openModelPicker]).
 *
 * **Ожидание генерации живёт в `viewModelScope`.** Ушёл с экрана — опрос умер;
 * бэкенд доводит генерацию до конца сам. Ни `WorkManager`, ни хранение
 * `job_id` на диске ради этого не заводим: незакрытую задачу видно в самой
 * ленте — пока она считается, история отдаёт заглушку [ChatMessage.Pending],
 * и по ней экран возобновляет опрос (см. [load]). Оттуда же
 * [ProjectUiState.Content.pendingJobId] берёт задачу для остановки, так что
 * прервать генерацию можно и после перезахода. Пока она считается, отправка
 * заблокирована: второй `POST /generate` бэкенд всё равно отвергает, у проекта
 * не бывает двух активных задач.
 *
 * Экран можно открыть с уже запущенной задачей первичной генерации
 * ([initialJobId]) — так на него уходит форма создания проекта, получив от
 * `POST /projects` ответ `202` с `job_id`. Тогда экран сразу показывает
 * скелетон ответа и ждёт результат, не заставляя перезаходить.
 */
internal class ProjectViewModel(
    private val chatRepository: ChatRepository,
    private val versionsRepository: VersionsRepository,
    private val projectRepository: ProjectRepository,
    private val generationModelRepository: GenerationModelRepository,
    private val projectId: String,
    initialJobId: String? = null,
) : ViewModel() {

    /** Задача первичной генерации, с которой экран открыли. Ждём её один раз. */
    private var pendingInitialJobId: String? = initialJobId

    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Loading)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    /**
     * Уже известные ссылки на превью, versionId → url. Переживают перечитывание
     * ленты: сервер отдаёт ссылку только для последней версии, и без кэша все
     * остальные пузыри теряли бы картинку на каждом обновлении.
     *
     * Presigned-ссылка живёт час — этого хватает на сеанс работы с экраном.
     */
    private val previewUrls = mutableMapOf<String, String>()

    /**
     * Версии, за ссылкой на которые уже ходили — успешно или нет. Пузырь в
     * `LazyColumn` пересоздаётся на каждом заходе в зону видимости, и без этого
     * набора прокрутка ленты вверх-вниз слала бы запрос за запросом.
     */
    private val requestedVersions = mutableSetOf<String>()

    init {
        load()
    }

    /** Повтор после ошибки. */
    fun retry() = load()

    /**
     * Переключает видимость проекта (`PATCH /projects/{id}`).
     *
     * Оптимистично: чип в шапке и шторка переключаются сразу, до ответа сервера —
     * так тап ощущается мгновенным. Не прошло — откатываем видимость обратно; свой
     * промпт-флоу это не трогает, генерация и приватность независимы.
     */
    fun setVisibility(isPublic: Boolean) {
        val state = _uiState.value
        if (state !is ProjectUiState.Content || state.isPublic == isPublic) return

        _uiState.updateContent { it.copy(isPublic = isPublic) }
        viewModelScope.launch {
            projectRepository.updateProject(projectId, isPublic = isPublic)
                .onFailure { _uiState.updateContent { content -> content.copy(isPublic = !isPublic) } }
        }
    }

    /** Ввод обрезаем по [MaxPromptBytes] — см. комментарий у константы. */
    fun onDraftChange(text: String) {
        _uiState.updateContent { it.copy(draft = limitPromptBytes(text)) }
    }

    /**
     * Отправляет черновик на генерацию.
     *
     * Поле очищается сразу, до ответа сервера: иначе ввод «залипает» с уже
     * отправленным текстом. Вернуть текст обратно можно ровно в одном случае —
     * если промпт до сервера не дошёл (см. [dropPending]).
     */
    fun send() {
        val state = _uiState.value
        if (state !is ProjectUiState.Content || state.isSending) return
        val text = state.draft.trim()
        if (text.isEmpty()) return

        _uiState.updateContent { it.copy(draft = "") }
        generate(text)
    }

    /**
     * Повторяет промпт, стоящий над пузырём ошибки [messageId].
     *
     * Это **новая задача и новая пара сообщений в истории** — бэкенд ничего не
     * перезаписывает, так что дубль промпта на сервере неизбежен. Сам пузырь
     * ошибки не убираем: он лежит в истории на сервере и всё равно вернётся при
     * следующем перечитывании ленты.
     */
    fun retryGeneration(messageId: String) {
        val state = _uiState.value
        if (state !is ProjectUiState.Content || state.isSending) return
        val text = state.messages.promptAbove(messageId) ?: return

        generate(text)
    }

    /**
     * Просит сервер прервать идущую генерацию.
     *
     * Скелетон в ленте при этом остаётся: ждём мы всё тем же опросом статуса, и
     * убрать его раньше исхода значило бы соврать — задача останавливается не
     * мгновенно, а сама отмена может и не дойти. До исхода экран показывает
     * промежуточное «останавливаем» ([ProjectUiState.Content.isCancelling]).
     *
     * Не прошло — снимаем только этот флаг: ожидание идёт своим чередом, и
     * нажать «остановить» можно ещё раз.
     */
    fun cancelGeneration() {
        val jobId = _uiState.value.contentOrNull()?.takeIf { it.canCancel }?.pendingJobId ?: return

        _uiState.updateContent { it.copy(isCancelling = true) }
        viewModelScope.launch {
            chatRepository.cancelGeneration(projectId, jobId)
                .onFailure { _uiState.updateContent { content -> content.copy(isCancelling = false) } }
        }
    }

    /**
     * Общий путь отправки: оптимистичный пузырь промпта → старт задачи →
     * скелетон ответа → ожидание результата.
     *
     * Промпт кладётся в ленту сразу: генерация идёт минутами, и всё это время
     * экран не должен выглядеть так, будто нажатие пропало.
     */
    private fun generate(text: String) {
        // Модель фиксируем в момент отправки: пока идёт генерация, выбрать
        // можно уже другую, и промпт не должен уехать не на той.
        val model = _uiState.value.contentOrNull()?.modelPicker?.selectedModelId
        val prompt = ChatMessage.Outgoing(
            id = UUID.randomUUID().toString(),
            text = text,
            sentAt = Instant.now(),
        )
        _uiState.updateContent {
            it.copy(messages = it.messages + prompt, isSending = true, isCancelling = false)
        }

        viewModelScope.launch {
            chatRepository.startGeneration(projectId, text, model).fold(
                onSuccess = { jobId -> awaitGeneration(jobId) },
                // Единственный случай, когда промпт можно убрать из ленты и
                // вернуть в поле: до сервера он не дошёл, в историю не попал.
                onFailure = { dropPending(prompt, text) },
            )
        }
    }

    /**
     * Ждёт результат задачи [jobId], показывая на её месте скелетон ответа.
     *
     * Скелетон ставим свой, только если его ещё нет: в ленте, перечитанной во
     * время генерации, он уже пришёл с сервера, и второй стоял бы рядом.
     *
     * Исход, о котором знает сервер (версия готова либо генерация упала), лежит
     * в истории чата — по нему лента перечитывается целиком, и после перезахода
     * экран выглядит так же. Сорвавшийся опрос — другое дело: записи об исходе
     * на сервере нет, перечитывать нечего, пузырь ошибки рисуем локально. Текст
     * `NetworkException` в него не кладём — он служебный.
     */
    private suspend fun awaitGeneration(jobId: String) {
        val skeleton = _uiState.value.contentOrNull()?.messages?.pendingFor(jobId)
            ?: ChatMessage.Pending(id = UUID.randomUUID().toString(), jobId = jobId).also { own ->
                _uiState.updateContent { it.copy(messages = it.messages + own) }
            }

        chatRepository.awaitGeneration(projectId, jobId).fold(
            onSuccess = { outcome ->
                when (outcome) {
                    is GenerationOutcome.Done -> showAnswer(skeleton, jobId)
                    is GenerationOutcome.Failed -> showFailure(skeleton, jobId, outcome.message)
                    GenerationOutcome.Cancelled -> showCancelled(skeleton, jobId)
                    GenerationOutcome.TimedOut -> showStillRunning(skeleton)
                }
            },
            onFailure = { showFailure(skeleton, jobId = null, message = null) },
        )
    }

    /**
     * Версия готова — показываем её перечитанной лентой.
     *
     * Не дождались ответа в истории — ленту не трогаем вовсе, только снимаем
     * скелетон: перечитанная без ответа, она заменила бы собой и оптимистичный
     * промпт, и скелетон, и результат генерации пропал бы с экрана совсем.
     */
    private suspend fun showAnswer(skeleton: ChatMessage.Pending, jobId: String) {
        val chat = reloadUntilAnswered(jobId)
        if (chat != null) applyChat(chat) else dropSkeleton(skeleton)
    }

    /**
     * Показывает провал: записью из перечитанной ленты, а если её там нет —
     * локальным пузырём ошибки. Оптимистичный промпт при этом не трогаем: на
     * сервере он есть.
     *
     * @param jobId задача, ответ которой ищем в ленте; `null` — сорвался сам
     *  опрос, на сервере записи об исходе нет и искать нечего.
     */
    private suspend fun showFailure(
        skeleton: ChatMessage.Pending,
        jobId: String?,
        message: String?,
    ) {
        val chat = jobId?.let { reloadUntilAnswered(it) }
        if (chat != null) {
            applyChat(chat)
            return
        }
        val failure = ChatMessage.Failed(
            id = skeleton.id,
            text = message,
            sentAt = Instant.now(),
        )
        replaceSkeleton(skeleton, failure)
    }

    /**
     * Перечитывает ленту, пока в ней не появится ответ на задачу [jobId].
     *
     * Бэкенд будит опрос статуса **до** того, как допишет результат в историю
     * (и сама запись у него best-effort — может не пройти вовсе), так что первая
     * же перечитанная лента вполне может прийти ещё без ответа. Без этой
     * проверки она молча затирала бы собой всё, что экран знает о генерации.
     *
     * @return лента с ответом; `null` — ответа так и нет либо лента не читается.
     */
    private suspend fun reloadUntilAnswered(jobId: String): ProjectChat? {
        repeat(ReloadAttempts) { attempt ->
            if (attempt > 0) delay(ReloadRetryDelayMillis)
            val chat = chatRepository.getChat(projectId).getOrNull()
            if (chat != null && chat.messages.hasAnswerFor(jobId)) return chat
        }
        return null
    }

    /**
     * Генерацию прервали — показываем это записью из перечитанной ленты, а если
     * бэкенд ещё не дописал её, то локальной пилюлей на месте скелетона.
     * Промпт не трогаем: на сервере он есть, версии отменённая генерация не
     * создала.
     */
    private suspend fun showCancelled(skeleton: ChatMessage.Pending, jobId: String) {
        val chat = reloadUntilAnswered(jobId)
        if (chat != null) {
            applyChat(chat)
            return
        }
        replaceSkeleton(
            skeleton,
            ChatMessage.Cancelled(id = skeleton.id, sentAt = Instant.now()),
        )
    }

    /**
     * Дедлайн вышел — это не провал: задача могла остаться считаться. Убираем
     * скелетон и говорим об этом пилюлей, промпт в ленте оставляем.
     */
    private fun showStillRunning(skeleton: ChatMessage.Pending) {
        replaceSkeleton(
            skeleton,
            ChatMessage.System.GenerationStillRunning(id = skeleton.id),
        )
    }

    /** Скелетон меняется на месте — чтобы сообщение встало туда же, где он стоял. */
    private fun replaceSkeleton(skeleton: ChatMessage.Pending, replacement: ChatMessage) {
        _uiState.updateContent { content ->
            content.copy(
                messages = content.messages.map { if (it.id == skeleton.id) replacement else it },
                isSending = false,
                isCancelling = false,
            )
        }
    }

    /**
     * Убирает оптимистичное сообщение и возвращает текст в поле ввода.
     *
     * Новый черновик не перетираем: пользователь мог начать писать следующее
     * сообщение, пока шла отправка.
     */
    private fun dropPending(pending: ChatMessage.Outgoing, text: String) {
        _uiState.updateContent { content ->
            content.copy(
                messages = content.messages.filterNot { it.id == pending.id },
                draft = if (content.draft.isEmpty()) text else content.draft,
                isSending = false,
                isCancelling = false,
            )
        }
    }

    /**
     * Убирает скелетон, оставив ленту как есть: версия создана, и показать
     * из-за неудачного перезапроса [ProjectUiState.Error] было бы враньём.
     */
    private fun dropSkeleton(skeleton: ChatMessage.Pending) {
        _uiState.updateContent { content ->
            content.copy(
                messages = content.messages.filterNot { it.id == skeleton.id },
                isSending = false,
                isCancelling = false,
            )
        }
    }

    /**
     * Серверная лента заменяет локальную целиком — оптимистичный промпт и
     * скелетон уходят вместе с ней.
     */
    private fun applyChat(chat: ProjectChat) {
        rememberPreviews(chat.messages)
        _uiState.updateContent { content ->
            content.copy(
                title = chat.title,
                isPublic = chat.isPublic,
                messages = chat.messages.withKnownPreviews(),
                isSending = false,
                isCancelling = false,
            )
        }
    }

    /**
     * Открывает шторку выбора модели и при первом открытии тянет справочник.
     *
     * Лениво, а не при загрузке экрана: `/models` — статический справочник,
     * который пользователь может и не открыть, а имя выбранной модели чип рисует
     * из сохранённого `id` без сети.
     */
    fun openModelPicker() {
        val picker = _uiState.value.contentOrNull()?.modelPicker ?: return

        _uiState.updateModelPicker { it.copy(isSheetVisible = true) }
        if (picker.models.isEmpty() && !picker.isLoading) loadModels()
    }

    fun dismissModelPicker() {
        _uiState.updateModelPicker { it.copy(isSheetVisible = false) }
    }

    /**
     * Запоминает выбор и закрывает шторку.
     *
     * Состояние правим сразу, не дожидаясь записи в настройки: промпт всё равно
     * уходит с тем `id`, что лежит в состоянии, а запись — фоновая деталь.
     */
    fun selectModel(id: String) {
        if (_uiState.value.contentOrNull() == null) return

        _uiState.updateModelPicker { it.copy(selectedModelId = id, isSheetVisible = false) }
        viewModelScope.launch { generationModelRepository.selectModel(id) }
    }

    /** Повтор после неудачной загрузки справочника. */
    fun retryModels() {
        val picker = _uiState.value.contentOrNull()?.modelPicker ?: return
        if (picker.isLoading) return

        loadModels()
    }

    /**
     * Тянет справочник моделей.
     *
     * Пустой список от ошибки не отличаем: выбирать всё равно не из чего, а
     * шторке с пустым списком нечего показать, кроме той же ошибки с ретраем.
     * Отправку промптов ни то, ни другое не блокирует — в теле уедет сохранённый
     * `id`, и разбираться с ним будет сервер.
     *
     * Сохранённой модели может не оказаться в списке (бэкенд поменял allow-list,
     * либо выбор приехал с другой версии приложения) — тогда откатываемся на
     * дефолт, а нет и его — на первую доступную, и перезаписываем настройку,
     * чтобы расхождение не всплыло ещё раз.
     */
    private fun loadModels() {
        _uiState.updateModelPicker { it.copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            val models = generationModelRepository.getAvailableModels().getOrNull().orEmpty()
            if (models.isEmpty()) {
                _uiState.updateModelPicker { it.copy(isLoading = false, hasError = true) }
                return@launch
            }

            val selected = _uiState.value.contentOrNull()?.modelPicker?.selectedModelId
                ?: GenerationModel.DefaultId
            val resolved = models.resolveSelected(selected)
            _uiState.updateModelPicker {
                it.copy(
                    models = models,
                    selectedModelId = resolved,
                    isLoading = false,
                    hasError = false,
                )
            }
            if (resolved != selected) generationModelRepository.selectModel(resolved)
        }
    }

    /**
     * Пузырь версии [versionId] появился на экране — при первом показе идём за
     * ссылкой на превью.
     *
     * История чата отдаёт только `version_id`, а ссылку на картинку сервер
     * прикладывает лишь к последней версии — у остальных пузырей она пустая, и
     * без этой догрузки в ленте была бы видна одна последняя картинка.
     *
     * Ходим по требованию, а не пачкой при открытии экрана: у долгого проекта
     * это были бы десятки запросов разом ради картинок, которых пользователь
     * может и не пролистать. Повторно по той же версии не ходим — ни при
     * возврате пузыря в зону видимости, ни после ошибки: пузырь просто
     * останется без картинки, рушить из-за этого экран незачем.
     */
    fun onPreviewNeeded(versionId: String) {
        if (versionId in previewUrls || !requestedVersions.add(versionId)) return
        viewModelScope.launch {
            versionsRepository.getVersionUrl(projectId, versionId)
                .onSuccess { url -> applyPreview(versionId, url) }
        }
    }

    /** Дорисовывает пришедшую ссылку всем пузырям этой версии. */
    private fun applyPreview(versionId: String, url: String) {
        previewUrls[versionId] = url
        _uiState.updateContent { content ->
            content.copy(
                messages = content.messages.map { message ->
                    if (message is ChatMessage.Incoming &&
                        message.versionId == versionId &&
                        message.previewUrl == null
                    ) {
                        message.copy(previewUrl = url)
                    } else {
                        message
                    }
                },
            )
        }
    }

    /** Ссылку последней версии сервер присылает сам — кладём её в кэш как свою. */
    private fun rememberPreviews(messages: List<ChatMessage>) {
        messages.filterIsInstance<ChatMessage.Incoming>().forEach { message ->
            message.previewUrl?.let { previewUrls[message.versionId] = it }
        }
    }

    private fun List<ChatMessage>.withKnownPreviews(): List<ChatMessage> = map { message ->
        if (message is ChatMessage.Incoming && message.previewUrl == null) {
            previewUrls[message.versionId]?.let { message.copy(previewUrl = it) } ?: message
        } else {
            message
        }
    }

    /**
     * Пустая лента — валидное состояние (новый проект, недоступная история у
     * не-автора), а не [ProjectUiState.Error]: ошибку репозиторий отдаёт только
     * когда не загрузился сам проект.
     */
    private fun load() {
        _uiState.value = ProjectUiState.Loading
        viewModelScope.launch {
            // До сборки Content, иначе чип моргнул бы дефолтом при открытии.
            val selectedModelId = generationModelRepository.getSelectedModelId()
            val chat = chatRepository.getChat(projectId).getOrNull()
            _uiState.value = if (chat != null) {
                rememberPreviews(chat.messages)
                ProjectUiState.Content(
                    title = chat.title,
                    isPublic = chat.isPublic,
                    messages = chat.messages.withKnownPreviews(),
                    modelPicker = ModelPickerState(selectedModelId = selectedModelId),
                )
            } else {
                ProjectUiState.Error
            }

            // Ждём либо задачу, с которой экран открыли, либо незакрытую из
            // ленты — после перезахода опрос поднимается только так. Оба
            // варианта дают один и тот же job_id, если экран открыт формой
            // создания: pendingInitialJobId просто оказывается первым.
            //
            // Только по успешной загрузке: на Error ждать некуда — скелетон
            // и ответ рисует Content, а задачу подхватит retry().
            // Обнуляем до вызова, а не после: retry() зовёт тот же load(), и
            // иначе повторная загрузка запустила бы ожидание второй раз.
            if (chat != null) {
                val jobId = pendingInitialJobId ?: _uiState.value.contentOrNull()?.pendingJobId
                pendingInitialJobId = null
                if (jobId != null) {
                    _uiState.updateContent { it.copy(isSending = true) }
                    awaitGeneration(jobId)
                }
            }
        }
    }
}

/** Сколько раз перечитать ленту, дожидаясь ответа сервера на нашу задачу. */
private const val ReloadAttempts = 3

/** Пауза между перечитываниями: даём бэкенду дописать историю. */
private const val ReloadRetryDelayMillis = 500L

/** Скелетон задачи [jobId], если он уже стоит в ленте. */
private fun List<ChatMessage>.pendingFor(jobId: String): ChatMessage.Pending? =
    filterIsInstance<ChatMessage.Pending>().lastOrNull { it.jobId == jobId }

/** Есть ли в ленте ответ ИИ на задачу [jobId] — удачный, с ошибкой или отменённый. */
private fun List<ChatMessage>.hasAnswerFor(jobId: String): Boolean = any { message ->
    when (message) {
        is ChatMessage.Incoming -> message.jobId == jobId
        is ChatMessage.Failed -> message.jobId == jobId
        is ChatMessage.Cancelled -> message.jobId == jobId
        else -> false
    }
}

/**
 * Текст ближайшего промпта над сообщением [messageId] — тот, который и надо
 * повторить. `null` — сообщения в ленте уже нет или промпта над ним не нашлось.
 */
private fun List<ChatMessage>.promptAbove(messageId: String): String? {
    val index = indexOfFirst { it.id == messageId }
    if (index < 0) return null
    return take(index).filterIsInstance<ChatMessage.Outgoing>().lastOrNull()?.text
}

/** Содержимое экрана или `null`, если он всё ещё грузится либо упал. */
private fun ProjectUiState.contentOrNull(): ProjectUiState.Content? =
    this as? ProjectUiState.Content

/** Правка состояния, только если экран всё ещё показывает содержимое. */
private inline fun MutableStateFlow<ProjectUiState>.updateContent(
    crossinline block: (ProjectUiState.Content) -> ProjectUiState.Content,
) = update { state -> if (state is ProjectUiState.Content) block(state) else state }

/** То же для вложенного состояния пикера модели. */
private inline fun MutableStateFlow<ProjectUiState>.updateModelPicker(
    crossinline block: (ModelPickerState) -> ModelPickerState,
) = updateContent { content -> content.copy(modelPicker = block(content.modelPicker)) }
