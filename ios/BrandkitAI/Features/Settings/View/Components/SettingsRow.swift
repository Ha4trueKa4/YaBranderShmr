import SwiftUI

struct SettingsRow: View {
    let icon: String
    let title: LocalizedStringKey
    var value: String?
    var showsChevron = false
    var action: (() -> Void)?

    var body: some View {
        if let action {
            Button(action: action) {
                content
            }
            .buttonStyle(.plain)
        } else {
            content
        }
    }

    private var content: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(.secondary)
                .frame(width: 40, height: 40)
                .background(Color(.tertiarySystemFill), in: .rect(cornerRadius: 10))

            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(.primary)

            Spacer(minLength: 12)

            if let value {
                Text(value)
                    .font(.system(size: 15))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }

            if showsChevron {
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.secondary)
            }
        }
        .padding(12)
        .contentShape(.rect)
    }
}

#Preview {
    VStack(spacing: 0) {
        SettingsRow(icon: "person", title: "Full name", value: "John Doe") {}
        SettingsDivider()
        SettingsRow(icon: "doc.text", title: "Privacy Policy", showsChevron: true) {}
    }
    .background(Color(.secondarySystemBackground))
    .clipShape(RoundedRectangle(cornerRadius: 16))
    .padding(16)
    .preferredColorScheme(.dark)
}
