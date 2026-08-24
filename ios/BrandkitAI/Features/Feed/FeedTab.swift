import SwiftUI

enum FeedTab: String, CaseIterable, Hashable {
    case all = "All"
    case subscriptions = "Subscriptions"

    /// Локализованное название вкладки для UI.
    var title: String {
        switch self {
        case .all: String(localized: "All")
        case .subscriptions: String(localized: "Subscriptions")
        }
    }

    /// Раздел API, который стоит за вкладкой.
    var scope: FeedScope {
        switch self {
        case .all: .discover
        case .subscriptions: .following
        }
    }
}
