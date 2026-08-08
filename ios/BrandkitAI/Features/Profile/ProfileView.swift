import SwiftUI

struct ProfileView: View {
    @Environment(AppRouter.self) private var appRouter
    @Environment(Router.self) private var router

    let target: ProfileTarget

    var body: some View {
        VStack(spacing: 16) {
            Text("Profile")

            Button("Open project") {
                router.push(.projectEditor)
            }

            if target == .current {
                Button("Sign out", role: .destructive) {
                    appRouter.signOut()
                }
            }
        }
        .navigationTitle("Profile")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .profile(.current))
        .environment(AppRouter(flow: .main))
}
