import Foundation

nonisolated struct Project: Identifiable, Equatable, Sendable {
    let id: UUID
    let authorID: UUID?
    let name: String
    let description: String
    let isPublic: Bool
    let createdAt: Date?
    let url: URL?
}
