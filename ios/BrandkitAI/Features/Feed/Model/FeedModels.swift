import Foundation

// MARK: - Domain

/// Раздел ленты в терминах API: `scope` запроса `GET /feed`.
nonisolated enum FeedScope: String, Sendable {
    case discover
    case following
}

/// Элемент ленты — публичный проект.
nonisolated struct FeedItem: Identifiable, Hashable, Sendable {
    let id: UUID
    let authorID: UUID
    let authorName: String
    let title: String
    let createdAt: Date?
    let previewURL: URL?
}

/// Страница ленты. `nextCursor == nil` — дальше ничего нет.
nonisolated struct FeedPage: Sendable {
    let items: [FeedItem]
    let nextCursor: String?
}

// MARK: - DTO (контракт GET /feed)

nonisolated struct FeedResponseDTO: Decodable, Sendable {
    let items: [FeedItemDTO]
    let nextCursor: String?

    enum CodingKeys: String, CodingKey {
        case items
        case nextCursor = "next_cursor"
    }
}

nonisolated struct FeedItemDTO: Decodable, Sendable {
    let id: String
    let authorID: String
    let authorLogin: String
    let name: String
    let createdAt: String
    let previewURL: String

    enum CodingKeys: String, CodingKey {
        case id
        case authorID = "author_id"
        case authorLogin = "author_login"
        case name
        case createdAt = "created_at"
        case previewURL = "preview_url"
    }
}

// MARK: - Mapping

extension FeedPage {
    /// Элементы с некорректными UUID пропускаются, а не роняют всю страницу.
    init(dto: FeedResponseDTO) {
        items = dto.items.compactMap(FeedItem.init(dto:))
        nextCursor = dto.nextCursor
    }
}

extension FeedItem {
    nonisolated private static let isoFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    nonisolated private static let isoFormatterNoFraction: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    init?(dto: FeedItemDTO) {
        guard let id = UUID(uuidString: dto.id),
              let authorID = UUID(uuidString: dto.authorID)
        else { return nil }

        self.id = id
        self.authorID = authorID
        authorName = dto.authorLogin
        title = dto.name
        createdAt = Self.isoFormatter.date(from: dto.createdAt)
            ?? Self.isoFormatterNoFraction.date(from: dto.createdAt)
        previewURL = dto.previewURL.isEmpty ? nil : URL(string: dto.previewURL)
    }
}
