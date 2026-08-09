import SwiftUI

struct ProjectRow: View {
    let item: ProjectListItem
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                ProjectThumbnail(colors: item.placeholderColors)

                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                        .font(.headline)
                        .foregroundStyle(.primary)

                    Text(item.subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "arrow.right")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(.primary)
            }
            .padding(12)
            .background(Color(.secondarySystemBackground), in: .rect(cornerRadius: 16))
            .contentShape(.rect(cornerRadius: 16))
        }
        .buttonStyle(ProjectRowButtonStyle())
        .accessibilityLabel("\(item.title), \(item.subtitle)")
    }
}

private struct ProjectRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.6 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

#Preview {
    VStack(spacing: 12) {
        ForEach(ProjectListItem.mocks) { item in
            ProjectRow(item: item) {}
        }
    }
    .padding(16)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color(.systemBackground))
    .preferredColorScheme(.dark)
}
