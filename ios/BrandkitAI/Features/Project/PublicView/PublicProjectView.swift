import SwiftUI

struct PublicProjectView: View {
    @Environment(Router.self) private var router

    var body: some View {
        VStack(spacing: 16) {
            Text("Public Project")

            Button("Author profile") {
                router.push(.profile(.other))
            }
        }
        .navigationTitle("Public Project")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .publicProject)
        .environment(AppRouter(flow: .main))
}
