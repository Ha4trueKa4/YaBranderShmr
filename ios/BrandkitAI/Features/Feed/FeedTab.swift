import SwiftUI

enum FeedTab: String, CaseIterable, Hashable {
    case all = "All"
    case subscriptions = "Subscriptions"

    /// Подпись вкладки. `rawValue` остаётся техническим идентификатором.
    var title: LocalizedStringKey {
        switch self {
        case .all: "All"
        case .subscriptions: "Subscriptions"
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
