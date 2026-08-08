import SwiftUI

/// Квадратная обложка проекта.
/// TODO: заменить градиент на `AsyncImage`, когда бэкенд начнёт отдавать картинки.
struct ProjectThumbnail: View {
    let colors: [Color]

    var body: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(
                LinearGradient(
                    colors: colors,
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .frame(width: 56, height: 56)
            .accessibilityHidden(true)
    }
}

#Preview {
    HStack(spacing: 12) {
        ProjectThumbnail(colors: [.pink, .purple])
        ProjectThumbnail(colors: [.green, .blue])
    }
    .padding()
    .preferredColorScheme(.dark)
}
