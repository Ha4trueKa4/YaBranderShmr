import SwiftUI

struct MainTabView: View {
    @Environment(AppRouter.self) private var appRouter

    var body: some View {
        TabView(selection: selection) {
            ForEach(AppTab.allCases) { tab in
                RoutedNavigationStack(router: appRouter.router(for: tab), root: tab.root)
                    .tabItem { Label(tab.title, systemImage: tab.systemImage) }
                    .tag(tab)
            }
        }
    }

    private var selection: Binding<AppTab> {
        Binding(
            get: { appRouter.selectedTab },
            set: { appRouter.select($0) }
        )
    }
}

#Preview {
    MainTabView()
        .environment(AppRouter(flow: .main))
}
