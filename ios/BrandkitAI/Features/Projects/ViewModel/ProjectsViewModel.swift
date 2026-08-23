import Foundation
import SwiftUI

@MainActor
@Observable
final class ProjectsViewModel {
    private(set) var items: [ProjectListItem] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    let title = "Projects"

    private let currentUserService: any CurrentUserService
    private let service: any ProjectServiceProtocol

    init(
        currentUserService: any CurrentUserService,
        service: any ProjectServiceProtocol = ProjectService()
    ) {
        self.currentUserService = currentUserService
        self.service = service
    }

    var isEmpty: Bool { items.isEmpty }

    func load() async {
        guard !isLoading else { return }
        await reload()
    }

    func refresh() async {
        guard !isLoading else { return }
        await reload()
    }

    func clearError() {
        errorMessage = nil
    }

    func delete(at offsets: IndexSet) {
        let deletedItems = offsets.map { items[$0] }
        items.remove(atOffsets: offsets)

        deleteFromServer(deletedItems)
    }

    func delete(_ item: ProjectListItem) {
        guard let index = items.firstIndex(where: { $0.id == item.id }) else { return }
        items.remove(at: index)

        deleteFromServer([item])
    }

    private func deleteFromServer(_ deletedItems: [ProjectListItem]) {
        Task { @MainActor in
            do {
                for item in deletedItems {
                    try await service.deleteProject(id: item.id)
                }
            } catch {
                errorMessage = message(for: error)
                await reload()
            }
        }
    }

    func refreshPreview(for projectID: UUID) async throws -> URL {
        let project = try await service.fetchProject(id: projectID)
        guard let url = project.url else { throw NetworkError.invalidResponse }
        return url
    }

    private func reload() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        await currentUserService.restore()

        guard let userID = currentUserService.id else {
            errorMessage = "Unable to identify the current user."
            return
        }

        do {
            let projects = try await service.fetchProjects(for: userID)
            items = projects.enumerated().map { index, project in
                ProjectListItem(
                    id: project.id,
                    title: project.name,
                    subtitle: project.isPublic ? "Public project" : "Private project",
                    previewURL: project.url,
                    isPrivate: !project.isPublic,
                    placeholderColors: Self.palette(for: project.name, index: index)
                )
            }
        } catch {
            if error.isTaskCancellation { return }
            let nsError = error as NSError
            print("[ProjectsViewModel] Failed to load projects: \(error)")
            print("[ProjectsViewModel] NSError domain=\(nsError.domain), code=\(nsError.code), description=\(nsError.localizedDescription)")
            errorMessage = message(for: error)
        }
    }

    private func message(for error: Error) -> String {
        if case NetworkError.httpError(let statusCode) = error {
            return "Server error (\(statusCode))"
        }
        if case NetworkError.missingAccessToken = error {
            return "Please sign in again."
        }
        return "Unable to load projects."
    }

    private static let palettes: [[Color]] = [
        [.pink, .purple],
        [.green, .blue],
        [.purple, .green],
        [.orange, .pink],
        [.cyan, .blue]
    ]

    private static func palette(for title: String, index: Int) -> [Color] {
        let seed = title.unicodeScalars.reduce(index) { $0 &+ Int($1.value) }
        return palettes[abs(seed) % palettes.count]
    }
}
