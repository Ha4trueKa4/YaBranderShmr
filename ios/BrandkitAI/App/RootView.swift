import SwiftUI

struct RootView: View {
    private enum CoverFlow {
        case none
        case onboarding
        case waitingForOnboardingDismissal
        case signIn
    }

    @AppStorage("hasFinishedOnboarding") private var hasFinishedOnboarding = false

    @State private var session = SessionStore()
    @State private var coverFlow: CoverFlow = .none

    private var isOnboardingPresented: Binding<Bool> {
        Binding(
            get: { coverFlow == .onboarding },
            set: { isPresented in
                guard !isPresented, coverFlow == .onboarding else { return }
                coverFlow = .waitingForOnboardingDismissal
            }
        )
    }

    private var isSignInPresented: Binding<Bool> {
        Binding(
            get: { coverFlow == .signIn && session.state == .signedOut },
            set: { isPresented in
                guard !isPresented, coverFlow == .signIn else { return }
                coverFlow = .none
            },
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
                MainShellView()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(coverFlow == .onboarding ? .clear : .background).ignoresSafeArea())
        .preferredColorScheme(.dark)
        .environment(session)
        .task {
            if hasFinishedOnboarding {
                coverFlow = .signIn
            } else {
                presentOnboarding()
            }

            session.restore()
        }
        .fullScreenCover(
            isPresented: isOnboardingPresented,
            onDismiss: {
                guard coverFlow == .waitingForOnboardingDismissal else { return }
                coverFlow = .signIn
            }
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
        .onChange(of: session.state) { _, state in
            guard state == .signedIn, coverFlow == .signIn else { return }
            coverFlow = .none
        }
    }

    private func finishOnboarding() {
        hasFinishedOnboarding = true
        coverFlow = .waitingForOnboardingDismissal
    }

    private func presentOnboarding() {
        guard coverFlow != .onboarding else { return }

        var transaction = Transaction()
        transaction.disablesAnimations = true

        withTransaction(transaction) {
            coverFlow = .onboarding
        }
    }
}

#Preview {
    RootView()
}
