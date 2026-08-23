import SwiftUI

/// Шестерёнка настроек поверх градиентной шапки профиля. Общая для
/// залогиненного профиля и гостевого плейсхолдера, чтобы кнопка выглядела
/// одинаково в обоих состояниях.
struct CoverSettingsButton: View {
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(Color(.onAccent).opacity(0.25))
                    .stroke(Color(.onAccent).opacity(0.35), lineWidth: 1)
                    .frame(width: 38, height: 38)
                Image(systemName: "gearshape")
                    .font(.system(size: 16, weight: .semibold))
                    .frame(width: 44, height: 44)
            }
        }
        .foregroundStyle(Color(.onAccent))
        .accessibilityLabel("Settings")
    }
}
