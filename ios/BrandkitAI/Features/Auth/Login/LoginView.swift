import SwiftUI

struct LoginView: View {
    @Environment(AppRouter.self) private var appRouter
    @Environment(Router.self) private var router

    var body: some View {
        VStack(spacing: 16) {
            Text("Login")

            Button("Sign in") {
                appRouter.finishAuth()
            }

            Button("Create account") {
                router.push(.register)
            }
        }
        .navigationTitle("Login")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .login)
        .environment(AppRouter(flow: .auth))
}
