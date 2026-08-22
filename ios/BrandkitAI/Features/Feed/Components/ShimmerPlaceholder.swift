import SwiftUI

struct ShimmerPlaceholder: View {
    private static let period: TimeInterval = 1.4

    var body: some View {
        Color(.shimmerBase)
            .overlay {
                GeometryReader { proxy in
                    TimelineView(.animation) { context in
                        let progress = context.date.timeIntervalSinceReferenceDate
                            .truncatingRemainder(dividingBy: Self.period) / Self.period
                        let bandWidth = proxy.size.width * 0.75
                        let travel = proxy.size.width + bandWidth

                        LinearGradient(
                            colors: [
                                Color(.clear),
                                Color(.shimmerHighlight),
                                Color(.clear)
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                        .frame(width: bandWidth)
                        .offset(x: travel * progress - bandWidth)
                    }
                }
            }
            .accessibilityHidden(true)
            .clipped()
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
}
