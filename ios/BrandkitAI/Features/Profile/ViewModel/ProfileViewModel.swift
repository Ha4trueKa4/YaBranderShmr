import SwiftUI

@MainActor
@Observable
final class ProfileViewModel {
    struct Project: Identifiable {
        let id: UUID
        let title: String
        let colors: [Color]
        let coverHeight: CGFloat
        let previewURL: URL?
    }

    private var loadedName = ""
    private(set) var projectsCount = 0
    private(set) var followersCount = 0
    private(set) var followingCount = 0
    private(set) var projects: [Project] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?
    /// Идентификатор показываемого профиля: нужен экранам подписчиков/подписок.
    private(set) var profileID: UUID?

    private(set) var isSubscribed = false
    private(set) var isFollowBusy = false
    private(set) var isCurrentUser = false

    private var hasAuthenticatedViewer = false
    private var hasFollowState = false
    private var followOverride: Bool?
    private var followOperationID: UUID?
    private var followTask: Task<Void, Never>?
    private var loadGeneration = 0
    private var pendingFollowToggle = false

    private let currentUserService: any CurrentUserService
    private let userProfileService: any UserProfileService
    private let userID: UUID?

    init(
        currentUserService: any CurrentUserService,
        userProfileService: any UserProfileService,
        userID: UUID? = nil
    ) {
        self.currentUserService = currentUserService
        self.userProfileService = userProfileService
        self.userID = userID
        self.profileID = userID ?? currentUserService.id
        self.hasAuthenticatedViewer = currentUserService.id != nil
    }

    var name: String {
        if userID == nil, let currentName = currentUserService.name {
            return currentName
        }
        return loadedName
    }

    var initials: String {
        name
            .split(separator: " ")
            .prefix(2)
            .compactMap(\.first)
            .map(String.init)
            .joined()
    }

    var canFollow: Bool {
        guard userID != nil,
              hasAuthenticatedViewer,
              hasFollowState
        else { return false }
        return !isCurrentUser
    }

    /// Гостю кнопка видна всегда (тап уведёт в авторизацию), залогиненному —
    /// только когда сервер вернул состояние подписки и тап реально сработает.
    var showsFollowButton: Bool {
        guard userID != nil, !isCurrentUser else { return false }
        return hasAuthenticatedViewer ? hasFollowState : true
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

        guard let id = userID ?? currentUserService.id else { return }
        profileID = id

        if userID != nil {
            let viewerID = currentUserService.id
            hasAuthenticatedViewer = viewerID != nil
            isCurrentUser = viewerID == userID
        }

        do {
            let profile = try await userProfileService.profile(for: id)
            guard generation == loadGeneration else { return }
            loadedName = profile.name

            if userID == nil {
                currentUserService.update(name: profile.name)
            }

            followersCount = profile.subscribersCount
            followingCount = profile.subscriptionsCount
            projectsCount = profile.projects.count
            projects = profile.projects.enumerated().map { offset, project in
                Project(
                    id: project.id,
                    title: project.name,
                    colors: Self.palette(for: project.name),
                    coverHeight: Self.coverHeight(for: offset),
                    previewURL: project.url
                )
            }
            mergeRemoteFollow(profile.isSubscribed)
            consumePendingFollowToggle()
        } catch {
            guard generation == loadGeneration, !error.isTaskCancellation else { return }
            pendingFollowToggle = false
            errorMessage = "Couldn't load profile."
        }
    }

    /// Тап по кнопке Follow. Сразу после входа профиль ещё загружен по-гостевому
    /// (без состояния подписки), поэтому переключение откладывается до
    /// ближайшей успешной перезагрузки.
    func requestFollowToggle() {
        if canFollow {
            toggleFollowing()
        } else {
            pendingFollowToggle = true
        }
    }

    private func consumePendingFollowToggle() {
        guard pendingFollowToggle else { return }
        pendingFollowToggle = false
        toggleFollowing()
    }

    func toggleFollowing() {
        guard canFollow,
              let id = userID,
              !isFollowBusy,
              followTask == nil
        else { return }

        let operationID = UUID()
        let previous = isSubscribed
        let previousCount = followersCount
        let previousOverride = followOverride
        let target = !previous
        let service = userProfileService

        followOperationID = operationID
        followOverride = target
        isSubscribed = target
        followersCount = max(0, previousCount + (target ? 1 : -1))
        isFollowBusy = true

        followTask = Task { @MainActor [weak self] in
            let succeeded: Bool
            do {
                if target {
                    try await service.subscribe(to: id)
                } else {
                    try await service.unsubscribe(from: id)
                }
                succeeded = true
            } catch {
                succeeded = false
            }

            guard let self, self.followOperationID == operationID else { return }

            if succeeded {
                self.isSubscribed = target
                self.followOverride = target
            } else {
                self.isSubscribed = previous
                self.followersCount = previousCount
                self.followOverride = previousOverride
            }

            self.isFollowBusy = false
            self.followOperationID = nil
            self.followTask = nil
        }
    }

    private func mergeRemoteFollow(_ remote: Bool?) {
        guard let remote else { return }
        hasFollowState = true

        if let followOverride {
            isSubscribed = followOverride
            if !isFollowBusy, remote == followOverride {
                self.followOverride = nil
            }
        } else {
            isSubscribed = remote
        }
    }

    private static let palettes: [[Color]] = [
        [.purple, .indigo],
        [.pink, Color(red: 0.45, green: 0.02, blue: 0.22)],
        [.cyan, Color(red: 0.02, green: 0.12, blue: 0.20)],
        [.orange, Color(red: 0.35, green: 0.13, blue: 0.01)],
        [.blue, .purple],
        [.yellow, .orange],
        [.mint, .blue],
        [.red, .purple]
    ]

    private static let coverHeights: [CGFloat] = [136, 164, 124, 112, 172, 138, 118, 158]

    private static func palette(for title: String) -> [Color] {
        let seed = title.unicodeScalars.reduce(0) { ($0 &+ Int($1.value)) % palettes.count }
        return palettes[seed]
    }

    private static func coverHeight(for offset: Int) -> CGFloat {
        coverHeights[offset % coverHeights.count]
    }
}
