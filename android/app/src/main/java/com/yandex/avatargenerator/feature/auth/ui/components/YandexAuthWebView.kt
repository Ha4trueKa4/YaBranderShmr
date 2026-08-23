package com.yandex.avatargenerator.feature.auth.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.auth.domain.model.YandexAuthRequest
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing

/**
 * Полноэкранный WebView страницы согласия Yandex ID (web-флоу).
 *
 * Токен экран не видит: как только навигация уходит на [YandexAuthRequest.redirectUri],
 * клиент достаёт `code` из query и отдаёт наружу, а саму страницу редиректа не
 * грузит. Обмен `code` на токен делает бэкенд.
 *
 * @param onCodeReceived редирект принёс `code` и `state` совпал.
 * @param onFailed редирект с `error`, без `code` или с чужим `state`.
 * @param onDismiss пользователь закрыл экран (крестик или системное «назад»).
 */
@Composable
internal fun YandexAuthWebView(
    request: YandexAuthRequest,
    onCodeReceived: (String) -> Unit,
    onFailed: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(color = AppColors.Background, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.xs),
                    verticalArrangement = Arrangement.Center,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.auth_yandex_close),
                            tint = AppColors.TextPrimary,
                        )
                    }
                }
                YandexAuthWebViewHost(
                    request = request,
                    onCodeReceived = onCodeReceived,
                    onFailed = onFailed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YandexAuthWebViewHost(
    request: YandexAuthRequest,
    onCodeReceived: (String) -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // Страница согласия Yandex ID без JS и DOM storage не работает.
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = YandexRedirectClient(request, onCodeReceived, onFailed)
                loadUrl(request.authorizeUrl)
            }
        },
        // WebView не уничтожается сам при уходе из композиции — освобождаем
        // вручную, иначе он держит контекст и нативные ресурсы после закрытия.
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
    )
}

/**
 * Ловит переход на `redirect_uri` и разбирает результат. На клике по ссылке
 * срабатывает [shouldOverrideUrlLoading] (возвращаем `true` — страницу
 * callback'а не грузим), на серверном 302 — [onPageStarted]: там
 * `shouldOverrideUrlLoading` не всегда вызывается. Оба идут через один
 * идемпотентный [handleRedirect].
 */
private class YandexRedirectClient(
    private val request: YandexAuthRequest,
    private val onCodeReceived: (String) -> Unit,
    private val onFailed: () -> Unit,
) : WebViewClient() {

    private var handled = false

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url
        if (!url.toString().startsWith(this.request.redirectUri)) return false
        handleRedirect(url)
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        if (url != null && url.startsWith(request.redirectUri)) {
            // handleRedirect первым: он ставит handled=true, поэтому отмена
            // загрузки ниже не долетит до onReceivedError как «ошибка».
            handleRedirect(url.toUri())
            view.stopLoading()
        }
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        // Только сбой самой страницы согласия (нет сети, TLS, DNS). Ошибки
        // подресурсов и уже разобранный редирект (handled) игнорируем — иначе
        // отмена загрузки callback'а прилетела бы сюда как ложный сбой.
        if (request.isForMainFrame && !handled) {
            handled = true
            onFailed()
        }
    }

    private fun handleRedirect(url: Uri) {
        // Редирект может прийти дважды (например, при повторной навигации) —
        // наружу отдаём результат один раз.
        if (handled) return
        handled = true

        val returnedState = url.getQueryParameter("state")
        val code = url.getQueryParameter("code")
        val error = url.getQueryParameter("error")
        when {
            error != null -> onFailed()
            returnedState != request.state -> onFailed()
            !code.isNullOrEmpty() -> onCodeReceived(code)
            else -> onFailed()
        }
    }
}
