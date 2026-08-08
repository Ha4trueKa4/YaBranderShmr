import SwiftUI

struct RoutedNavigationStack: View {
    @Bindable var router: Router
    let root: Route

    var body: some View {
        NavigationStack(path: $router.path) {
            RouteDestination(route: root)
                .navigationDestination(for: Route.self) { route in
                    RouteDestination(route: route)
                }
        }
        .sheet(item: $router.sheet) { route in
            RouteDestination.SheetDestination(route: route)
        }
        .environment(router)
    }
}
