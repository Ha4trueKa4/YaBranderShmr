//
//  AuthHeader.swift
//  BrandkitAI
//

import SwiftUI


/// TODO: заменить содержимое `overlay` на реальный логотип, когда появится ассет:
///       `Image("Logo").resizable().scaledToFit().padding(8)`
struct LogoBadge: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(.tint.opacity(0.2))
            .frame(width: 60, height: 60)
            .overlay {
                Image(systemName: "swift").resizable().scaledToFit().padding(8)
            }
            .accessibilityHidden(true)
    }
}

struct AuthHeader: View {
    let title: String

    var body: some View {
        VStack(spacing: 20) {
            LogoBadge()

            Text(title)
                .font(.title.bold())
        }
    }
}

struct AuthFooter: View {
    let question: String
    let actionTitle: String
    let action: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Text(question)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            LinkButton(title: actionTitle, action: action)
        }
    }
}

#Preview {
    VStack(spacing: 40) {
        AuthHeader(title: "Sign in")
        AuthFooter(question: "No account?", actionTitle: "Register") {}
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color(.systemBackground))
    .tint(.purple)
    .preferredColorScheme(.dark)
}
