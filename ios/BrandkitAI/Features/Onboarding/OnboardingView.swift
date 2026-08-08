import SwiftUI

struct OnboardingView: View {
    @Environment(AppRouter.self) private var appRouter

    var body: some View {
        VStack(spacing: 16) {
            Text("Onboarding")

            Button("Get started") {
                appRouter.finishOnboarding()
            }
        }
        .navigationTitle("Onboarding")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .onboarding)
        .environment(AppRouter())
}
