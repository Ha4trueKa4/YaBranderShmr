import SwiftUI

/// Состояние навигации главного флоу: выбранный раздел и стек каждого раздела.
///
/// Стеки лежат здесь, а не во вью, чтобы разделы помнили свою глубину независимо
/// друг от друга и чтобы переход можно было выполнить откуда угодно.
@MainActor
@Observable
final class AppRouter {
    var selectedTab: MainTab = .feed
    var paths: [MainTab: [AppRoute]] = [:]

    /// Стек раздела для `NavigationStack(path:)`.
    /// Хранилище — словарь, но наружу это знание не течёт.
    func path(for tab: MainTab) -> Binding<[AppRoute]> {
        Binding(
            get: { self.paths[tab, default: []] },
            set: { self.paths[tab] = $0 }
        )
    }

    func push(_ route: AppRoute, in tab: MainTab? = nil) {
        paths[tab ?? selectedTab, default: []].append(route)
    }

    func pop(_ tab: MainTab? = nil) {
        let tab = tab ?? selectedTab
        guard !(paths[tab] ?? []).isEmpty else { return }
        paths[tab]?.removeLast()
    }

    func popToRoot(_ tab: MainTab? = nil) {
        paths[tab ?? selectedTab] = []
    }

    /// Тап по пункту таб-бара. Повторный тап по активному разделу возвращает
    /// его на корневой экран.
    func select(_ tab: MainTab) {
        if selectedTab == tab {
            popToRoot(tab)
        } else {
            selectedTab = tab
        }
    }
}
