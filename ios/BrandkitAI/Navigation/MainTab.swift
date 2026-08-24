import Foundation

enum MainTab: String, CaseIterable, Identifiable, Hashable {
    case feed
    case create
    case profile

    var id: Self { self }

    var title: String {
        switch self {
        case .feed: String(localized: "Feed")
        case .create: String(localized: "Create")
        case .profile: String(localized: "Profile")
        }
    }

    var icon: String {
        switch self {
        case .feed: "newspaper"
        case .create: "plus.circle"
        case .profile: "person"
        }
    }

}
