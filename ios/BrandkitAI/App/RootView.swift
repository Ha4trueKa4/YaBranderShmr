import SwiftUI

struct RootView: View {
    private enum CoverFlow {
        case none
        case onboarding
        case signIn
        case signUp
    }

    @AppStorage("hasFinishedOnboarding") private var hasFinishedOnboarding = false
    @AppStorage(AppTheme.storageKey) private var themeRawValue = AppTheme.system.rawValue

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

            case .signedOut, .signedIn:
                MainShellView()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(coverFlow == .onboarding ? .clear : .background).ignoresSafeArea())
        .preferredColorScheme((AppTheme(rawValue: themeRawValue) ?? .system).colorScheme)
        .environment(session)
        .task {
            session.restore()

            if session.state == .signedIn {
                hasFinishedOnboarding = true
            } else if !hasFinishedOnboarding {
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
                onGetStarted: finishOnboarding,
                onSignIn: { finishOnboarding(to: .signIn) }
            )
                .interactiveDismissDisabled()
        }
        .sheet(
            isPresented: isSignInPresented,
            onDismiss: {
                finishDismissal(of: .signIn)
            }
        ) {
            NavigationStack {
                SignInView(session: session) {
                    present(.signUp)
                }
                .toolbar { authenticationCloseButton }
            }
            .tint(Color.accentColor)
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .sheet(
            isPresented: isSignUpPresented,
            onDismiss: {
                finishDismissal(of: .signUp)
            }
        ) {
            NavigationStack {
                SignUpView(session: session) {
                    present(.signIn)
                }
                .toolbar { authenticationCloseButton }
            }
            .tint(Color.accentColor)
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .onChange(of: session.state) { _, state in
            switch state {
            case .signedIn:
                pendingCover = nil
                pendingCoverSource = nil
                transitionID = UUID()
                coverFlow = .none

            case .signedOut:
                break

            case .unknown:
                break
            }
        }
        .onChange(of: session.signInPresentationRequestID) {
            guard session.state == .signedOut else { return }
            present(.signIn)
        }
    }

    @ToolbarContentBuilder
    private var authenticationCloseButton: some ToolbarContent {
        if #available(iOS 26.0, *) {
            ToolbarItem(placement: .topBarTrailing) {
                authenticationCloseAction
            }
            .sharedBackgroundVisibility(.hidden)
        } else {
            ToolbarItem(placement: .topBarTrailing) {
                authenticationCloseAction
            }
        }
    }

    private var authenticationCloseAction: some View {
        Button {
            coverFlow = .none
        } label: {
            Image(systemName: "xmark")
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Close")
    }

    private func finishOnboarding() {
        hasFinishedOnboarding = true
        coverFlow = .none
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
