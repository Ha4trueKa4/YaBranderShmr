import SwiftUI

struct ShimmerPlaceholder: View {
    private static let period: TimeInterval = 1.4

    var body: some View {
        Color(.secondarySystemBackground)
            .overlay {
                GeometryReader { proxy in
                    TimelineView(.animation) { context in
                        let progress = context.date.timeIntervalSinceReferenceDate
                            .truncatingRemainder(dividingBy: Self.period) / Self.period
                        let bandWidth = proxy.size.width * 0.6
                        let travel = proxy.size.width + bandWidth

                        LinearGradient(
                            colors: [.clear, .white.opacity(0.08), .clear],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                        .frame(width: bandWidth)
                        .offset(x: travel * progress - bandWidth)
                    }
                }
            }
            .accessibilityHidden(true)
    }
}

#Preview {
    VStack(spacing: 16) {
        ShimmerPlaceholder()
            .aspectRatio(1, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 16))

        ShimmerPlaceholder()
            .frame(width: 120, height: 14)
            .clipShape(Capsule())
    }
    .padding(16)
    .preferredColorScheme(.dark)
}
