import SwiftUI

struct ProfileConnectionRow<Trailing: View>: View {
    let name: String
    var subtitle: String? = nil
    var avatarSize: CGFloat = 56
    let onOpenProfile: () -> Void
    @ViewBuilder let trailing: () -> Trailing

    init(
        name: String,
        subtitle: String? = nil,
        avatarSize: CGFloat = 56,
        onOpenProfile: @escaping () -> Void = {},
        @ViewBuilder trailing: @escaping () -> Trailing
    ) {
        self.name = name
        self.subtitle = subtitle
        self.avatarSize = avatarSize
        self.onOpenProfile = onOpenProfile
        self.trailing = trailing
    }

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onOpenProfile) {
                HStack(spacing: 12) {
                    AvatarView(initials: name.initials, size: avatarSize)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(name)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(.primary)
                            .lineLimit(2)

                        if let subtitle {
                            Text(subtitle)
                                .font(.system(size: 13))
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            trailing()
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 88)
        .background(Color(.secondarySystemBackground), in: .rect(cornerRadius: 14))
    }
}

#Preview {
    VStack(spacing: 12) {
        ProfileConnectionRow(
            name: "Sarah Chen",
            subtitle: "Brand designer",
            onOpenProfile: {}
        ) {
            FollowButton(isSubscribed: false, isBusy: false, action: {})
        }

        ProfileConnectionRow(name: "Alex Morgan") {
            FollowButton(isSubscribed: true, isBusy: false, action: {})
        }
    }
    .padding(16)
}
