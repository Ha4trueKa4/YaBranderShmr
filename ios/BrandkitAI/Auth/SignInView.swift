//
//  SignInView.swift
//  BrandkitAI
//

import SwiftUI

struct SignInView: View {
    @Environment(SessionStore.self) private var session

    @State private var email = ""
    @State private var password = ""
    @State private var isShowingRegister = false

    var body: some View {
        AuthScreenLayout {
            AuthHeader(title: "Sign in")
                .padding(.bottom, 32)

            VStack(spacing: 16) {
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
                    textContentType: .password,
                    submitLabel: .go
                )
            }

            PrimaryButton(title: "Sign in") {
                session.signIn(email: email, password: password)
            }
            .padding(.top, 24)

            LinkButton(title: "Forgot password?") {}
                .padding(.top, 16)

            AuthFooter(question: "No account?", actionTitle: "Register") {
                isShowingRegister = true
            }
            .padding(.top, 32)
        }
        .navigationDestination(isPresented: $isShowingRegister) {
            SignUpView()
        }
    }
}

#Preview {
    NavigationStack {
        SignInView()
    }
    .environment(SessionStore())
}
