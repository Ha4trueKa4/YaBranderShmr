import SwiftUI

/// Переключатель разделов ленты: горизонтальный список кнопок, как в макете.
struct FeedTabPickerView: View {
    @Binding var selection: FeedTab

    var body: some View {
        tabs
            .frame(maxWidth: .infinity)
            .overlay(alignment: .bottom) { Divider() }
    }

    private var tabs: some View {
        ScrollView(.horizontal) {
            HStack(alignment: .bottom, spacing: 24) {
                ForEach(FeedTab.allCases, id: \.self) { tab in
                    FeedTabButton(tab: tab, isSelected: tab == selection) {
                        select(tab)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 8)
        }
        .scrollIndicators(.hidden)
        .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
    }

    private func select(_ tab: FeedTab) {
        guard tab != selection else { return }
        withAnimation(.easeOut(duration: 0.2)) { selection = tab }
    }
}

#Preview {
    @Previewable @State var selection: FeedTab = .all

    VStack(spacing: 0) {
        FeedTabPickerView(selection: $selection)
        Spacer()
    }
}
