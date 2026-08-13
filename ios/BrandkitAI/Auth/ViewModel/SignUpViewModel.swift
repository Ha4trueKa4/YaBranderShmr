import Foundation

/// Состояние формы регистрации и её действия.
@MainActor
@Observable
final class SignUpViewModel {
    var fullName = ""
    var email = ""
    var password = ""

    private let session: SessionStore

    init(session: SessionStore) {
        self.session = session
    }

    var canSubmit: Bool {
        !fullName.trimmingCharacters(in: .whitespaces).isEmpty
            && !email.trimmingCharacters(in: .whitespaces).isEmpty
            && !password.isEmpty
    }

    var isLoading: Bool { session.isLoading }
    var errorMessage: String? { session.errorMessage }

    func register() {
        guard canSubmit else { return }
        session.register(email: email, password: password, nickname: fullName)
    }
}
