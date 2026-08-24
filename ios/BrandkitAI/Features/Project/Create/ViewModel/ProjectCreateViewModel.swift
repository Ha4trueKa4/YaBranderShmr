import Foundation

@MainActor
@Observable
final class ProjectCreateViewModel {
    static let maxNameLength = 255
    static let maxPromptLength = 5_000

    var name = ""
    var prompt = ""

    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var availableModels: [GenerationModel] = []
    private(set) var isLoadingModels = false
    private(set) var modelsLoadFailed = false
    private(set) var submissionIsUncertain = false
    private(set) var selectedModelID: String

    let title = "New Project"
    let hint = "Your prompt will become the first message in the project chat and start generation."

    private let service: any ProjectServiceProtocol

    init(service: any ProjectServiceProtocol = ProjectService()) {
        self.service = service
        self.selectedModelID = UserDefaults.standard.string(
            forKey: GenerationModel.selectionStorageKey
        ) ?? GenerationModel.defaultID
    }

    var selectedModel: GenerationModel {
        availableModels.first(where: { $0.id == selectedModelID })
            ?? GenerationModel(remoteID: selectedModelID, owner: nil)
    }

    var canSubmit: Bool {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmedName.isEmpty
            && trimmedName.unicodeScalars.count <= Self.maxNameLength
            && !trimmedPrompt.isEmpty
            && trimmedPrompt.unicodeScalars.count <= Self.maxPromptLength
            && !isLoading
            && !isLoadingModels
            && !modelsLoadFailed
            && !submissionIsUncertain
            && availableModels.contains(where: { $0.id == selectedModelID })
    }

    var nameValidationMessage: String? {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmedName.unicodeScalars.count > Self.maxNameLength else { return nil }
        return "Project name is too long (maximum 255 characters)."
    }

    var promptValidationMessage: String? {
        let trimmedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmedPrompt.unicodeScalars.count > Self.maxPromptLength else { return nil }
        return "Prompt is too long (maximum 5000 characters)."
    }

    func loadModels(force: Bool = false) async {
        guard !isLoadingModels else { return }
        guard force || availableModels.isEmpty else { return }

        isLoadingModels = true
        modelsLoadFailed = false
        defer { isLoadingModels = false }

        do {
            let models = try await service.fetchModels()
            guard !models.isEmpty else { throw NetworkError.invalidResponse }
            availableModels = models

            if !models.contains(where: { $0.id == selectedModelID }) {
                selectedModelID = models.first(where: { $0.id == GenerationModel.defaultID })?.id
                    ?? models[0].id
                persistSelectedModel()
            }
        } catch {
            modelsLoadFailed = true
        }
    }

    func selectModel(_ model: GenerationModel) {
        selectedModelID = model.id
        persistSelectedModel()
    }

    func submit() async -> ProjectCreationResult? {
        guard canSubmit else { return nil }

        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            return try await service.createProject(
                name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                prompt: prompt.trimmingCharacters(in: .whitespacesAndNewlines),
                modelID: selectedModel.id
            )
        } catch {
            if Self.isUncertainSubmission(error) {
                submissionIsUncertain = true
            }
            errorMessage = message(for: error)
            return nil
        }
    }

    private func persistSelectedModel() {
        UserDefaults.standard.set(selectedModelID, forKey: GenerationModel.selectionStorageKey)
    }

    private func message(for error: Error) -> String {
        if Self.isUncertainSubmission(error) {
            return "The result is uncertain. Return to the project list and refresh before trying again."
        }
        if case NetworkError.httpError(let statusCode) = error,
           statusCode == 400 {
            return "Enter a prompt up to 5000 characters."
        }
        if case NetworkError.httpError(let statusCode) = error {
            return "Server error (\(statusCode))"
        }
        if case NetworkError.serverValidation(_, _, let field) = error,
           field == "name" {
            return "Enter a valid project name."
        }
        if case NetworkError.serverValidation(_, _, let field) = error,
           field == "prompt" {
            return "Enter a prompt up to 5000 characters."
        }
        return "Unable to create the project."
    }

    private static func isUncertainSubmission(_ error: Error) -> Bool {
        if error is CancellationError { return true }
        if let urlError = error as? URLError {
            return [.timedOut, .networkConnectionLost, .cancelled].contains(urlError.code)
        }
        guard let networkError = error as? NetworkError else { return false }
        switch networkError {
        case .decodingError, .invalidResponse:
            return true
        default:
            return false
        }
    }
}
