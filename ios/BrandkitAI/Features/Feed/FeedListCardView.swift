import SwiftUI

struct FeedListCardView: View {
    static let titleFont = Font.system(size: 14, weight: .bold)
    static let authorFont = Font.system(size: 12, weight: .medium)

    let title: String
    let previewURL: URL?
    let coverColors: [Color]?
    let coverHeight: CGFloat?
    let accountName: String?
    let onTap: () -> Void

    init(
        title: String,
        previewURL: URL? = nil,
        coverColors: [Color]? = nil,
        coverHeight: CGFloat? = nil,
        accountName: String? = nil,
        onTap: @escaping () -> Void
    ) {
        self.title = title
        self.previewURL = previewURL
        self.coverColors = coverColors
        self.coverHeight = coverHeight
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

                if let accountName {
                    HStack(spacing: 8) {
                        AvatarView(initials: accountName.initials, size: 24)

                        Text(accountName)
                            .font(Self.authorFont)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
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
            accountName: "Alice",
            onTap: {}
        )

        FeedListCardSkeleton()
    }
    .padding(16)
}
