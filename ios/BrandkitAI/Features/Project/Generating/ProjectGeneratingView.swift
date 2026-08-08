import SwiftUI

struct ProjectGeneratingView: View {
    @Environment(AppRouter.self) private var appRouter

    var body: some View {
        VStack(spacing: 16) {
            Text("Generating")

            Button("Open editor") {
                appRouter.openGeneratedProject()
            }
        }
        .navigationTitle("Generating")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .projectGenerating)
        .environment(AppRouter(flow: .main))
}
