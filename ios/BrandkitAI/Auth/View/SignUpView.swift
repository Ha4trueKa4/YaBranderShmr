//
//  SignUpView.swift
//  BrandkitAI
//

import SwiftUI

struct SignUpView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var viewModel: SignUpViewModel

    init(viewModel: SignUpViewModel) {
        _viewModel = State(initialValue: viewModel)
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
                    autocapitalization: .words
                )

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
                    textContentType: .newPassword,
                    submitLabel: .go
                )
            }

            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }

            PrimaryButton(title: "Create account") {
                viewModel.register()
            }
            .padding(.top, 24)
            .disabled(viewModel.isLoading)

            ScreenFooter(
                question: "Already have an account?",
                actionTitle: "Sign in"
            ) {
                dismiss()
            }
            .padding(.top, 32)
        }
    }
}

#Preview {
    NavigationStack {
        SignUpView(viewModel: SignUpViewModel(session: SessionStore()))
    }
}
