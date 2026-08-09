import SwiftUI

struct ChatImageMockView: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.02, green: 0.08, blue: 0.14),
                    Color(red: 0.04, green: 0.18, blue: 0.22),
                    Color(red: 0.03, green: 0.05, blue: 0.12)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            Circle()
                .fill(.purple.opacity(0.55))
                .frame(width: 130, height: 80)
                .blur(radius: 28)
                .offset(x: 70, y: -38)

            Circle()
                .fill(.cyan.opacity(0.5))
                .frame(width: 110, height: 70)
                .blur(radius: 24)
                .offset(x: -65, y: 35)

            Image(systemName: "sparkles")
                .font(.system(size: 42, weight: .light))
                .foregroundStyle(.white, .purple)
                .shadow(color: .cyan.opacity(0.8), radius: 12)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 120)
        .clipShape(.rect(cornerRadius: 8))
    }
}
