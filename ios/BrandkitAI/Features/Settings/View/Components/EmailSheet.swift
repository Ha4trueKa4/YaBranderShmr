import SwiftUI

struct EmailSheet: View {
    let email: String?

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "envelope")
                .font(.system(size: 28, weight: .medium))
                .foregroundStyle(.secondary)
                .frame(width: 56, height: 56)
                .background(Color(.tertiarySystemFill), in: .rect(cornerRadius: 14))

            Text("Email")
                .font(.system(size: 20, weight: .bold))

            Text(email ?? String(localized: "No email available"))
                .font(.system(size: 16))
                .foregroundStyle(email == nil ? .secondary : .primary)
                .textSelection(.enabled)

            PrimaryButton(title: String(localized: "Close")) {
                dismiss()
            }
            .padding(.top, 4)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .brandBackground()
        .presentationDetents([.height(280)])
        .presentationDragIndicator(.visible)
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        EmailSheet(email: "user@email.com")
    }
    .preferredColorScheme(.dark)
}
