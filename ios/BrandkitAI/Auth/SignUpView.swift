//
//  SignUpView.swift
//  BrandkitAI
//

import SwiftUI

struct SignUpView: View {
    var onSignInTap: () -> Void = {}

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

            PrimaryButton(title: "Create account") {}
                .padding(.top, 24)

            AuthFooter(
                question: "Already have an account?",
                actionTitle: "Sign in",
                action: onSignInTap
            )
            .padding(.top, 32)
        }
    }
}

#Preview {
    SignUpView()
}
