package com.yandex.avatargenerator.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Перезагружает данные экрана при возврате на него ([Lifecycle.Event.ON_RESUME]),
 * пропуская первый резюм: там данные уже грузит `init` ViewModel'и, а пока ходили
 * в другой экран, инфа могла устареть (создали проект, подписались, сменили
 * видимость). [onResume] должен обновлять молча — без скелетона поверх текущего
 * содержимого, иначе каждый возврат мигал бы загрузкой.
 *
 * Флаг «первого резюма» держим в [rememberSaveable], а не [remember]: при
 * навигации вперёд NavHost уничтожает композицию destination'а (а `NavBackStackEntry`
 * и ViewModel живут), и обычный `remember` при возврате переинициализировался бы в
 * `true` — тогда приходящий следом ON_RESUME снова считался бы «первым» и onResume
 * не вызвался бы. Saveable-состояние navigation-compose сохраняет по entry, поэтому
 * флаг переживает возврат. Побочка — после поворота экрана флаг тоже `false`, будет
 * один лишний тихий refresh; это безвредно.
 */
@Composable
fun RefreshOnResume(key: Any?, onResume: () -> Unit) {
    var isFirstResume by rememberSaveable(key) { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) isFirstResume = false else onResume()
    }
}
