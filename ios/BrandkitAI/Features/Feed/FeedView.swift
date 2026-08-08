import SwiftUI

struct FeedView: View {
    @Environment(Router.self) private var router

    var body: some View {
        VStack(spacing: 16) {
            Text("Feed")

            Button("Open public project") {
                router.push(.publicProject)
            }
        }
        .navigationTitle("Feed")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .feed)
        .environment(AppRouter(flow: .main))
}
