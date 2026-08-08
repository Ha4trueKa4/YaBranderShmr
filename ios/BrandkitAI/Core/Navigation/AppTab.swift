import Foundation

enum AppTab: String, CaseIterable, Identifiable, Hashable {
    case feed
    case create
    case profile

    var id: Self { self }

    var title: String {
        switch self {
        case .feed: "Feed"
        case .create: "Create"
        case .profile: "Profile"
        }
    }

    var systemImage: String {
        switch self {
        case .feed: "square.grid.2x2"
        case .create: "plus.circle"
        case .profile: "person"
        }
    }

    var root: Route {
        switch self {
        case .feed: .feed
        case .create: .projectCreate
        case .profile: .profile(.current)
        }
    }
}
