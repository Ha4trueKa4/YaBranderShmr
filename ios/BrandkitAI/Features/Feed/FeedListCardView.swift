import SwiftUI

struct FeedListCardView: View {
    let title: String
    let accountImage: Image?
    let coverColors: [Color]?
    let coverHeight: CGFloat?
    let accountAvatar: Image?
    let accountName: String?
    let onTap: () -> Void

    init(
        title: String,
        accountImage: Image? = nil,
        coverColors: [Color]? = nil,
        coverHeight: CGFloat? = nil,
        accountAvatar: Image? = nil,
        accountName: String? = nil,
        onTap: @escaping () -> Void
    ) {
        self.title = title
        self.accountImage = accountImage
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
                    .font(.system(size: 14, weight: .bold))
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
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                    }
                }
            }
            .padding(12)
        }
        .alternateBackground(cornerSize: CGSize(width: 16, height: 16))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .contentShape(RoundedRectangle(cornerRadius: 16))
    }

    @ViewBuilder
    private var cover: some View {
        if let coverColors, !coverColors.isEmpty {
            LinearGradient(
                colors: coverColors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .frame(height: coverHeight ?? 128)
        } else if let accountImage {
            accountImage
                .resizable()
                .scaledToFit()
        }
    }
}
