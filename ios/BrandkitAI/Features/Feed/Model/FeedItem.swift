import Foundation

/// Элемент ленты — публичный проект.
nonisolated struct FeedItem: Identifiable, Hashable, Sendable {
    let id: UUID
    let authorID: UUID
    let authorName: String
    let title: String
    let createdAt: Date?
    let previewURL: URL?
}
