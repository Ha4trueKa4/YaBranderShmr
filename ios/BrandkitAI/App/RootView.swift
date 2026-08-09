import SwiftUI

struct RootView: View {
    @AppStorage("hasFinishedOnboarding") private var hasFinishedOnboarding = false

    @State private var session = SessionStore()
    @State private var isOnboardingPresented = false
    @State private var canPresentSignIn = false

    private var isSignInPresented: Binding<Bool> {
        Binding(
            get: {
                canPresentSignIn && hasFinishedOnboarding && session.state == .signedOut
            },
            set: { _ in }
        )
    }

    var body: some View {
        Group {
            switch session.state {
            case .unknown:
                ProgressView()

            case .signedOut:
                Color.clear

            case .signedIn:
                MainTabView()
            }
        }
        .environment(session)
        .task {
            if hasFinishedOnboarding {
                canPresentSignIn = true
            } else {
                presentOnboarding()
            }

            session.restore()
        }
        .fullScreenCover(
            isPresented: $isOnboardingPresented,
            onDismiss: { canPresentSignIn = true }
        ) {
            OnboardingView(onFinish: finishOnboarding)
                .interactiveDismissDisabled()
        }
        .fullScreenCover(isPresented: isSignInPresented) {
            NavigationStack {
                SignInView(session: session)
            }
            .tint(.purple)
            .interactiveDismissDisabled()
        }
    }

    private func finishOnboarding() {
        hasFinishedOnboarding = true
        isOnboardingPresented = false
    }

    private func presentOnboarding() {
        guard !isOnboardingPresented else { return }

        var transaction = Transaction()
        transaction.disablesAnimations = true

        withTransaction(transaction) {
            canPresentSignIn = false
            isOnboardingPresented = true
        }
    }
}

#Preview {
    RootView()
}
