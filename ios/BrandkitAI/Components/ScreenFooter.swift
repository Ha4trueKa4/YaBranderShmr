import SwiftUI

struct ScreenFooter: View {
    let question: String
    let actionTitle: String
    let action: () -> Void

    var body: some View {
        VStack(spacing: 10) {
            Text(question)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            LinkButton(title: actionTitle, action: action)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    ScreenFooter(question: "No account?", actionTitle: "Register") {}
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .preferredColorScheme(.dark)
}
