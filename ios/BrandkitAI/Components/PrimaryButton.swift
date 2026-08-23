import SwiftUI

struct PrimaryButton: View {
    let title: String
    var isEnabled: Bool = true
    var isLoading = false
    var fill: AnyShapeStyle = AnyShapeStyle(.tint)
    var cornerRadius: CGFloat = 12
    let action: () -> Void

    @State private var feedbackTrigger = 0

    var body: some View {
        Button {
            feedbackTrigger += 1
            action()
        } label: {
            HStack(spacing: 10) {
                if isLoading {
                    ProgressView()
                        .tint(Color(.onAccent))
                }

                Text(isLoading ? "Please wait..." : title)
                    .font(.headline)
            }
            .foregroundStyle(Color(.onAccent))
            .frame(maxWidth: .infinity)
            .frame(height: 52)
        }
        .buttonStyle(PrimaryButtonStyle(fill: fill, cornerRadius: cornerRadius))
        .disabled(!isEnabled || isLoading)
        .opacity(isEnabled || isLoading ? 1 : 0.5)
        .sensoryFeedback(.impact(weight: .light), trigger: feedbackTrigger)
        .accessibilityLabel(isLoading ? "Loading" : title)
    }
}

private struct PrimaryButtonStyle: ButtonStyle {
    let fill: AnyShapeStyle
    let cornerRadius: CGFloat

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(fill, in: .rect(cornerRadius: cornerRadius))
            .opacity(configuration.isPressed ? 0.85 : 1)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

#Preview {
    PrimaryButton(title: "Sign in") {}
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .tint(Color.accentColor)
        .preferredColorScheme(.dark)
}
