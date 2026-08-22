import Foundation
import SwiftUI

struct ProjectListItem: Identifiable, Hashable {
    let id: UUID
    let title: String
    let subtitle: String
    let previewURL: URL?
    let isPrivate: Bool
    let placeholderColors: [Color]

    init(
        id: UUID = UUID(),
        title: String,
        subtitle: String,
        previewURL: URL? = nil,
        isPrivate: Bool = true,
        placeholderColors: [Color]
    ) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.previewURL = previewURL
        self.isPrivate = isPrivate
        self.placeholderColors = placeholderColors
    }
}

extension ProjectListItem {
    static let mocks: [ProjectListItem] = [
        ProjectListItem(
            id: UUID(),
            title: "Chat 3",
            subtitle: "Prompt tweak · 15 min ago",
            placeholderColors: [.pink, .purple]
        ),
        ProjectListItem(
            id: UUID(),
            title: "Chat 2",
            subtitle: "Sarah Chen · 1 hr ago",
            placeholderColors: [.green, .blue]
        ),
        ProjectListItem(
            id: UUID(),
            title: "Chat 1",
            subtitle: "Sarah Chen · 1 hr ago",
            placeholderColors: [.purple, .green]
        )
    ]
}
