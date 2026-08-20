import Foundation

@MainActor
@Observable
final class ProjectCreateViewModel {
    var name = ""
    var brief = ""

    private(set) var isLoading = false
    private(set) var errorMessage: String?

    let title = "New Project"
    let hint = "Our AI will generate brand assets, palettes, and typography presets."

    private let service: any ProjectServiceProtocol

    init(service: any ProjectServiceProtocol = ProjectService()) {
        self.service = service
    }

    var canSubmit: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func submit() async -> Project? {
        guard canSubmit, !isLoading else { return nil }

        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            return try await service.createProject(
                name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                description: brief.trimmingCharacters(in: .whitespacesAndNewlines)
            )
        } catch {
            errorMessage = message(for: error)
            return nil
        }
    }

    private func message(for error: Error) -> String {
        if case NetworkError.httpError(let statusCode) = error {
            return "Server error (\(statusCode))"
        }
        if case NetworkError.serverValidation(_, _, let field) = error,
           field == "name" {
            return "Enter a valid project name."
        }
        return "Unable to create the project."
    }
}
