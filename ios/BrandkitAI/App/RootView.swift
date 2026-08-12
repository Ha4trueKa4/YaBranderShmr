import SwiftUI

struct RootView: View {
    @AppStorage("hasFinishedOnboarding") private var hasFinishedOnboarding = false

    @State private var session = SessionStore()

    var body: some View {
        Group {
            if !hasFinishedOnboarding {
                OnboardingView(onFinish: { hasFinishedOnboarding = true })
            } else {
                switch session.state {
                case .unknown:
                    ProgressView()

                case .signedOut:
                    NavigationStack {
                        SignInView(session: session)
                    }
                    .tint(.purple)

                case .signedIn:
                    MainShellView()
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.background).ignoresSafeArea())
        .preferredColorScheme(.dark)
        .environment(session)
        .task {
            session.restore()
        }
    }
}

#Preview {
    RootView()
}
