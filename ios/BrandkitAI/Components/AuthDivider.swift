import SwiftUI

/// Разделитель «— or —» между парольной формой и кнопками соцвхода.
struct AuthDivider: View {
    var body: some View {
        HStack(spacing: 12) {
            line
            Text("or")
                .font(.footnote)
                .foregroundStyle(.secondary)
            line
        }
        .accessibilityHidden(true)
    }

    private var line: some View {
        Rectangle()
            .fill(Color(.separator))
            .frame(height: 1)
            .frame(maxWidth: .infinity)
    }
}

#Preview {
    AuthDivider()
        .padding(24)
        .preferredColorScheme(.dark)
}
