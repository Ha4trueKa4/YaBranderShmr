import SwiftUI

struct ThemePicker: View {
    @Binding var selection: AppTheme

    private static let spacing: CGFloat = 4

    private var selectedIndex: Int {
        AppTheme.allCases.firstIndex(of: selection) ?? 0
    }

    var body: some View {
        ZStack(alignment: .leading) {
            GeometryReader { proxy in
                let count = CGFloat(AppTheme.allCases.count)
                let segmentWidth = (proxy.size.width - Self.spacing * (count - 1)) / count

                RoundedRectangle(cornerRadius: 9)
                    .fill(Color.accentColor)
                    .frame(width: segmentWidth)
                    .offset(x: CGFloat(selectedIndex) * (segmentWidth + Self.spacing))
            }

            HStack(spacing: Self.spacing) {
                ForEach(AppTheme.allCases) { theme in
                    segment(for: theme)
                }
            }
        }
        .padding(4)
        .background(Color(.tertiarySystemFill), in: .rect(cornerRadius: 12))
        .animation(.snappy(duration: 0.3), value: selection)
    }

    private func segment(for theme: AppTheme) -> some View {
        let isSelected = theme == selection

        return Button {
            selection = theme
        } label: {
            Text(theme.title)
                .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                .foregroundStyle(isSelected ? Color(.onAccent) : .secondary)
                .frame(maxWidth: .infinity)
                .frame(height: 36)
                .contentShape(.rect)
        }
        .buttonStyle(ThemeSegmentButtonStyle())
    }
}

private struct ThemeSegmentButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.55 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

#Preview {
    @Previewable @State var theme: AppTheme = .dark

    ThemePicker(selection: $theme)
        .padding(16)
        .preferredColorScheme(.dark)
}
