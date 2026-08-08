import SwiftUI

struct ProjectListItem: Identifiable, Hashable {
    let id = UUID()
    let title: String
    let subtitle: String
    let placeholderColors: [Color]
}

extension ProjectListItem {
    static let mocks: [ProjectListItem] = [
        ProjectListItem(
            title: "Chat 3",
            subtitle: "Prompt tweak · 15 min ago",
            placeholderColors: [.pink, .purple]
        ),
        ProjectListItem(
            title: "Chat 2",
            subtitle: "Sarah Chen · 1 hr ago",
            placeholderColors: [.green, .blue]
        ),
        ProjectListItem(
            title: "Chat 1",
            subtitle: "Sarah Chen · 1 hr ago",
            placeholderColors: [.purple, .green]
        )
    ]
}
