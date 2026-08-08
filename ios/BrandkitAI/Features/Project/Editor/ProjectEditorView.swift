import SwiftUI

struct ProjectEditorView: View {
    @Environment(Router.self) private var router

    var body: some View {
        VStack(spacing: 16) {
            Text("Editor")

            Button("Chat") {
                router.push(.projectChat)
            }

            Button("Version history") {
                router.push(.projectVersionHistory)
            }

            Button("Visibility") {
                router.present(.projectVisibility)
            }
        }
        .navigationTitle("Editor")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .projectEditor)
        .environment(AppRouter(flow: .main))
}
