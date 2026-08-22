import SwiftUI

struct NavigationBackButton: View {
    let action: () -> Void
    var foregroundColor: Color = .primary

    var body: some View {
        Button(action: action) {
            Image(systemName: "arrow.left")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(foregroundColor)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Back")
    }
}

private struct CustomNavigationBarModifier: ViewModifier {
    let title: String
    let onBack: (() -> Void)?
    @Environment(\.colorScheme) private var colorScheme

    func body(content: Content) -> some View {
        content
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(onBack != nil)
            .toolbar {
                if let onBack {
                    backItem(action: onBack)
                }
            }
            .toolbarBackgroundVisibility(.visible, for: .navigationBar)
            .toolbarBackground(Color.background, for: .navigationBar)
            .toolbarColorScheme(colorScheme, for: .navigationBar)
    }

    @ToolbarContentBuilder
    private func backItem(action: @escaping () -> Void) -> some ToolbarContent {
        if #available(iOS 26.0, *) {
            ToolbarItem(placement: .topBarLeading) {
                NavigationBackButton(action: action)
            }
            .sharedBackgroundVisibility(.hidden)
        } else {
            ToolbarItem(placement: .topBarLeading) {
                NavigationBackButton(action: action)
            }
        }
    }
}

extension View {
    func customNavigationBar(title: String = "", onBack: (() -> Void)? = nil) -> some View {
        modifier(CustomNavigationBarModifier(title: title, onBack: onBack))
    }
}
