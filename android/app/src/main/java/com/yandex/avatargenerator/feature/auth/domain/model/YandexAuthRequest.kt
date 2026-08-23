package com.yandex.avatargenerator.feature.auth.domain.model

/**
 * Параметры одного захода в Yandex ID: что открыть, куда придёт редирект и какой
 * `state` ждать обратно. Живёт в состоянии экрана, пока открыт WebView согласия.
 *
 * @property authorizeUrl страница согласия `oauth.yandex.ru/authorize`.
 * @property redirectUri префикс, по переходу на который WebView достаёт `code`;
 *  саму страницу не грузит.
 * @property state анти-CSRF: экран пускает `code` в дело, только если вернувшийся
 *  `state` совпал с этим.
 */
data class YandexAuthRequest(
    val authorizeUrl: String,
    val redirectUri: String,
    val state: String,
)
