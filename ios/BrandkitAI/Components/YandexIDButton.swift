import SwiftUI

struct YandexIDButton: View {
    var isEnabled = true
    var isLoading = false
    let action: () -> Void

    @Environment(\.colorScheme) private var colorScheme

    private var background: Color { colorScheme == .dark ? .white : .black }
    private var foreground: Color { colorScheme == .dark ? .black : .white }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if isLoading {
                    ProgressView()
                        .tint(foreground)
                } else {
                    logo
                    Text("Sign in with Yandex ID")
                        .font(.headline)
                }
            }
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(background, in: .rect(cornerRadius: 20))
        }
        .disabled(!isEnabled || isLoading)
        .opacity(isEnabled || isLoading ? 1 : 0.5)
        .accessibilityLabel("Sign in with Yandex ID")
    }

    private var logo: some View {
        Image(.yandexLogo)
            .resizable()
            .scaledToFit()
            .frame(width: 26, height: 26)
            .accessibilityHidden(true)
    }
}

#Preview("Dark") {
    VStack(spacing: 16) {
        YandexIDButton {}
        YandexIDButton(isLoading: true) {}
    }
    .padding(24)
    .preferredColorScheme(.dark)
}

#Preview("Light") {
    VStack(spacing: 16) {
        YandexIDButton {}
        YandexIDButton(isLoading: true) {}
    }
    .padding(24)
    .preferredColorScheme(.light)
}
