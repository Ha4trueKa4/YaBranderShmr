import SwiftUI

/// Главный флоу: по стеку навигации на раздел, таб-бар навешен на корневой
/// экран **внутри** стека. Поэтому пушнутый экран перекрывает бар физически —
/// с нативной анимацией и рабочим swipe-back, без флагов видимости.
struct MainShellView: View {
    @Environment(SessionStore.self) private var session
    @State private var router = AppRouter()

    var body: some View {
        ZStack {
            ForEach(MainTab.allCases) { tab in
                let isActive = router.selectedTab == tab

                stack(for: tab)
                    .opacity(isActive ? 1 : 0)
                    .allowsHitTesting(isActive)
            }
        }
        .environment(router)
    }

    private func stack(for tab: MainTab) -> some View {
        NavigationStack(path: router.path(for: tab)) {
            root(for: tab)
                .brandBackground()
                .safeAreaInset(edge: .bottom, spacing: 0) { tabBar }
                .toolbar(.hidden, for: .navigationBar)
                .swipeBackEnabled()
                .navigationDestination(for: AppRoute.self) { AppRouteView(route: $0) }
        }
    }

    private var tabBar: some View {
        MainTabBarView(selected: router.selectedTab, onSelect: router.select)
    }

    @ViewBuilder
    private func root(for tab: MainTab) -> some View {
        switch tab {
        case .feed:
            FeedView(
                // TODO: экрана поиска ещё нет — кнопка в шапке пока без перехода.
                onOpenProject: { projectID in
                    router.push(.publicProject(projectID: projectID), in: .feed)
                }
            )

        case .create:
            ProjectsView(
                session: session,
                onOpenChat: { item in
                    router.push(.chat(projectID: item.id, title: item.title), in: .create)
                },
                onNewProject: { router.push(.projectCreate, in: .create) }
            )

        case .profile:
            ProfileView(
                session: session,
                isCurrentUser: true,
                onOpenSettings: { router.push(.settings, in: .profile) },
                onOpenProject: { projectID, title, imageURL in
                    router.push(.projectImage(projectID: projectID, title: title, imageURL: imageURL), in: .profile)
                },
                onOpenConnections: { userID, kind in
                    router.push(.profileConnections(userID: userID, kind: kind), in: .profile)
                }
            )
        }
    }
}

#Preview {
    MainShellView()
        .environment(SessionStore())
}
