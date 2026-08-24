import Foundation

nonisolated protocol ProjectServiceProtocol: Sendable {
    func fetchProjects(for userID: UUID) async throws -> [Project]
    func fetchProject(id: UUID) async throws -> Project
    func createProject(name: String, description: String) async throws -> Project
    func deleteProject(id: UUID) async throws
    func updateVisibility(projectID: UUID, isPublic: Bool) async throws -> Project
    func fetchChat(projectID: UUID) async throws -> [RemoteChatMessage]
    func fetchModels() async throws -> [GenerationModel]
    func generate(projectID: UUID, prompt: String, modelID: String, clientRequestID: String?) async throws -> String
    func generateStatus(projectID: UUID, jobID: String) async throws -> GenerationStatus
    func cancelGeneration(projectID: UUID, jobID: String) async throws -> GenerationStatus
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
            authorization: .optional
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

    func fetchModels() async throws -> [GenerationModel] {
        let response: ModelsResponseDTO = try await client.request(
            .get,
            .models,
            authorization: .none
        )
        return response.data.map {
            GenerationModel(remoteID: $0.id, owner: $0.ownedBy)
        }
    }

    func generate(
        projectID: UUID,
        prompt: String,
        modelID: String,
        clientRequestID: String?
    ) async throws -> String {
        let response: GenerateResponseDTO = try await client.request(
            .post,
            .generate(projectID: projectID),
            authorization: .required,
            body: GenerateProjectRequest(
                prompt: prompt,
                model: modelID,
                clientRequestId: clientRequestID
            ),
            timeout: 180
        )
        guard let jobID = response.jobID else { throw NetworkError.invalidResponse }
        return jobID
    }

    func generateStatus(projectID: UUID, jobID: String) async throws -> GenerationStatus {
        // Без NetworkClient-retry: retry-политикой опроса владеет
        // ChatViewModel.pollGeneration — иначе попытки двух слоёв
        // перемножаются и ожидание растягивается на десятки минут.
        try await client.request(
            .get,
            .generateStatus(projectID: projectID, jobID: jobID),
            authorization: .required,
            body: Optional<String>.none,
            timeout: 60,
            retries: false
        )
    }

    func cancelGeneration(projectID: UUID, jobID: String) async throws -> GenerationStatus {
        try await client.request(
            .post,
            .cancelGeneration(projectID: projectID, jobID: jobID),
            authorization: .required
        )
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
    let error: String?
    let status: String?
    let jobID: String?
    let createdAt: Date

    init(
        role: String,
        text: String?,
        versionID: UUID?,
        error: String? = nil,
        status: String? = nil,
        jobID: String? = nil,
        createdAt: Date
    ) {
        self.role = role
        self.text = text
        self.versionID = versionID
        self.error = error
        self.status = status
        self.jobID = jobID
        self.createdAt = createdAt
    }
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
    let model: String
    /// Стабильный идентификатор логической отправки (uuid баббла сообщения):
    /// одинаков при всех повторных попытках одного сообщения. Сервер поле
    /// пока игнорирует; когда бэкенд поддержит идемпотентность по нему,
    /// повторный POST вернёт существующую job вместо второй генерации.
    let clientRequestId: String?

    private enum CodingKeys: String, CodingKey {
        case prompt
        case model
        case clientRequestId = "client_request_id"
    }
}

private nonisolated struct ModelsResponseDTO: Decodable, Sendable {
    let data: [ModelDTO]
}

private nonisolated struct ModelDTO: Decodable, Sendable {
    let id: String
    let ownedBy: String?
}

private nonisolated struct GenerateResponseDTO: Decodable, Sendable {
    let jobID: String?

    private enum CodingKeys: String, CodingKey {
        case jobID = "jobId"
    }
}

nonisolated enum GenerationStatus: Decodable, Sendable {
    enum Status: String, Decodable, Sendable {
        case pending
        case done
        case error
        case cancelling
        case cancelled
    }

    case pending
    case done(url: URL?)
    case error(message: String)
    case cancelling
    case cancelled

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let status = try container.decode(Status.self, forKey: .status)
        switch status {
        case .pending:
            self = .pending
        case .done:
            let url = try container.decodeIfPresent(URL.self, forKey: .url)
            self = .done(url: url)
        case .error:
            let message = try container.decodeIfPresent(String.self, forKey: .message) ?? String(localized: "Generation failed.")
            self = .error(message: message)
        case .cancelling:
            self = .cancelling
        case .cancelled:
            self = .cancelled
        }
    }

    private enum CodingKeys: String, CodingKey {
        case status
        case url
        case message
    }
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
                error: message.error,
                status: message.status,
                jobID: message.jobID,
                createdAt: date
            )
        }
    }
}

private nonisolated struct ChatMessageDTO: Decodable, Sendable {
    let role: String
    let text: String?
    let versionID: UUID?
    let error: String?
    let status: String?
    let jobID: String?
    let createdAt: String?

    private enum CodingKeys: String, CodingKey {
        case role
        case text
        case versionID = "versionId"
        case error
        case status
        case jobID = "jobId"
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
