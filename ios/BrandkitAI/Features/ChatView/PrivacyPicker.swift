import SwiftUI

struct PrivacyPicker: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var selection: ChatViewModel.ProjectPrivacy
    var onSelect: (ChatViewModel.ProjectPrivacy) -> Void = { _ in }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Project Visibility")
                .font(.title2.bold())

            Text("Manage who can inspect and clone your brandkit assets.")
                .font(.body)
                .foregroundStyle(.secondary)
                .padding(.top, 4)

            VStack(spacing: 12) {
                ForEach(ChatViewModel.ProjectPrivacy.allCases) { privacy in
                    option(privacy)
                }
            }
            .padding(.top, 28)

        }
        .padding(.horizontal, 24)
        .padding(.top, 24)
        .presentationDetents([.height(360)])
        .presentationDragIndicator(.visible)
        .presentationBackground(Color(.background))
    }

    private func option(_ privacy: ChatViewModel.ProjectPrivacy) -> some View {
        let isSelected = selection == privacy

        return Button {
            selection = privacy
            onSelect(privacy)
            dismiss()
        } label: {
            HStack(spacing: 16) {
                Image(systemName: privacy.icon)
                    .font(.system(size: 24, weight: .medium))
                    .foregroundStyle(isSelected ? Color.accentColor : .secondary)
                    .frame(width: 32)

                VStack(alignment: .leading, spacing: 3) {
                    Text(privacy.title)
                        .font(.body.weight(.semibold))
                        .foregroundStyle(.primary)

                    Text(privacy.subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                }

                Spacer(minLength: 8)

                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Color.accentColor)
                        .frame(width: 24, height: 24)
                } else {
                    Circle()
                        .stroke(Color.secondary.opacity(0.25), lineWidth: 1)
                        .frame(width: 24, height: 24)
                }
            }
            .padding(.horizontal, 18)
            .frame(minHeight: 88)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 18))
            .overlay {
                RoundedRectangle(cornerRadius: 18)
                    .stroke(
                        isSelected ? Color.accentColor : Color.secondary.opacity(0.16),
                        lineWidth: isSelected ? 1.5 : 1
                    )
            }
            .contentShape(RoundedRectangle(cornerRadius: 18))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

#Preview {
    @Previewable @State var selection: ChatViewModel.ProjectPrivacy = .privateProject

    Color.clear
        .sheet(isPresented: .constant(true)) {
            PrivacyPicker(selection: $selection)
        }
        .preferredColorScheme(.dark)
}
