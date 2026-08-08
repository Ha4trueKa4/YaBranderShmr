import SwiftUI

struct RootView: View {
    @State private var appRouter = AppRouter()

    var body: some View {
        Group {
            switch appRouter.flow {
            case .onboarding:
                RoutedNavigationStack(router: appRouter.onboarding, root: .onboarding)

            case .auth:
                RoutedNavigationStack(router: appRouter.auth, root: .login)

            case .main:
                MainTabView()
            }
        }
        .environment(appRouter)
    }
}

#Preview {
    RootView()
}
