import SwiftUI

struct NewProjectButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label("New project", systemImage: "plus")
                .font(.headline)
                .foregroundStyle(Color(.onAccent))
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
                .background(.tint, in: .rect(cornerRadius: 14))
        }
        .buttonStyle(NewProjectButtonStyle())
    }
}

private struct NewProjectButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.85 : 1)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

#Preview {
    NewProjectButton {}
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
        .padding(16)
        .background(Color(.systemBackground))
        .preferredColorScheme(.dark)
}
