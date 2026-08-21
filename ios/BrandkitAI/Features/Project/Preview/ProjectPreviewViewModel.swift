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
    private(set) var versionID: UUID?
    private(set) var isLoading = false
    private(set) var loadFailed = false

    private(set) var authorID: UUID?
    private(set) var authorName: String = ""
    private(set) var isSubscribed = false
    private(set) var isFollowBusy = false
    private(set) var moreProjects: [RelatedProject] = []
    private(set) var isCurrentUser = false

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

        switch mode {
        case .version(let versionID, let createdAt):
            self.versionID = versionID
            self.createdAt = createdAt
        case .owned(let imageURL):
            self.imageURL = imageURL
        case .publicProject:
            break
        }
    }

    var showsAuthorSection: Bool { mode == .publicProject }

    var canFollow: Bool {
        guard case .publicProject = mode else { return false }
        return authorID != nil && !isCurrentUser
    }

    var authorInitials: String { authorName.initials }

    var dateText: String {
        let prefix = showsAuthorSection ? "Published" : "Created"
        return "\(prefix) \(Self.relativeString(for: createdAt))"
    }

    func versionImageURL() async throws -> URL {
        guard let versionID else { throw NetworkError.invalidResponse }
        return try await projectService.fetchVersionURL(projectID: projectID, versionID: versionID)
    }

    func load() async {
        guard !isLoading else { return }
        isLoading = true
        loadFailed = false
        defer { isLoading = false }

        switch mode {
        case .version:
            break
        case .owned:
            do {
                let project = try await projectService.fetchProject(id: projectID)
                if title.isEmpty { title = project.name }
                createdAt = project.createdAt
                if imageURL == nil { imageURL = project.url }
            } catch {
                loadFailed = true
            }
        case .publicProject:
            do {
                let project = try await projectService.fetchProject(id: projectID)
                title = project.name
                createdAt = project.createdAt
                imageURL = project.url
                authorID = project.authorID
                if let authorID {
                    let profile = try await userProfileService.profile(for: authorID)
                    authorName = profile.name
                    isSubscribed = profile.isSubscribed ?? false
                    moreProjects = profile.projects
                        .filter { $0.id != projectID }
                        .map { RelatedProject(id: $0.id, title: $0.name, previewURL: $0.url) }
                    isCurrentUser = currentUserService.id == authorID
                }
            } catch {
                loadFailed = true
            }
        }
    }

    func toggleFollowing() {
        guard canFollow, let authorID, !isFollowBusy else { return }
        let previous = isSubscribed
        isSubscribed.toggle()
        isFollowBusy = true

        Task { @MainActor in
            defer { isFollowBusy = false }
            do {
                if isSubscribed {
                    try await userProfileService.subscribe(to: authorID)
                } else {
                    try await userProfileService.unsubscribe(from: authorID)
                }
            } catch {
                isSubscribed = previous
            }
        }
    }

    private static func relativeString(for date: Date?) -> String {
        guard let date else { return "just now" }
        return date.formatted(.relative(presentation: .named, unitsStyle: .wide))
    }
}
