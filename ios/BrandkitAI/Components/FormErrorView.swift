import SwiftUI

struct FormErrorView: View {
    let message: String

    var body: some View {
        Label {
            Text(message)
                .multilineTextAlignment(.leading)
        } icon: {
            Image(systemName: "exclamationmark.triangle.fill")
        }
        .font(.footnote)
        .foregroundStyle(.red)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.red.opacity(0.12), in: .rect(cornerRadius: 12))
        .overlay {
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.red.opacity(0.35), lineWidth: 1)
        }
        .accessibilityElement(children: .combine)
        .transition(.move(edge: .top).combined(with: .opacity))
    }
}

#Preview {
    FormErrorView(message: "Unable to connect. Check your internet connection and try again.")
        .padding(24)
        .frame(maxHeight: .infinity)
        .background(Color(.background))
        .preferredColorScheme(.dark)
}
