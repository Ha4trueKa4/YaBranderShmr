import Foundation

@MainActor
@Observable
final class Router {
    var path: [Route] = []

    var sheet: SheetRoute?

    var canPop: Bool { !path.isEmpty }

    func push(_ route: Route) {
        path.append(route)
    }

    func pop() {
        guard canPop else { return }
        path.removeLast()
    }

    func popToRoot() {
        path.removeAll()
    }

    func setPath(_ routes: [Route]) {
        path = routes
    }

    func present(_ sheet: SheetRoute) {
        self.sheet = sheet
    }

    func dismissSheet() {
        sheet = nil
    }

    func reset() {
        path.removeAll()
        sheet = nil
    }
}
