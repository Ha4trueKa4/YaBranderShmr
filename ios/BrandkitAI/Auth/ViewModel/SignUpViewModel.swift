import Foundation

/// Состояние формы регистрации и её действия.
@MainActor
@Observable
final class SignUpViewModel {
    var fullName = ""
    var email = ""
    var password = ""
    private(set) var didAttemptSubmit = false
    private(set) var validationTrigger = 0

    private let session: SessionStore

    init(session: SessionStore) {
        self.session = session
    }

    var canSubmit: Bool {
        fullNameError == nil && emailError == nil && passwordError == nil
    }

    var isLoading: Bool { session.isLoading }
    var isPasswordLoading: Bool { session.isLoading && session.activeOperation == .register }
    var isYandexLoading: Bool { session.isLoading && session.activeOperation == .yandexSignIn }
    var errorMessage: String? { session.errorMessage }
    var errorField: SessionStore.AuthField? { session.errorField }
    var didSucceed: Bool {
        session.lastSuccessfulOperation == .register || session.lastSuccessfulOperation == .yandexSignIn
    }

    var fullNameError: String? {
        let localError = didAttemptSubmit ? AuthValidation.fullName(fullName) : nil
        return localError ?? (errorField == .fullName ? errorMessage : nil)
    }

    var emailError: String? {
        let localError = didAttemptSubmit ? AuthValidation.email(email) : nil
        return localError ?? (errorField == .email ? errorMessage : nil)
    }

    var passwordError: String? {
        let localError = didAttemptSubmit ? AuthValidation.password(password, requiredStrength: true) : nil
        return localError ?? (errorField == .password ? errorMessage : nil)
    }

    var shouldShowGeneralError: Bool {
        errorMessage != nil && errorField == nil
    }

    func register() {
        didAttemptSubmit = true
        guard canSubmit else {
            validationTrigger += 1
            return
        }
        session.register(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            password: password,
            nickname: fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }

    func signInWithYandex() {
        session.signInWithYandex()
    }

    func clearError() {
        session.clearError()
    }
}
