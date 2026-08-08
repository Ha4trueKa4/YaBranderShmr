import SwiftUI

struct AlternateBackground: ViewModifier {
    let cornerSize: CGSize

    func body(content: Content) -> some View {
        content
            .background {
                RoundedRectangle(cornerSize: cornerSize)
                    .foregroundStyle(.ultraThinMaterial)
                    .overlay {
                        RoundedRectangle(cornerSize: cornerSize)
                            .stroke()
                            .foregroundStyle(.ultraThinMaterial)
                    }
            }
    }
}

extension View {
    func alternateBackground(cornerSize: CGSize) -> some View {
        modifier(AlternateBackground(cornerSize: cornerSize))
    }
}
