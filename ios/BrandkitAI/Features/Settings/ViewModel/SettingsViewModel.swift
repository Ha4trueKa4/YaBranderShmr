import Foundation

@MainActor
@Observable
final class SettingsViewModel {
    private(set) var isSavingName = false
    private(set) var nameError: String?

    private let currentUserService: any CurrentUserService
    private let userProfileService: any UserProfileService

    init(
        currentUserService: any CurrentUserService,
        userProfileService: any UserProfileService
    ) {
        self.currentUserService = currentUserService
        self.userProfileService = userProfileService
    }

    var name: String { currentUserService.name ?? "" }
    var email: String? { currentUserService.email }

    func clearNameError() {
        nameError = nil
    }

    func saveName(_ newName: String) async -> Bool {
        let trimmed = newName.trimmingCharacters(in: .whitespacesAndNewlines)

        guard trimmed != name else { return true }

        guard (3...64).contains(trimmed.count) else {
            nameError = String(localized: "Name must be 3–64 characters.")
            return false
        }

        guard let id = currentUserService.id else {
            nameError = String(localized: "Please sign in again.")
            return false
        }

        isSavingName = true
        nameError = nil
        defer { isSavingName = false }

        do {
            let savedName = try await userProfileService.updateName(trimmed, for: id)
            currentUserService.update(name: savedName)
            return true
        } catch {
            nameError = Self.message(for: error)
            return false
        }
    }

    private static func message(for error: Error) -> String {
        if case NetworkError.serverValidation(_, let code, _) = error {
            switch code {
            case "NICKNAME_EXISTS":
                return String(localized: "This name is already taken.")
            case "INVALID_NICKNAME":
                return String(localized: "Name must be 3–64 characters.")
            default:
                break
            }
        }
        if case NetworkError.missingAccessToken = error {
            return String(localized: "Please sign in again.")
        }
        return String(localized: "Couldn't update the name. Try again.")
    }
}
