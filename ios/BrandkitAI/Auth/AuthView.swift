//
//  AuthView.swift
//  BrandkitAI
//
//  Created by qppon on 08.08.2026.
//

import SwiftUI

struct AuthView: View {
    private enum Mode {
        case signIn
        case signUp
    }

    @State private var mode: Mode = .signIn

    var body: some View {
        ZStack {
            switch mode {
            case .signIn:
                SignInView(onRegisterTap: { switchTo(.signUp) })
                    .transition(.opacity)
            case .signUp:
                SignUpView(onSignInTap: { switchTo(.signIn) })
                    .transition(.opacity)
            }
        }
        // Акцент макета. TODO: перенести в AccentColor в Assets, когда будет палитра.
        .tint(.purple)
    }

    private func switchTo(_ newMode: Mode) {
        withAnimation(.easeInOut(duration: 0.2)) {
            mode = newMode
        }
    }
}

#Preview("Sign in") {
    AuthView()
}

#Preview("Create account") {
    SignUpView()
}
