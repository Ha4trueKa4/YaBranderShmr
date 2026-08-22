import SwiftUI

@MainActor
@Observable
final class ProfileConnectionsViewModel {
    private(set) var users: [ProfileConnection] = []
    private(set) var busyUserIDs: Set<UUID> = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var currentUserID: UUID?

    private let currentUserService: any CurrentUserService
    private let userProfileService: any UserProfileService
    private let userID: UUID
    private let kind: ProfileConnectionsKind

    init(
        currentUserService: any CurrentUserService,
        userProfileService: any UserProfileService,
        userID: UUID,
        kind: ProfileConnectionsKind
    ) {
        self.currentUserService = currentUserService
        self.userProfileService = userProfileService
        self.userID = userID
        self.kind = kind
        self.currentUserID = currentUserService.id
    }

    func load() async {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        if currentUserService.id == nil {
            await currentUserService.restore()
        }
        currentUserID = currentUserService.id

        do {
            switch kind {
            case .followers:
                users = try await userProfileService.subscribers(for: userID)
            case .following:
                users = try await userProfileService.subscriptions(for: userID)
            }
        } catch {
            errorMessage = "Couldn't load \(kind.title.lowercased())."
        }
    }

    func canFollow(_ user: ProfileConnection) -> Bool {
        currentUserID != nil && user.id != currentUserID && user.isSubscribed != nil
    }

    func toggleFollowing(userID: UUID) {
        guard !busyUserIDs.contains(userID),
              let index = users.firstIndex(where: { $0.id == userID }),
              canFollow(users[index]),
              let previous = users[index].isSubscribed
        else { return }

        let target = !previous
        let service = userProfileService
        users[index].isSubscribed = target
        busyUserIDs.insert(userID)

        Task { @MainActor [weak self] in
            let succeeded: Bool
            do {
                if target {
                    try await service.subscribe(to: userID)
                } else {
                    try await service.unsubscribe(from: userID)
                }
                succeeded = true
            } catch {
                succeeded = false
            }

            guard let self else { return }
            if !succeeded,
               let index = self.users.firstIndex(where: { $0.id == userID }) {
                self.users[index].isSubscribed = previous
            }
            self.busyUserIDs.remove(userID)
        }
    }
}
