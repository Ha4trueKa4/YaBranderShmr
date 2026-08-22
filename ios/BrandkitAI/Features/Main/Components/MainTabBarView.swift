import SwiftUI

struct MainTabBarView: View {
    let selected: MainTab
    let onSelect: (MainTab) -> Void

    var body: some View {
        items
            .padding(.horizontal, 32)
            .frame(maxWidth: .infinity)
            .background {
                Color(.tabBarBackground)
                    .overlay(alignment: .top) { separator }
                    .ignoresSafeArea(edges: .bottom)
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
    }

    private var items: some View {
        HStack(spacing: 0) {
            ForEach(MainTab.allCases) { tab in
                MainTabBarItem(tab: tab, isSelected: tab == selected) {
                    onSelect(tab)
                }

                if tab != MainTab.allCases.last {
                    Spacer(minLength: 0)
                }
            }
        }
    }

    private var separator: some View {
        Rectangle()
            .fill(Color(.border))
            .frame(height: 1)
    }
}

#Preview {
    VStack(spacing: 0) {
        Spacer()
        MainTabBarView(selected: .feed) { _ in }
    }
    .background(Color(.background))
}
