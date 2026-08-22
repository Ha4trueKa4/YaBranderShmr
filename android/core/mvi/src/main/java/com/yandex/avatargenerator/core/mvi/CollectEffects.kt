package com.yandex.avatargenerator.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Подписка экрана на одноразовые эффекты.
 *
 * Собираем, только пока экран не ниже `STARTED`: иначе эффект, пришедший в фоне,
 * дёрнул бы `navigate` на остановленном `NavController`. Пока подписки нет,
 * эффекты ждут в канале [BaseViewModel] и доедут при возврате на экран — кроме
 * одного крайнего случая: элемент, уже вынутый из канала, но не успевший
 * доехать до [onEffect] к моменту остановки экрана, теряется (так устроен
 * `receiveAsFlow`). Окно — доли кадра на самом сворачивании приложения; если
 * для какого-то экрана потеря перехода недопустима, эффект там не подходит,
 * нужен флаг в состоянии.
 *
 * [onEffect] читаем через [rememberUpdatedState]: сбор не перезапускается на
 * каждой рекомпозиции, и без этого навсегда захватилась бы самая первая лямбда
 * вместе со всем, что она замкнула.
 */
@Composable
fun <E : BaseEffect> CollectEffects(effects: Flow<E>, onEffect: (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { currentOnEffect(it) }
        }
    }
}
