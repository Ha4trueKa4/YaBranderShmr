import SwiftUI

struct RootView: View {
    private enum CoverFlow {
        case none
        case onboarding
        case signIn
        case signUp
    }

    @AppStorage("hasFinishedOnboarding") private var hasFinishedOnboarding = false

    @State private var session = SessionStore()
    @State private var coverFlow: CoverFlow = .none
    @State private var pendingCover: CoverFlow?
    @State private var pendingCoverSource: CoverFlow?
    @State private var transitionID = UUID()

    private var isOnboardingPresented: Binding<Bool> {
        Binding(
            get: { coverFlow == .onboarding },
            set: { isPresented in
                guard !isPresented, coverFlow == .onboarding else { return }
                coverFlow = .none
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

    private var isSignUpPresented: Binding<Bool> {
        Binding(
            get: { coverFlow == .signUp && session.state == .signedOut },
            set: { isPresented in
                guard !isPresented, coverFlow == .signUp else { return }
                coverFlow = .none
            }
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
            session.restore()

            if session.state == .signedIn {
                hasFinishedOnboarding = true
            } else if hasFinishedOnboarding {
                present(.signIn)
            } else {
                presentOnboarding()
            }
        }
        .fullScreenCover(
            isPresented: isOnboardingPresented,
            onDismiss: {
                finishDismissal(of: .onboarding)
            }
        ) {
            OnboardingView(
                onSignUp: { finishOnboarding(to: .signUp) },
                onSignIn: { finishOnboarding(to: .signIn) }
            )
                .interactiveDismissDisabled()
        }
        .fullScreenCover(
            isPresented: isSignInPresented,
            onDismiss: {
                finishDismissal(of: .signIn)
            }
        ) {
            NavigationStack {
                SignInView(session: session) {
                    present(.signUp)
                }
                .swipeBackEnabled()
            }
            .tint(Color.accentColor)
            .interactiveDismissDisabled()
        }
        .fullScreenCover(
            isPresented: isSignUpPresented,
            onDismiss: {
                finishDismissal(of: .signUp)
            }
        ) {
            NavigationStack {
                SignUpView(session: session) {
                    present(.signIn)
                }
            }
            .tint(Color.accentColor)
            .interactiveDismissDisabled()
        }
        .onChange(of: session.state) { _, state in
            switch state {
            case .signedIn:
                pendingCover = nil
                pendingCoverSource = nil
                transitionID = UUID()
                coverFlow = .none

            case .signedOut:
                if hasFinishedOnboarding {
                    present(.signIn)
                }

            case .unknown:
                break
            }
        }
    }

    private func finishOnboarding(to cover: CoverFlow) {
        hasFinishedOnboarding = true
        present(cover)
    }

    private func presentOnboarding() {
        guard coverFlow == .none else { return }

        var transaction = Transaction()
        transaction.disablesAnimations = true

        withTransaction(transaction) {
            coverFlow = .onboarding
        }
    }

    private func present(_ cover: CoverFlow) {
        guard coverFlow != cover else { return }

        guard coverFlow == .none else {
            pendingCover = cover
            pendingCoverSource = coverFlow
            transitionID = UUID()
            coverFlow = .none
            return
        }

        pendingCover = nil
        pendingCoverSource = nil
        transitionID = UUID()
        coverFlow = cover
    }

    private func finishDismissal(of dismissedCover: CoverFlow) {
        guard coverFlow == .none else { return }
        guard pendingCoverSource == dismissedCover,
              let pendingCover else { return }
        let pendingTransitionID = transitionID
        self.pendingCover = nil
        self.pendingCoverSource = nil

        Task { @MainActor in
            await Task.yield()
            guard coverFlow == .none,
                  session.state == .signedOut,
                  transitionID == pendingTransitionID
            else { return }
            self.coverFlow = pendingCover
        }
    }

}

#Preview {
    RootView()
}
