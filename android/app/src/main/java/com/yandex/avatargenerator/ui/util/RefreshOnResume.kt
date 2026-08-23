package com.yandex.avatargenerator.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Перезагружает данные экрана при возврате на него ([Lifecycle.Event.ON_RESUME]),
 * пропуская первый резюм: там данные уже грузит `init` ViewModel'и, а пока ходили
 * в другой экран, инфа могла устареть (создали проект, подписались, сменили
 * видимость). [onResume] должен обновлять молча — без скелетона поверх текущего
 * содержимого, иначе каждый возврат мигал бы загрузкой.
 */
@Composable
fun RefreshOnResume(key: Any?, onResume: () -> Unit) {
    var isFirstResume by remember(key) { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) isFirstResume = false else onResume()
    }
}
