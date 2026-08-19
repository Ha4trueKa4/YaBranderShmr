import Foundation

@MainActor
@Observable
final class ChatImagePreviewViewModel {
    let projectID: UUID
    let title: String
    let versionID: UUID
    let publishedText: String

    private let service: any ProjectServiceProtocol

    init(
        projectID: UUID,
        title: String,
        versionID: UUID = UUID(),
        publishedText: String = "Generated project version",
        service: any ProjectServiceProtocol = ProjectService()
    ) {
        self.projectID = projectID
        self.title = title
        self.versionID = versionID
        self.publishedText = publishedText
        self.service = service
    }

    func versionURL() async throws -> URL {
        try await service.fetchVersionURL(projectID: projectID, versionID: versionID)
    }
}
