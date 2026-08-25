import SwiftUI

/// Кнопка одного раздела ленты: подпись и подчёркивание под ней,
/// оба красятся по признаку выбранности.
struct FeedTabButton: View {
    let tab: FeedTab
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                title
                underline
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tab.title)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }

    private var title: some View {
        Text(tab.title)
            .font(.system(size: 15, weight: isSelected ? .semibold : .regular))
            .foregroundStyle(isSelected ? Color.primary : Color.secondary)
            .lineLimit(1)
            .fixedSize()
    }

    private var underline: some View {
        Capsule()
            .fill(isSelected ? Color.accentColor : Color.clear)
            .frame(height: 2)
    }
}

#Preview {
    HStack(spacing: 24) {
        FeedTabButton(tab: .all, isSelected: true) {}
        FeedTabButton(tab: .subscriptions, isSelected: false) {}
    }
    .padding(16)
}
