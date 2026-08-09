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
    var isShowingRegister = false

    private let session: SessionStore

    init(session: SessionStore) {
        self.session = session
    }

    var canSubmit: Bool {
        !email.trimmingCharacters(in: .whitespaces).isEmpty && !password.isEmpty
    }

    func signIn() {
        guard canSubmit else { return }
        session.signIn(email: email, password: password)
    }

    func openRegister() {
        isShowingRegister = true
    }

    /// Экран регистрации открывается отсюда, поэтому его модель собирается тут же.
    func makeSignUpViewModel() -> SignUpViewModel {
        SignUpViewModel(session: session)
    }
}
