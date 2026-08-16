import Foundation

nonisolated protocol FeedServiceProtocol: Sendable {
    func fetchPage(scope: FeedScope, cursor: String?, limit: Int) async throws -> FeedPage
}

/// Загрузка ленты: `GET /feed` с курсорной пагинацией.
///
/// Авторизация опциональная: `discover` доступен и без токена,
/// `following` без токена вернёт 401 — это решает сервер, а не клиент.
nonisolated final class FeedService: FeedServiceProtocol {
    private let client: NetworkClientProtocol

    init(client: NetworkClientProtocol = NetworkClient.shared) {
        self.client = client
    }

    func fetchPage(scope: FeedScope, cursor: String?, limit: Int) async throws -> FeedPage {
        let dto: FeedResponseDTO = try await client.request(
            .get,
            .feed(scope: scope, cursor: cursor, limit: limit),
            authorization: .optional
        )
        return FeedPage(dto: dto)
    }
}
