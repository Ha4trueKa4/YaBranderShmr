import Foundation

nonisolated protocol ProjectServiceProtocol: Sendable {
    func fetchProjects(for userID: UUID) async throws -> [Project]
    func fetchProject(id: UUID) async throws -> Project
    func createProject(name: String, description: String) async throws -> Project
    func deleteProject(id: UUID) async throws
    func updateVisibility(projectID: UUID, isPublic: Bool) async throws -> Project
    func fetchChat(projectID: UUID) async throws -> [RemoteChatMessage]
    func generate(projectID: UUID, prompt: String) async throws -> URL?
    func fetchVersionURL(projectID: UUID, versionID: UUID) async throws -> URL
}

nonisolated final class ProjectService: ProjectServiceProtocol {
    private let client: NetworkClient

    init(client: NetworkClient = NetworkClient.shared) {
        self.client = client
    }

    func fetchProjects(for userID: UUID) async throws -> [Project] {
        let dto: UserProjectsResponseDTO = try await client.request(
            .get,
            .userProfile(id: userID),
            authorization: .required
        )
        return (dto.projects ?? []).compactMap(Project.init(dto:))
    }

    func fetchProject(id: UUID) async throws -> Project {
        let dto: ProjectDTO = try await client.request(
            .get,
            .project(id: id),
            authorization: .required
        )
        return Project(dto: dto)
    }

    func createProject(name: String, description: String) async throws -> Project {
        let dto: ProjectDTO = try await client.request(
            .post,
            .projects,
            authorization: .required,
            body: CreateProjectRequest(name: name, description: description)
        )
        return Project(dto: dto)
    }

    func deleteProject(id: UUID) async throws {
        try await client.request(
            .delete,
            .project(id: id),
            authorization: .required
        )
    }

    func updateVisibility(projectID: UUID, isPublic: Bool) async throws -> Project {
        let dto: ProjectDTO = try await client.request(
            .patch,
            .project(id: projectID),
            authorization: .required,
            body: UpdateVisibilityRequest(isPublic: isPublic)
        )
        return Project(dto: dto)
    }

    func fetchChat(projectID: UUID) async throws -> [RemoteChatMessage] {
        let response: ChatHistoryResponseDTO = try await client.request(
            .get,
            .projectChat(projectID: projectID),
            authorization: .required
        )
        return response.messages
    }

    func generate(projectID: UUID, prompt: String) async throws -> URL? {
        let response: GenerateResponseDTO = try await client.request(
            .post,
            .generate(projectID: projectID),
            authorization: .required,
            body: GenerateProjectRequest(prompt: prompt),
            timeout: 180
        )
        return response.url
    }

    func fetchVersionURL(projectID: UUID, versionID: UUID) async throws -> URL {
        let response: VersionURLResponseDTO = try await client.request(
            .get,
            .projectVersion(projectID: projectID, versionID: versionID),
            authorization: .required
        )
        guard let url = response.url else { throw NetworkError.invalidResponse }
        return url
    }
}

nonisolated struct RemoteChatMessage: Sendable, Codable {
    let role: String
    let text: String?
    let versionID: UUID?
    let createdAt: Date
}

private nonisolated struct UserProjectsResponseDTO: Decodable, Sendable {
    let projects: [ProjectDTO]?
}

private nonisolated struct ProjectDTO: Decodable, Sendable {
    let id: UUID
    let authorID: UUID?
    let name: String
    let description: String?
    let isPublic: Bool?
    let createdAt: String?
    let url: URL?

    private enum CodingKeys: String, CodingKey {
        case id
        case authorID = "authorId"
        case name
        case description
        case isPublic
        case createdAt
        case url
    }
}

private nonisolated struct CreateProjectRequest: Encodable, Sendable {
    let name: String
    let description: String
}

private nonisolated struct UpdateVisibilityRequest: Encodable, Sendable {
    let isPublic: Bool
}

private nonisolated struct GenerateProjectRequest: Encodable, Sendable {
    let prompt: String
}

private nonisolated struct GenerateResponseDTO: Decodable, Sendable {
    let url: URL?
}

private nonisolated struct ChatHistoryResponseDTO: Decodable, Sendable {
    let chatHistory: [ChatMessageDTO]

    var messages: [RemoteChatMessage] {
        chatHistory.compactMap { message in
            let date = message.createdAt.flatMap(ISO8601DateFormatter.parse) ?? Date()
            return RemoteChatMessage(
                role: message.role,
                text: message.text,
                versionID: message.versionID,
                createdAt: date
            )
        }
    }
}

private nonisolated struct ChatMessageDTO: Decodable, Sendable {
    let role: String
    let text: String?
    let versionID: UUID?
    let createdAt: String?

    private enum CodingKeys: String, CodingKey {
        case role
        case text
        case versionID = "versionId"
        case createdAt
    }
}

private nonisolated struct VersionURLResponseDTO: Decodable, Sendable {
    let versionID: UUID?
    let url: URL?

    private enum CodingKeys: String, CodingKey {
        case versionID = "versionId"
        case url
    }
}

private extension Project {
    nonisolated init(dto: ProjectDTO) {
        self.init(
            id: dto.id,
            authorID: dto.authorID,
            name: dto.name,
            description: dto.description ?? "",
            isPublic: dto.isPublic ?? false,
            createdAt: dto.createdAt.flatMap(ISO8601DateFormatter.parse),
            url: dto.url
        )
    }
}

private extension ISO8601DateFormatter {
    nonisolated(unsafe) static let fractional: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    nonisolated(unsafe) static let plain: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    nonisolated static func parse(_ value: String) -> Date? {
        fractional.date(from: value) ?? plain.date(from: value)
    }
}
