import Foundation

nonisolated protocol UserProfileService: Sendable {
    func profile(for id: UUID) async throws -> UserProfile
    func subscribe(to userID: UUID) async throws
    func unsubscribe(from userID: UUID) async throws
    func updateName(_ name: String, for id: UUID) async throws -> String
}
