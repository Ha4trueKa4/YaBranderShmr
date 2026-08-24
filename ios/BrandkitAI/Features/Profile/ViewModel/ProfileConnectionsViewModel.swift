import SwiftUI

@MainActor
@Observable
final class ProfileConnectionsViewModel {
    private(set) var users: [ProfileConnection] = []
    private(set) var busyUserIDs: Set<UUID> = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var currentUserID: UUID?

    private var loadGeneration = 0
    private var pendingFollowUserID: UUID?

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
        // Каждый вызов перекрывает предыдущий: `.task(id: session.state)`
        // отменяет старую задачу при смене сессии, а устаревшая загрузка не
        // должна ни применить данные, ни показать ошибку отмены.
        loadGeneration += 1
        let generation = loadGeneration
        isLoading = true
        errorMessage = nil
        defer {
            if generation == loadGeneration { isLoading = false }
        }

        if currentUserService.id == nil {
            await currentUserService.restore()
        }
        guard generation == loadGeneration else { return }
        currentUserID = currentUserService.id

        do {
            let loaded: [ProfileConnection]
            switch kind {
            case .followers:
                loaded = try await userProfileService.subscribers(for: userID)
            case .following:
                loaded = try await userProfileService.subscriptions(for: userID)
            }
            guard generation == loadGeneration else { return }
            users = loaded
            consumePendingFollowToggle()
        } catch {
            guard generation == loadGeneration, !error.isTaskCancellation else { return }
            pendingFollowUserID = nil
            errorMessage = switch kind {
            case .followers: String(localized: "Couldn't load followers.")
            case .following: String(localized: "Couldn't load following.")
            }
        }
    }

    func canFollow(_ user: ProfileConnection) -> Bool {
        currentUserID != nil && user.id != currentUserID && user.isSubscribed != nil
    }

    /// Гостю кнопка видна всегда (тап уведёт в авторизацию), залогиненному —
    /// только когда сервер вернул состояние подписки и тап реально сработает.
    func showsFollowButton(for user: ProfileConnection) -> Bool {
        guard user.id != currentUserID else { return false }
        return currentUserID == nil || user.isSubscribed != nil
    }

    /// Тап по кнопке Follow в строке. Сразу после входа список ещё загружен
    /// по-гостевому (без состояний подписки), поэтому переключение
    /// откладывается до ближайшей успешной перезагрузки.
    func requestFollowToggle(userID: UUID) {
        if let index = users.firstIndex(where: { $0.id == userID }), canFollow(users[index]) {
            toggleFollowing(userID: userID)
        } else {
            pendingFollowUserID = userID
        }
    }

    private func consumePendingFollowToggle() {
        guard let userID = pendingFollowUserID else { return }
        pendingFollowUserID = nil
        toggleFollowing(userID: userID)
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
