import Foundation

/// Страница ленты. `nextCursor == nil` — дальше ничего нет.
nonisolated struct FeedPage: Sendable {
    let items: [FeedItem]
    let nextCursor: String?
}
