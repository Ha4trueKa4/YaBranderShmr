import SwiftUI

struct FollowButton: View {
    let isSubscribed: Bool
    let isBusy: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(isSubscribed ? "Following" : "Follow")
        }
        .font(.system(size: 14, weight: .semibold))
        .foregroundStyle(isSubscribed ? Color.accentColor : .white)
        .padding(.horizontal, 18)
        .frame(height: 36)
        .background(isSubscribed ? Color.clear : Color.accentColor, in: Capsule())
        .overlay {
            Capsule()
                .stroke(Color.accentColor, lineWidth: 1.5)
                .opacity(isSubscribed ? 1 : 0)
        }
        .buttonStyle(FollowButtonStyle())
        .animation(.easeInOut, value: isSubscribed)
        .disabled(isBusy)
    }
}

struct FollowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.94 : 1)
            .opacity(configuration.isPressed ? 0.8 : 1)
            .animation(.spring(duration: 0.22, bounce: 0.25), value: configuration.isPressed)
    }
}

#Preview {
    VStack(spacing: 20) {
        FollowButton(isSubscribed: false, isBusy: false, action: {})
        FollowButton(isSubscribed: true, isBusy: false, action: {})
        FollowButton(isSubscribed: false, isBusy: true, action: {})
    }
    .padding()
}
