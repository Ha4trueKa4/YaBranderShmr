import Foundation

/// Проверка протухания подписанных ссылок на приватные изображения.
///
/// Предполагает AWS S3 presigned URL с параметрами `X-Amz-Date` (формат
/// `yyyyMMdd'T'HHmmss'Z'`) и `X-Amz-Expires` (секунды). Для ссылок без этих
/// параметров возвращает `false` — считается, что срок действия не истекает.
enum URLSignature {
    static func isExpired(_ url: URL, now: Date = Date()) -> Bool {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let queryItems = components.queryItems
        else { return false }

        let values = Dictionary(uniqueKeysWithValues: queryItems.map {
            ($0.name.lowercased(), $0.value ?? "")
        })
        guard let dateValue = values["x-amz-date"],
              let expiresValue = values["x-amz-expires"],
              let expires = TimeInterval(expiresValue),
              let date = signatureDateFormatter.date(from: dateValue)
        else { return false }

        return date.addingTimeInterval(expires - 30) <= now
    }

    private static let signatureDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyyMMdd'T'HHmmss'Z'"
        return formatter
    }()
}
