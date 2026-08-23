import AuthenticationServices
import Foundation
import UIKit
import YandexLoginSDK

/// Абстракция над Yandex LoginSDK: вьюмодели и превью зависят от протокола,
/// а не от синглтона SDK.
@MainActor
protocol YandexAuthorizing {
    /// Запускает нативный флоу Yandex ID (приложения Яндекса → Safari)
    /// и возвращает OAuth-токен для обмена на нашу сессию.
    func authorize() async throws -> String
}

enum YandexAuthError: Error {
    /// Пользователь закрыл окно авторизации. Не ошибка — молча сбросить состояние.
    case cancelled
    /// SDK не активирован (плейсхолдерный Client ID) или нет окна для показа.
    case unavailable
}

/// Мост между колбэчным API LoginSDK и async/await.
///
/// SDK общается через глобальный observer, поэтому сервис существует в одном
/// экземпляре и держит максимум одну активную авторизацию.
@MainActor
final class YandexAuthService: NSObject, YandexAuthorizing {
    private var continuation: CheckedContinuation<String, Error>?
    private static var isActivated = false

    /// Вызывается один раз на старте приложения. При плейсхолдерном Client ID
    /// активация падает — кнопка Яндекса тогда отвечает `.unavailable`,
    /// остальное приложение живёт как обычно.
    static func activate() {
        do {
            try YandexLoginSDK.shared.activate(with: YandexOAuthConfig.clientID)
            isActivated = true
        } catch {
            isActivated = false
        }
    }

    /// Пробрасывает возврат из Safari/браузера по схеме `yx<clientID>`.
    static func handleOpenURL(_ url: URL) {
        _ = YandexLoginSDK.shared.tryHandleOpenURL(url)
    }

    /// Пробрасывает возврат из приложений Яндекса по universal link.
    static func handleUserActivity(_ userActivity: NSUserActivity) {
        _ = YandexLoginSDK.shared.tryHandleUserActivity(userActivity)
    }

    func authorize() async throws -> String {
        guard Self.isActivated, continuation == nil else {
            throw YandexAuthError.unavailable
        }
        guard let presenter = Self.topViewController() else {
            throw YandexAuthError.unavailable
        }

        YandexLoginSDK.shared.add(observer: self)
        return try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation
            do {
                try YandexLoginSDK.shared.authorize(with: presenter)
            } catch {
                self.finish(with: .failure(error))
            }
        }
    }

    private func finish(with result: Result<String, Error>) {
        guard let continuation else { return }
        self.continuation = nil
        YandexLoginSDK.shared.remove(observer: self)
        switch result {
        case .success(let token):
            continuation.resume(returning: token)
        case .failure(let error):
            continuation.resume(throwing: Self.normalized(error))
        }
    }

    /// Отмену SDK отдаёт как ошибку — приводим её к `YandexAuthError.cancelled`.
    /// Тип `userClosedWebViewController` у SDK internal, доступен только текст.
    private static func normalized(_ error: Error) -> Error {
        if let sdkError = error as? any YandexLoginSDKError,
           sdkError.message.contains("closed the view controller") {
            return YandexAuthError.cancelled
        }
        if let sessionError = error as? ASWebAuthenticationSessionError,
           sessionError.code == .canceledLogin {
            return YandexAuthError.cancelled
        }
        return error
    }

    private static func topViewController() -> UIViewController? {
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)

        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}

extension YandexAuthService: YandexLoginSDKObserver {
    nonisolated func didFinishLogin(with result: Result<LoginResult, any Error>) {
        Task { @MainActor in
            self.finish(with: result.map(\.token))
        }
    }
}
