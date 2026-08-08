import SwiftUI

struct ProjectCreateView: View {
    @Environment(Router.self) private var router

    var body: some View {
        VStack(spacing: 16) {
            Text("New Project")

            Button("Generate Brandkit") {
                router.push(.projectGenerating)
            }
        }
        .navigationTitle("New Project")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .projectCreate)
        .environment(AppRouter(flow: .main))
}
