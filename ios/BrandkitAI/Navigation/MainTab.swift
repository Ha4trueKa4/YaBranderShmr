import SwiftUI

enum MainTab: String, CaseIterable, Identifiable, Hashable {
    case feed
    case create
    case profile

    var id: Self { self }

    var title: LocalizedStringKey {
        switch self {
        case .feed: "Feed"
        case .create: "Create"
        case .profile: "Profile"
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
