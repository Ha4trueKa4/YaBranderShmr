import Foundation

/// Состояние формы входа и её действия.
///
/// Вью хранит только то, что нужно SwiftUI для отрисовки; ввод, валидация
/// и обращение к сессии живут здесь.
@MainActor
@Observable
final class SignInViewModel {
    var email = ""
    var password = ""
    private(set) var didAttemptSubmit = false
    private(set) var validationTrigger = 0

    private let session: SessionStore

    init(session: SessionStore) {
        self.session = session
    }

    var canSubmit: Bool {
        emailError == nil && passwordError == nil
    }

    var isLoading: Bool { session.isLoading }
    var errorMessage: String? { session.errorMessage }
    var errorField: SessionStore.AuthField? { session.errorField }
    var didSucceed: Bool { session.lastSuccessfulOperation == .signIn }

    var emailError: String? {
        let localError = didAttemptSubmit ? AuthValidation.email(email) : nil
        return localError ?? (errorField == .email ? errorMessage : nil)
    }

    var passwordError: String? {
        let localError = didAttemptSubmit ? AuthValidation.password(password, requiredStrength: false) : nil
        return localError ?? (errorField == .password ? errorMessage : nil)
    }

    var shouldShowGeneralError: Bool {
        errorMessage != nil && errorField == nil
    }

    func signIn() {
        didAttemptSubmit = true
        guard canSubmit else {
            validationTrigger += 1
            return
        }
        session.signIn(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            password: password
        )
    }

    func clearError() {
        session.clearError()
    }
}
