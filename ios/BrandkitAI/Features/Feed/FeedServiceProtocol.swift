import Foundation

nonisolated protocol FeedServiceProtocol: Sendable {
    func fetchPage(scope: FeedScope, cursor: String?, limit: Int) async throws -> FeedPage
}
