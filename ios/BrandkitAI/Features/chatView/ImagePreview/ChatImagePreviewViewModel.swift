import Foundation

@MainActor
@Observable
final class ChatImagePreviewViewModel {
    let projectID: UUID
    let title: String
    let publishedText: String

    init(
        projectID: UUID,
        title: String,
        publishedText: String = "Published 3 days ago"
    ) {
        self.projectID = projectID
        self.title = title
        self.publishedText = publishedText
    }
}
