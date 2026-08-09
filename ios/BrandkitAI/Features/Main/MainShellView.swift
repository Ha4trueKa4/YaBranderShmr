import SwiftUI

/// Главный флоу: по стеку навигации на раздел, таб-бар навешен на корневой
/// экран **внутри** стека. Поэтому пушнутый экран перекрывает бар физически —
/// с нативной анимацией и рабочим swipe-back, без флагов видимости.
struct MainShellView: View {
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
                // Корень должен занимать всё окно, иначе бар прилипнет к низу
                // контента, а не экрана.
                .brandBackground()
                .safeAreaInset(edge: .bottom, spacing: 0) { tabBar }
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: AppRoute.self) { AppRouteView(route: $0) }
        }
    }

    private var tabBar: some View {
        MainTabBarView(selected: router.selectedTab) { tab in
            withAnimation(.easeOut(duration: 0.2)) { router.select(tab) }
        }
    }

    @ViewBuilder
    private func root(for tab: MainTab) -> some View {
        switch tab {
        case .feed:
            FeedView()

        case .create:
            ProjectsView(
                onOpenChat: { router.push(.projectEditor(projectID: $0.id), in: .create) },
                onNewProject: { router.push(.projectCreate, in: .create) }
            )

        case .profile:
            ProfileView(
                isCurrentUser: true,
                // TODO: id придёт из списка проектов профиля.
                onOpenProject: { router.push(.projectEditor(projectID: UUID()), in: .profile) }
            )
        }
    }
}

#Preview {
    MainShellView()
        .environment(SessionStore())
}
