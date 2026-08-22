import SwiftUI

/// Кнопка «Войти с Яндекс ID» по гайдлайнам бренда: чёрная с фирменным
/// логотипом. Цвета зафиксированы брендбуком Яндекса и от темы приложения
/// не зависят — поэтому литералы, а не тематические токены.
struct YandexIDButton: View {
    var isEnabled = true
    var isLoading = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    logo
                    Text("Sign in with Yandex ID")
                        .font(.headline)
                }
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(.black, in: .rect(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled || isLoading)
        .opacity(isEnabled || isLoading ? 1 : 0.5)
        .accessibilityLabel("Sign in with Yandex ID")
    }

    private var logo: some View {
        // Символ без суффикса «Image» — Xcode отбрасывает его
        // при генерации из ассета `yandexLogoImage`.
        Image(.yandexLogo)
            .resizable()
            .scaledToFit()
            .frame(width: 26, height: 26)
            .accessibilityHidden(true)
    }
}

#Preview {
    VStack(spacing: 16) {
        YandexIDButton {}
        YandexIDButton(isLoading: true) {}
    }
    .padding(24)
    .preferredColorScheme(.dark)
}
