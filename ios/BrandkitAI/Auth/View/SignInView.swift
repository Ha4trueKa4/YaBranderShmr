import SwiftUI

struct SignInView: View {
    let onSignUp: () -> Void
    @State private var viewModel: SignInViewModel

    init(session: SessionStore, onSignUp: @escaping () -> Void = {}) {
        self.onSignUp = onSignUp
        _viewModel = State(initialValue: SignInViewModel(session: session))
    }

    var body: some View {
        @Bindable var viewModel = viewModel

        FormScreenLayout {
            ScreenHeader(title: "Sign in")
                .padding(.bottom, 32)

            VStack(spacing: 16) {
                FormTextField(
                    label: "Email",
                    placeholder: "hello@brandkit.ai",
                    text: $viewModel.email,
                    textContentType: .emailAddress,
                    keyboardType: .emailAddress,
                    errorMessage: viewModel.emailError
                )

                FormTextField(
                    label: "Password",
                    placeholder: "Enter your password",
                    text: $viewModel.password,
                    isSecure: true,
                    textContentType: .password,
                    submitLabel: .go,
                    errorMessage: viewModel.passwordError,
                    onSubmit: { viewModel.signIn() }
                )
            }

            if viewModel.shouldShowGeneralError, let errorMessage = viewModel.errorMessage {
                FormErrorView(message: errorMessage)
                    .padding(.top, 16)
            }

            PrimaryButton(
                title: "Sign in",
                isEnabled: !viewModel.isLoading,
                isLoading: viewModel.isPasswordLoading,
                fill: AnyShapeStyle(Color.accentColor),
                cornerRadius: 20
            ) {
                viewModel.signIn()
            }
            
            .padding(.top, 24)

            YandexIDButton(
                isEnabled: !viewModel.isLoading,
                isLoading: viewModel.isYandexLoading
            ) {
                viewModel.signInWithYandex()
            }
            .padding(.top, 12)

            ScreenFooter(question: "No account?", actionTitle: "Register") {
                viewModel.clearError()
                onSignUp()
            }
            .padding(.top, 24)
        }
        .onChange(of: viewModel.email) { viewModel.clearError() }
        .onChange(of: viewModel.password) { viewModel.clearError() }
        .animation(.easeOut(duration: 0.2), value: viewModel.errorMessage)
        .sensoryFeedback(.error, trigger: viewModel.validationTrigger)
        .sensoryFeedback(.error, trigger: viewModel.errorMessage)
        .sensoryFeedback(.success, trigger: viewModel.didSucceed)
    }
}

#Preview {
    NavigationStack {
        SignInView(session: SessionStore())
    }
    .tint(Color.accentColor)
}
