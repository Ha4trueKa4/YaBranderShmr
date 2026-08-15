//
//  SignUpView.swift
//  BrandkitAI
//

import SwiftUI

struct SignUpView: View {
    let onSignIn: () -> Void
    @State private var viewModel: SignUpViewModel

    init(session: SessionStore, onSignIn: @escaping () -> Void = {}) {
        self.onSignIn = onSignIn
        _viewModel = State(initialValue: SignUpViewModel(session: session))
    }

    var body: some View {
        @Bindable var viewModel = viewModel

        FormScreenLayout {
            ScreenHeader(title: "Create account")
                .padding(.bottom, 32)

            VStack(spacing: 16) {
                FormTextField(
                    label: "Full name",
                    placeholder: "John Doe",
                    text: $viewModel.fullName,
                    textContentType: .name,
                    autocapitalization: .words,
                    errorMessage: viewModel.fullNameError
                )

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
                    textContentType: .newPassword,
                    submitLabel: .go,
                    errorMessage: viewModel.passwordError,
                    onSubmit: { viewModel.register() }
                )
            }

            if viewModel.shouldShowGeneralError, let errorMessage = viewModel.errorMessage {
                FormErrorView(message: errorMessage)
                    .padding(.top, 16)
            }

            PrimaryButton(
                title: "Create account",
                isEnabled: !viewModel.isLoading,
                isLoading: viewModel.isLoading
            ) {
                viewModel.register()
            }
            .padding(.top, 24)

            ScreenFooter(
                question: "Already have an account?",
                actionTitle: "Sign in"
            ) {
                viewModel.clearError()
                onSignIn()
            }
            .padding(.top, 32)
        }
        .onChange(of: viewModel.fullName) { viewModel.clearError() }
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
        SignUpView(session: SessionStore())
    }
    .tint(Color.accentColor)
}
