import Foundation

nonisolated enum APIEndpoint {
    // Auth
    case register
    case login

    // Users
    case userProfile(id: UUID)
    case subscribe(userID: UUID)
    case subscribers(userID: UUID)
    case subscriptions(userID: UUID)

    // Projects
    case projects
    case project(id: UUID)
    case generate(projectID: UUID)

    // Feed
    case feed(scope: FeedScope, cursor: String?, limit: Int)

    var path: String {
        switch self {
        case .register:
            "/auth/register"

        case .login:
            "/auth/login"

        case .userProfile(let id):
            "/users/\(id.uuidString)"

        case .subscribe(let userID):
            "/users/\(userID.uuidString)/subscribe"

        case .subscribers(let userID):
            "/users/\(userID.uuidString)/subscribers"

        case .subscriptions(let userID):
            "/users/\(userID.uuidString)/subscriptions"

        case .projects:
            "/projects"

        case .project(let id):
            "/projects/\(id.uuidString)"

        case .generate(let projectID):
            "/projects/\(projectID.uuidString)/generate"

        case .feed(let scope, let cursor, let limit):
            Self.feedPath(scope: scope, cursor: cursor, limit: limit)
        }
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
