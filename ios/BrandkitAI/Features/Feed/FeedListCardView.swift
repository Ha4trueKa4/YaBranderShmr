import SwiftUI

struct FeedListCardView: View {
    static let titleFont = Font.system(size: 14, weight: .bold)
    static let authorFont = Font.system(size: 12, weight: .medium)

    let title: String
    let previewURL: URL?
    let coverColors: [Color]?
    let coverHeight: CGFloat?
    let accountAvatar: Image?
    let accountName: String?
    let onTap: () -> Void

    init(
        title: String,
        previewURL: URL? = nil,
        coverColors: [Color]? = nil,
        coverHeight: CGFloat? = nil,
        accountAvatar: Image? = nil,
        accountName: String? = nil,
        onTap: @escaping () -> Void
    ) {
        self.title = title
        self.previewURL = previewURL
        self.coverColors = coverColors
        self.coverHeight = coverHeight
        self.accountAvatar = accountAvatar
        self.accountName = accountName
        self.onTap = onTap
    }

    var body: some View {
        Button(action: onTap) {
            card
        }
        .buttonStyle(.plain)
    }

    private var card: some View {
        VStack(spacing: 0) {
            cover

            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(Self.titleFont)
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                if accountAvatar != nil || accountName != nil {
                    HStack(spacing: 8) {
                        if let accountAvatar {
                            accountAvatar
                                .resizable()
                                .scaledToFill()
                                .frame(width: 24, height: 24)
                                .clipShape(Circle())
                        }

                        if let accountName {
                            Text(accountName)
                                .font(Self.authorFont)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                    }
                }
            }
            .padding(12)
        }
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .contentShape(RoundedRectangle(cornerRadius: 16))
    }

    private var cover: some View {
        Group {
            if let coverColors, !coverColors.isEmpty {
                LinearGradient(
                    colors: coverColors,
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .frame(height: coverHeight ?? 128)
            } else {
                FeedPreviewImage(url: previewURL)
            }
        }
        .clipShape(.rect(topLeadingRadius: 16, topTrailingRadius: 16))
    }
}

#Preview {
    HStack(alignment: .top, spacing: 12) {
        FeedListCardView(
            title: "Project Alpha",
            previewURL: nil,
            accountAvatar: Image("image.placeholder"),
            accountName: "Alice",
            onTap: {}
        )

        FeedListCardSkeleton()
    }
    .padding(16)
    .preferredColorScheme(.dark)
}
