//
//  SignUpView.swift
//  BrandkitAI
//

import SwiftUI

struct SignUpView: View {
    @Environment(SessionStore.self) private var session
    @Environment(\.dismiss) private var dismiss

    @State private var fullName = ""
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        AuthScreenLayout {
            AuthHeader(title: "Create account")
                .padding(.bottom, 32)

            VStack(spacing: 16) {
                AuthField(
                    label: "Full name",
                    placeholder: "John Doe",
                    text: $fullName,
                    textContentType: .name,
                    autocapitalization: .words
                )

                AuthField(
                    label: "Email",
                    placeholder: "hello@brandkit.ai",
                    text: $email,
                    textContentType: .emailAddress,
                    keyboardType: .emailAddress
                )

                AuthField(
                    label: "Password",
                    placeholder: "Enter your password",
                    text: $password,
                    isSecure: true,
                    textContentType: .newPassword,
                    submitLabel: .go
                )
            }

            PrimaryButton(title: "Create account") {
                session.register(email: email, password: password, nickname: fullName)
            }
            .padding(.top, 24)

            AuthFooter(
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
        SignUpView()
    }
    .environment(SessionStore())
}
