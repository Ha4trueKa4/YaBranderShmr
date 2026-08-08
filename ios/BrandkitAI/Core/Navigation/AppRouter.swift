import Foundation

@MainActor
@Observable
final class AppRouter {
    private(set) var flow: AppFlow
    private(set) var selectedTab: AppTab = .feed

    let onboarding = Router()
    let auth = Router()

    private let feed = Router()
    private let create = Router()
    private let profile = Router()

    init(flow: AppFlow = .onboarding) {
        self.flow = flow
    }

    func router(for tab: AppTab) -> Router {
        switch tab {
        case .feed: feed
        case .create: create
        case .profile: profile
        }
    }

    func select(_ tab: AppTab) {
        if tab == selectedTab {
            router(for: tab).popToRoot()
        }
        selectedTab = tab
    }

    func finishOnboarding() {
        auth.reset()
        flow = .auth
    }

    func finishAuth() {
        selectedTab = .feed
        flow = .main
    }

    func signOut() {
        [onboarding, auth, feed, create, profile].forEach { $0.reset() }
        selectedTab = .feed
        flow = .auth
    }

    func openGeneratedProject() {
        selectedTab = .create
        create.setPath([.projectEditor])
    }
}
