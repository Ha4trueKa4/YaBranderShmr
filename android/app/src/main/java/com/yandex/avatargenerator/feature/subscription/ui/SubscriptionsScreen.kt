package com.yandex.avatargenerator.feature.subscription.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.core.mvi.CollectEffects
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Экран подписок пользователя: список подписчиков или подписок (какой именно —
 * задаёт [initialTab], та вкладка, по которой пришли с профиля). Заголовок и
 * запрос зависят от вкладки; переключателя на экране нет — на второй список
 * возвращаются через профиль.
 *
 * Навигация отдана наружу коллбэками, а внутри она приходит эффектом
 * [SubscriptionsEffect]: куда ведёт тап по строке — в чужой профиль или на свою
 * вкладку — решает ViewModel, у которой есть id текущего пользователя.
 *
 * `userId`/`initialTab`/`currentUserId` известны только при открытии — передаём
 * их параметрами ([parametersOf]); граф — в
 * `feature/subscription/di/SubscriptionModule.kt`.
 *
 * @param onBack закрыть экран (стрелка «назад»).
 * @param onUserClick открыть профиль пользователя из списка по его id.
 * @param onOpenOwnProfile переключиться на вкладку своего профиля — тап по
 *   собственной строке в списке.
 * @param currentUserId id текущего пользователя — его строка показывается без
 *   кнопки подписки (на себя не подписываются); `null`, если id недоступен.
 */
@Composable
fun SubscriptionsScreen(
    userId: String,
    initialTab: SubscriptionsTab,
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    onOpenOwnProfile: () -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: String? = null,
    viewModel: SubscriptionsViewModel = koinViewModel { parametersOf(userId, initialTab) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // id приезжает из потока и на первом кадре всегда null, поэтому отдаём его
    // интентом, а не параметром конструктора: тот вызывается ровно раз и
    // заморозил бы null.
    LaunchedEffect(currentUserId) {
        viewModel.onIntent(SubscriptionsIntent.OnCurrentUserResolved(currentUserId))
    }

    // Возврат на экран: пока ходили в чужой профиль, там могли подписаться или
    // отписаться.
    //
    // Первый резюм пропускаем — список уже грузится из init ViewModel. Флаг живёт
    // в композиции и намеренно не `rememberSaveable`: при повороте композиция
    // пересоздаётся, ViewModel с уже загруженным списком — нет, и сброс флага
    // избавляет от лишнего запроса на каждый поворот.
    var isFirstResume by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) {
            isFirstResume = false
        } else {
            viewModel.onIntent(SubscriptionsIntent.OnScreenResumed)
        }
    }

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            SubscriptionsEffect.NavigateBack -> onBack()
            is SubscriptionsEffect.OpenUserProfile -> onUserClick(effect.userId)
            SubscriptionsEffect.OpenOwnProfile -> onOpenOwnProfile()
        }
    }

    SubscriptionsContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
