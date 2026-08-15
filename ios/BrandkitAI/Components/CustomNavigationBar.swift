import SwiftUI

struct NavigationBackButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "arrow.left")
                .font(.body.weight(.semibold))
                .foregroundStyle(.primary)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Back")
    }
}

private struct CustomNavigationBarModifier: ViewModifier {
    let title: String
    let onBack: (() -> Void)?

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
