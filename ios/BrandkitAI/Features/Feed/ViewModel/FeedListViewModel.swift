import Foundation

@MainActor
@Observable
final class FeedListViewModel {
    private static let pageSize = 20
    
    private static let prefetchDistance = 4

    private(set) var items: [FeedItem] = []
    private(set) var isLoading = false
    private(set) var isLoadingMore = false
    private(set) var errorMessage: String?

    private var nextCursor: String?
    private var hasMore = true
    private var generation = 0
    private let scope: FeedScope
    private let service: any FeedServiceProtocol

    init(scope: FeedScope, service: any FeedServiceProtocol = FeedService()) {
        self.scope = scope
        self.service = service
    }

    var isEmpty: Bool { items.isEmpty }

    func loadInitial() async {
        guard items.isEmpty, !isLoading else { return }
        await reload()
    }

    func refresh() async {
        guard !isLoading else { return }
        await reload()
    }

    func loadMoreIfNeeded(after item: FeedItem) {
        guard hasMore, !isLoading, !isLoadingMore,
              items.suffix(Self.prefetchDistance).contains(item)
        else { return }

        let startedGeneration = generation
        isLoadingMore = true
        Task {
            defer { isLoadingMore = false }
            do {
                let page = try await service.fetchPage(
                    scope: scope,
                    cursor: nextCursor,
                    limit: Self.pageSize
                )
                guard startedGeneration == generation else { return }

                let knownIDs = Set(items.map(\.id))
                items += page.items.filter { !knownIDs.contains($0.id) }
                apply(cursor: page.nextCursor)
            } catch {
                
            }
        }
    }

    private func reload() async {
        generation += 1
        isLoading = true
        errorMessage = nil
        do {
            let page = try await service.fetchPage(scope: scope, cursor: nil, limit: Self.pageSize)
            items = page.items
            apply(cursor: page.nextCursor)
        } catch is CancellationError {
            // Экран ушёл — молча сворачиваемся.
        } catch {
            if items.isEmpty {
                errorMessage = Self.message(for: error)
            }
        }
        isLoading = false
    }

    private func apply(cursor: String?) {
        nextCursor = cursor
        hasMore = cursor != nil
    }

    private static func message(for error: Error) -> String {
        if case NetworkError.httpError(let statusCode) = error {
            switch statusCode {
            case 401:
                return "Sign in to see your subscriptions"
            default:
                return "Server error (\(statusCode))"
            }
        }
        return "Unable to connect to the server"
    }
}
