import SwiftUI

struct MainTabBarItem: View {
    let tab: MainTab
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: tab.icon)
                    .font(.system(size: 20))
                    .frame(width: 20, height: 20)

                Text(tab.title)
                    .font(.system(size: 11, weight: .medium))
                    .lineLimit(1)
                    .fixedSize()
            }
            .foregroundStyle(isSelected ? Color.accentColor : Color(.tabBarInactive))
            .frame(width: 56)
            .padding(.vertical, 13.5)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tab.title)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

#Preview {
    HStack(spacing: 40) {
        MainTabBarItem(tab: .feed, isSelected: true) {}
        MainTabBarItem(tab: .create, isSelected: false) {}
        MainTabBarItem(tab: .profile, isSelected: false) {}
    }
    .background(Color(.tabBarBackground))
}
