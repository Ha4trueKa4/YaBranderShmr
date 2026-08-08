import SwiftUI

struct RegisterView: View {
    @Environment(AppRouter.self) private var appRouter
    @Environment(Router.self) private var router

    var body: some View {
        VStack(spacing: 16) {
            Text("Register")

            Button("Create account") {
                appRouter.finishAuth()
            }

            Button("Back to sign in") {
                router.pop()
            }
        }
        .navigationTitle("Register")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .register)
        .environment(AppRouter(flow: .auth))
}
