package com.yandex.avatargenerator.feature.project.ui.publicproject

import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.project.domain.ProjectExporter
import com.yandex.avatargenerator.feature.project.domain.PublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import kotlinx.coroutines.launch

/**
 * ViewModel экрана публичного проекта: грузит проект по [projectId] через
 * [PublicProjectRepository], держит состояние кнопки подписки и запускает
 * выгрузку превью в PNG/SVG ([ProjectExporter]).
 */
internal class PublicProjectViewModel(
    private val repository: PublicProjectRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val exporter: ProjectExporter,
    private val projectId: String,
) : BaseViewModel<PublicProjectIntent, PublicProjectState, PublicProjectEffect>(
    PublicProjectState(),
) {

    init {
        load()
    }

    override suspend fun handleIntent(intent: PublicProjectIntent, state: PublicProjectState) {
        when (intent) {
            PublicProjectIntent.OnRetryClick -> load()
            PublicProjectIntent.OnBackClick -> sendEffect(PublicProjectEffect.NavigateBack)

            PublicProjectIntent.OnAuthorClick -> {
                // Автор без id (профиль не подгрузился) — открывать нечего.
                val authorId = state.project?.author?.id
                if (!authorId.isNullOrEmpty()) {
                    sendEffect(PublicProjectEffect.OpenAuthor(authorId))
                }
            }

            is PublicProjectIntent.OnRelatedClick ->
                sendEffect(PublicProjectEffect.OpenRelated(intent.projectId))

            PublicProjectIntent.OnFollowClick -> toggleFollow(state)
            is PublicProjectIntent.OnExportClick -> export(state, intent.format)
        }
    }

    /**
     * Сохраняет превью проекта в выбранном [format]. Пока файл пишется —
     * повторные тапы игнорируем ([PublicProjectState.isExporting]). Проект без
     * превью выгружать нечего.
     *
     * Флаг поднимаем **до** `launch`: цикл интентов не приостанавливается, пока
     * в канале есть следующий интент, и второй тап успел бы увидеть `false`.
     */
    private suspend fun export(state: PublicProjectState, format: ExportFormat) {
        val project = state.project ?: return
        if (state.isExporting) return

        val svgUrl = project.previewUrl
        if (svgUrl.isNullOrBlank()) {
            sendEffect(PublicProjectEffect.ShowNothingToExport)
            return
        }

        updateState { copy(isExporting = true) }
        viewModelScope.launch {
            val result = exporter.export(svgUrl, project.title, format)
            updateState { copy(isExporting = false) }
            sendEffect(
                if (result.isSuccess) {
                    PublicProjectEffect.ShowExportSuccess(format)
                } else {
                    PublicProjectEffect.ShowExportFailure(format)
                },
            )
        }
    }

    /**
     * Переключает подписку на автора через `POST`/`DELETE /users/{id}/subscribe`.
     * Обновляем UI оптимистично и откатываем, если запрос не прошёл; пока запрос
     * в полёте — повторные тапы игнорируем, чтобы встречные subscribe/unsubscribe
     * не разъезжались с откатами.
     *
     * Автор без id (профиль не подгрузился) подписать нельзя — тап игнорируем.
     */
    private fun toggleFollow(state: PublicProjectState) {
        val project = state.project ?: return
        val authorId = project.author.id
        if (authorId.isEmpty()) return
        if (state.isFollowInFlight) return

        val following = !project.author.isFollowing
        updateState { copy(isFollowInFlight = true).applyFollow(following) }

        viewModelScope.launch {
            val result = if (following) {
                subscriptionRepository.subscribe(authorId)
            } else {
                subscriptionRepository.unsubscribe(authorId)
            }
            updateState {
                val settled = copy(isFollowInFlight = false)
                if (result.isFailure) settled.applyFollow(!following) else settled
            }
        }
    }

    private fun PublicProjectState.applyFollow(following: Boolean): PublicProjectState {
        val current = project ?: return this
        return copy(
            project = current.copy(author = current.author.copy(isFollowing = following)),
        )
    }

    /**
     * Грузит проект отдельной корутиной, а не прямо в `handleIntent`: иначе
     * очередь интентов встала бы на всё время похода в сеть.
     */
    private fun load() {
        updateState { copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            repository.getPublicProject(projectId)
                .onSuccess { project ->
                    updateState { copy(isLoading = false, project = project, hasError = false) }
                }
                .onFailure {
                    updateState { copy(isLoading = false, hasError = true) }
                }
        }
    }
}
