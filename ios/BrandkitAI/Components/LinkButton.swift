import SwiftUI

struct LinkButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.tint)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    LinkButton(title: "Forgot password?") {}
        .tint(.purple)
        .preferredColorScheme(.dark)
}
