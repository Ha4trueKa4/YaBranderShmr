import Foundation

/// Декодирование полезной нагрузки JWT без проверки подписи.
///
/// Нужно для восстановления id пользователя после переустановки (токен в
/// Keychain сохраняется, а UserDefaults очищается) и для проверки срока жизни
/// токена. Подпись здесь не проверяется — сам токен всё равно валидирует бэкенд
/// на каждом запросе.
nonisolated enum JWTDecoder {
    static func userID(from token: String) -> UUID? {
        (payload(from: token)?["user_id"] as? String)
            .flatMap(UUID.init(uuidString:))
    }

    static func expirationDate(from token: String) -> Date? {
        guard let value = payload(from: token)?["exp"] as? NSNumber else { return nil }
        return Date(timeIntervalSince1970: value.doubleValue)
    }

    /// Токен считается протухшим и в случае, когда `exp` не удалось прочитать:
    /// лучше разлогиниться, чем продолжать слать заведомо невалидный токен.
    static func isExpired(_ token: String, now: Date = .now) -> Bool {
        guard let expiration = expirationDate(from: token) else { return true }
        return expiration <= now
    }

    private static func payload(from token: String) -> [String: Any]? {
        let segments = token.split(separator: ".")
        guard segments.count == 3 else { return nil }

        guard let data = base64URLDecode(String(segments[1])),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }

        return json
    }

    private static func base64URLDecode(_ input: String) -> Data? {
        var base64 = input
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 {
            base64.append("=")
        }
        return Data(base64Encoded: base64)
    }
}
