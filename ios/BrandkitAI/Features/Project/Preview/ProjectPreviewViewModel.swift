import Foundation
import SwiftUI

@MainActor
@Observable
final class ProjectPreviewViewModel {
    enum Mode: Equatable {
        case version(versionID: UUID, createdAt: Date?)
        case owned(imageURL: URL?)
        case publicProject
    }

    struct RelatedProject: Identifiable, Equatable {
        let id: UUID
        let title: String
        let previewURL: URL?
    }

    let projectID: UUID
    let mode: Mode

    private(set) var title: String
    private(set) var createdAt: Date?
    private(set) var imageURL: URL?
    private(set) var imageIsPrivate = false
    private(set) var versionID: UUID?
    private(set) var isLoading = false
    private(set) var loadFailed = false

    private(set) var authorID: UUID?
    private(set) var authorName: String = ""
    private(set) var isSubscribed = false
    private(set) var isFollowBusy = false
    private(set) var moreProjects: [RelatedProject] = []
    private(set) var isCurrentUser = false

    private var hasAuthenticatedViewer = false
    private var hasFollowState = false
    private var followOverride: Bool?
    private var followOperationID: UUID?
    private var followTask: Task<Void, Never>?
    private var loadGeneration = 0
    private var pendingFollowToggle = false

    private let projectService: any ProjectServiceProtocol
    private let userProfileService: any UserProfileService
    private let currentUserService: any CurrentUserService

    init(
        projectID: UUID,
        mode: Mode,
        currentUserService: any CurrentUserService,
        userProfileService: any UserProfileService,
        title: String = "",
        projectService: any ProjectServiceProtocol = ProjectService()
    ) {
        self.projectID = projectID
        self.mode = mode
        self.currentUserService = currentUserService
        self.userProfileService = userProfileService
        self.title = title
        self.projectService = projectService
        self.hasAuthenticatedViewer = currentUserService.id != nil

        switch mode {
        case .version(let versionID, let createdAt):
            self.versionID = versionID
            self.createdAt = createdAt
        case .owned(let imageURL):
            self.imageURL = imageURL
            self.imageIsPrivate = true
        case .publicProject:
            break
        }
    }

    var showsAuthorSection: Bool { mode == .publicProject }

    var canFollow: Bool {
        guard case .publicProject = mode,
              authorID != nil,
              hasAuthenticatedViewer,
              hasFollowState
        else { return false }
        return !isCurrentUser
    }

    /// Гостю кнопка видна всегда (тап уведёт в авторизацию), залогиненному —
    /// только когда сервер вернул состояние подписки и тап реально сработает.
    var showsFollowButton: Bool {
        guard case .publicProject = mode, authorID != nil, !isCurrentUser else { return false }
        return hasAuthenticatedViewer ? hasFollowState : true
    }

    var authorInitials: String { authorName.initials }

    var dateText: String {
        let relative = Self.relativeString(for: createdAt)
        return showsAuthorSection
            ? String(localized: "Published \(relative)")
            : String(localized: "Created \(relative)")
    }

    func versionImageURL() async throws -> URL {
        guard let versionID else { throw NetworkError.invalidResponse }
        return try await projectService.fetchVersionURL(projectID: projectID, versionID: versionID)
    }

    func refreshOwnedImageURL() async throws -> URL {
        guard case .owned = mode else { throw NetworkError.invalidResponse }
        let project = try await projectService.fetchProject(id: projectID)
        try Task.checkCancellation()
        applyOwnedProject(project)
        guard let imageURL else { throw NetworkError.invalidResponse }
        return imageURL
    }

    func exportSVGData() async throws -> Data {
        switch mode {
        case .version:
            guard let versionID else { throw NetworkError.invalidResponse }
            let key = SVGCache.versionKey(projectID: projectID, versionID: versionID)
            if let cached = await SVGCache.shared.dataIfCached(for: key) {
                return cached
            }
            let url = try await versionImageURL()
            return try await SVGCache.shared.data(for: url, key: key)

        case .owned:
            let url: URL
            if let imageURL, !(imageIsPrivate && URLSignature.isExpired(imageURL)) {
                url = imageURL
            } else {
                url = try await refreshOwnedImageURL()
            }
            return try await cachedData(for: url, isPrivate: imageIsPrivate)

        case .publicProject:
            guard let imageURL else { throw NetworkError.invalidResponse }
            return try await cachedData(for: imageURL, isPrivate: false)
        }
    }

    private func cachedData(for url: URL, isPrivate: Bool) async throws -> Data {
        let source = SVGDocumentView.Source.url(url, isPrivate: isPrivate)
        let key = source.cacheKey ?? url.absoluteString
        return try await SVGCache.shared.data(for: url, key: key)
    }

    func load() async {
        // Каждый вызов перекрывает предыдущий: `.task(id: session.state)`
        // отменяет старую задачу при смене сессии, а устаревшая загрузка не
        // должна ни применить данные, ни показать ошибку отмены.
        loadGeneration += 1
        let generation = loadGeneration
        isLoading = true
        loadFailed = false
        defer {
            if generation == loadGeneration { isLoading = false }
        }

        switch mode {
        case .version:
            break
        case .owned:
            do {
                let project = try await projectService.fetchProject(id: projectID)
                try Task.checkCancellation()
                guard generation == loadGeneration else { return }
                applyOwnedProject(project)
            } catch {
                guard generation == loadGeneration, !error.isTaskCancellation else { return }
                loadFailed = true
            }
        case .publicProject:
            await loadPublicProject(generation: generation)
        }
    }

    /// Тап по кнопке Follow. Сразу после входа данные автора ещё загружены
    /// по-гостевому (без состояния подписки), поэтому переключение
    /// откладывается до ближайшей успешной перезагрузки.
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
              let authorID,
              !isFollowBusy,
              followTask == nil
        else { return }

        let operationID = UUID()
        let previous = isSubscribed
        let previousOverride = followOverride
        let target = !previous
        let service = userProfileService

        followOperationID = operationID
        followOverride = target
        isSubscribed = target
        isFollowBusy = true

        followTask = Task { @MainActor [weak self] in
            let succeeded: Bool
            do {
                if target {
                    try await service.subscribe(to: authorID)
                } else {
                    try await service.unsubscribe(from: authorID)
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
                self.followOverride = previousOverride
            }

            self.isFollowBusy = false
            self.followOperationID = nil
            self.followTask = nil
        }
    }

    private func loadPublicProject(generation: Int) async {
        let project: Project
        do {
            project = try await projectService.fetchProject(id: projectID)
            try Task.checkCancellation()
        } catch {
            guard generation == loadGeneration, !error.isTaskCancellation else { return }
            loadFailed = true
            return
        }
        guard generation == loadGeneration else { return }

        title = project.name
        createdAt = project.createdAt
        imageURL = project.url
        authorID = project.authorID

        guard let authorID else { return }

        if currentUserService.id == nil {
            await currentUserService.restore()
        }
        guard !Task.isCancelled else { return }

        let viewerID = currentUserService.id
        hasAuthenticatedViewer = viewerID != nil
        isCurrentUser = viewerID == authorID

        do {
            let profile = try await userProfileService.profile(for: authorID)
            try Task.checkCancellation()
            guard generation == loadGeneration else { return }

            authorName = profile.name
            moreProjects = profile.projects
                .filter { $0.id != projectID }
                .map { RelatedProject(id: $0.id, title: $0.name, previewURL: $0.url) }
            mergeRemoteFollow(profile.isSubscribed)
            consumePendingFollowToggle()
        } catch is CancellationError {
            return
        } catch {
            // Project data remains usable; follow stays hidden until its state is known.
            pendingFollowToggle = false
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

    private func applyOwnedProject(_ project: Project) {
        title = project.name
        createdAt = project.createdAt
        imageIsPrivate = !project.isPublic
        imageURL = project.url
    }

    private static func relativeString(for date: Date?) -> String {
        guard let date else { return String(localized: "just now") }
        return date.formatted(.relative(presentation: .named, unitsStyle: .wide))
    }
}
