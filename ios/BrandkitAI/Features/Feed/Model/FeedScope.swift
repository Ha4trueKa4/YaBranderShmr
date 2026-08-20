import Foundation

/// Раздел ленты в терминах API: `scope` запроса `GET /feed`.
nonisolated enum FeedScope: String, Sendable {
    case discover
    case following
}
