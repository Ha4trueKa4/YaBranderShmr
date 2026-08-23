import Foundation

nonisolated enum APIEndpoint {
    // Auth
    case register
    case login
    case yandexAuth

    // Models
    case models

    // Users
    case userProfile(id: UUID)
    case subscribe(userID: UUID)
    case subscribers(userID: UUID)
    case subscriptions(userID: UUID)

    // Projects
    case projects
    case project(id: UUID)
    case generate(projectID: UUID)
    case generateStatus(projectID: UUID, jobID: String)
    case cancelGeneration(projectID: UUID, jobID: String)
    case projectChat(projectID: UUID)
    case projectVersions(projectID: UUID)
    case projectVersion(projectID: UUID, versionID: UUID)

    // Feed
    case feed(scope: FeedScope, cursor: String?, limit: Int)

    var path: String {
        switch self {
        case .register:
            "/auth/register"

        case .login:
            "/auth/login"

        case .yandexAuth:
            "/auth/yandex"

        case .models:
            "/models"

        case .userProfile(let id):
            "/users/\(Self.pathID(id))"

        case .subscribe(let userID):
            "/users/\(Self.pathID(userID))/subscribe"

        case .subscribers(let userID):
            "/users/\(Self.pathID(userID))/subscribers"

        case .subscriptions(let userID):
            "/users/\(Self.pathID(userID))/subscriptions"

        case .projects:
            "/projects"

        case .project(let id):
            "/projects/\(Self.pathID(id))"

        case .generate(let projectID):
            "/projects/\(Self.pathID(projectID))/generate"

        case .generateStatus(let projectID, let jobID):
            "/projects/\(Self.pathID(projectID))/generate/\(jobID)"

        case .cancelGeneration(let projectID, let jobID):
            "/projects/\(Self.pathID(projectID))/generate/\(jobID)/cancel"

        case .projectChat(let projectID):
            "/projects/\(Self.pathID(projectID))/chat"

        case .projectVersions(let projectID):
            "/projects/\(Self.pathID(projectID))/versions"

        case .projectVersion(let projectID, let versionID):
            "/projects/\(Self.pathID(projectID))/versions/\(Self.pathID(versionID))"

        case .feed(let scope, let cursor, let limit):
            Self.feedPath(scope: scope, cursor: cursor, limit: limit)
        }
    }

    private static func pathID(_ id: UUID) -> String {
        id.uuidString.lowercased()
    }

    private static func feedPath(scope: FeedScope, cursor: String?, limit: Int) -> String {
        var path = "/feed?scope=\(scope.rawValue)&limit=\(limit)"
        if let cursor,
           let encoded = cursor.addingPercentEncoding(withAllowedCharacters: .alphanumerics) {
            path += "&cursor=\(encoded)"
        }
        return path
    }
}

// MARK: - Вызов по эндпоинту

extension NetworkClientProtocol {
    func request<Response: Decodable & Sendable>(
        _ method: HTTPMethod,
        _ endpoint: APIEndpoint,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)? = Optional<String>.none
    ) async throws -> Response {
        try await request(method, path: endpoint.path, authorization: authorization, body: body)
    }

    func request(
        _ method: HTTPMethod,
        _ endpoint: APIEndpoint,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)? = Optional<String>.none
    ) async throws {
        try await request(method, path: endpoint.path, authorization: authorization, body: body)
    }
}

extension NetworkClient {
    func request<Response: Decodable & Sendable>(
        _ method: HTTPMethod,
        _ endpoint: APIEndpoint,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?,
        timeout: TimeInterval?
    ) async throws -> Response {
        try await request(method, path: endpoint.path, authorization: authorization, body: body, timeout: timeout)
    }
}
