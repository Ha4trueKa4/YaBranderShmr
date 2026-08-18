import Foundation

// MARK: - DTO (контракт GET /feed)

nonisolated struct FeedResponseDTO: Decodable, Sendable {
    let items: [FeedItemDTO]
    let nextCursor: String?
}

nonisolated struct FeedItemDTO: Decodable, Sendable {
    let id: String
    let authorId: String
    let authorLogin: String
    let name: String
    let createdAt: String
    let previewUrl: String
}

// MARK: - Маппинг DTO → домен

extension FeedPage {
    nonisolated init(dto: FeedResponseDTO) {
        items = dto.items.compactMap(FeedItem.init(dto:))
        nextCursor = dto.nextCursor
    }
}

extension FeedItem {
    nonisolated(unsafe) private static let isoFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    nonisolated(unsafe) private static let isoFormatterNoFraction: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    nonisolated init?(dto: FeedItemDTO) {
        guard let id = UUID(uuidString: dto.id),
              let authorID = UUID(uuidString: dto.authorId)
        else { return nil }

        self.id = id
        self.authorID = authorID
        authorName = dto.authorLogin
        title = dto.name
        createdAt = Self.isoFormatter.date(from: dto.createdAt)
            ?? Self.isoFormatterNoFraction.date(from: dto.createdAt)
        previewURL = dto.previewUrl.isEmpty ? nil : URL(string: dto.previewUrl)
    }
}
