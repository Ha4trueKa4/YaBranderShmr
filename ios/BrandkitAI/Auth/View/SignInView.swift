//
//  SignInView.swift
//  BrandkitAI
//

import SwiftUI

struct SignInView: View {
    @State private var viewModel: SignInViewModel

    init(session: SessionStore) {
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
                    keyboardType: .emailAddress
                )

                FormTextField(
                    label: "Password",
                    placeholder: "Enter your password",
                    text: $viewModel.password,
                    isSecure: true,
                    textContentType: .password,
                    submitLabel: .go
                )
            }

            PrimaryButton(title: "Sign in") {
                viewModel.signIn()
            }
            .padding(.top, 24)

            LinkButton(title: "Forgot password?") {}
                .padding(.top, 16)

            ScreenFooter(question: "No account?", actionTitle: "Register") {
                viewModel.openRegister()
            }
            .padding(.top, 32)
        }
        .navigationDestination(isPresented: $viewModel.isShowingRegister) {
            SignUpView(viewModel: viewModel.makeSignUpViewModel())
        }
    }
}

#Preview {
    NavigationStack {
        SignInView(session: SessionStore())
    }
}
