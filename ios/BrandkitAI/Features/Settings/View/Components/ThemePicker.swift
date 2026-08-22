import SwiftUI

struct ThemePicker: View {
    @Binding var selection: AppTheme

    var body: some View {
        HStack(spacing: 4) {
            ForEach(AppTheme.allCases) { theme in
                segment(for: theme)
            }
        }
        .padding(4)
        .background(Color(.tertiarySystemFill), in: .rect(cornerRadius: 12))
    }

    private func segment(for theme: AppTheme) -> some View {
        let isSelected = theme == selection

        return Button {
            withAnimation(.easeOut(duration: 0.15)) {
                selection = theme
            }
        } label: {
            Text(theme.title)
                .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                .foregroundStyle(isSelected ? Color(.onAccent) : .secondary)
                .frame(maxWidth: .infinity)
                .frame(height: 36)
                .background {
                    if isSelected {
                        RoundedRectangle(cornerRadius: 9)
                            .fill(Color.accentColor)
                    }
                }
                .contentShape(.rect)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    @Previewable @State var theme: AppTheme = .dark

    ThemePicker(selection: $theme)
        .padding(16)
        .preferredColorScheme(.dark)
}
