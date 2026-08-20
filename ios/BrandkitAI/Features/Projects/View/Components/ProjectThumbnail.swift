import SwiftUI

struct ProjectThumbnail: View {
    let url: URL?
    let isPrivate: Bool
    let colors: [Color]
    let refreshURL: (@Sendable () async throws -> URL)?

    init(
        url: URL? = nil,
        isPrivate: Bool = false,
        colors: [Color],
        refreshURL: (@Sendable () async throws -> URL)? = nil
    ) {
        self.url = url
        self.isPrivate = isPrivate
        self.colors = colors
        self.refreshURL = refreshURL
    }

    var body: some View {
        Group {
            if let url {
                SVGDocumentView(
                    source: .url(url, isPrivate: isPrivate),
                    refreshURL: refreshURL
                )
            } else {
                RoundedRectangle(cornerRadius: 12)
                    .fill(
                        LinearGradient(
                            colors: colors,
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            }
        }
        .frame(width: 56, height: 56)
        .clipShape(RoundedRectangle(cornerRadius: 12))
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
