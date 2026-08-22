import SwiftUI

struct AvatarView: View {
    let initials: String
    let size: CGFloat
    var strokeWidth: CGFloat = 0

    var body: some View {
        Text(initials.uppercased())
            .font(.system(size: 0.27 * size + 4.5, weight: .bold))
            .foregroundStyle(Color(.onAccent))
            .frame(width: size, height: size)
            .background(Color(.endGradient), in: .circle)
            .overlay {
                if strokeWidth > 0 {
                    Circle().stroke(Color(.background), lineWidth: strokeWidth)
                }
            }
    }
}

extension String {
    var initials: String {
        split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined()
    }
}

#Preview {
    HStack(spacing: 16) {
        AvatarView(initials: "AB", size: 64, strokeWidth: 3)
        AvatarView(initials: "SC", size: 42)
        AvatarView(initials: "A", size: 24)
    }
    .padding()
}
