import Foundation

/// Проект пользователя в его профиле.
nonisolated struct UserProject: Sendable, Equatable, Identifiable {
    let id: UUID
    let name: String
    let url: URL?
}
