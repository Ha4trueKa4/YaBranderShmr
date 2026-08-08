import SwiftUI

struct ProjectChatView: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("AI Chat Assistant")
        }
        .navigationTitle("Chat")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .projectChat)
        .environment(AppRouter(flow: .main))
}
