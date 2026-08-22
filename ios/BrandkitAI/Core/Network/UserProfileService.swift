import Foundation

nonisolated protocol UserProfileService: Sendable {
    func profile(for id: UUID) async throws -> UserProfile
    func subscribers(for userID: UUID) async throws -> [ProfileConnection]
    func subscriptions(for userID: UUID) async throws -> [ProfileConnection]
    func subscribe(to userID: UUID) async throws
    func unsubscribe(from userID: UUID) async throws
    func updateName(_ name: String, for id: UUID) async throws -> String
}
