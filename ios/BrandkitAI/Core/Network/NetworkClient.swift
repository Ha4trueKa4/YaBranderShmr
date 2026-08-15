//
//  NetworkClient.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 12.08.2026.
//

import Foundation

nonisolated final class NetworkClient: NetworkClientProtocol, Sendable {
    private let baseURL: String
    private let tokenStore: TokenStore
    private let session: URLSession

    init(baseURL: String, tokenStore: TokenStore = TokenStore(), timeout: TimeInterval = 15) {
        self.baseURL = baseURL
        self.tokenStore = tokenStore
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = timeout
        config.timeoutIntervalForResource = timeout
        self.session = URLSession(configuration: config)
    }

    func request<Response: Decodable & Sendable>(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?
    ) async throws -> Response {
        let data = try await execute(
            method: method,
            path: path,
            authorization: authorization,
            body: body
        )
        do {
            return try JSONDecoder().decode(Response.self, from: data)
        } catch {
            throw NetworkError.decodingError
        }
    }

    func request(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?
    ) async throws {
        _ = try await execute(
            method: method,
            path: path,
            authorization: authorization,
            body: body
        )
    }

    // MARK: - Private

    private func execute(
        method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?
    ) async throws -> Data {
        let shouldRetry = method == .get
        if !shouldRetry {
            return try await rawRequest(
                method: method,
                path: path,
                authorization: authorization,
                body: body
            )
        }
        return try await rawRequestWithRetry(
            method: method,
            path: path,
            authorization: authorization,
            body: body
        )
    }

    private func rawRequest(
        method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?
    ) async throws -> Data {
        guard let url = URL(string: baseURL + path) else {
            throw NetworkError.invalidURL
        }

        var urlRequest = URLRequest(url: url, timeoutInterval: session.configuration.timeoutIntervalForRequest)
        urlRequest.httpMethod = method.rawValue
        switch authorization {
        case .none:
            break

        case .optional:
            if let token = tokenStore.accessToken, !token.isEmpty {
                urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }

        case .required:
            guard let token = tokenStore.accessToken, !token.isEmpty else {
                throw NetworkError.missingAccessToken
            }
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let body {
            do {
                urlRequest.httpBody = try JSONEncoder().encode(body)
            } catch {
                throw NetworkError.encodingError
            }
        }

        let (data, response) = try await session.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            throw NetworkError.httpError(statusCode: httpResponse.statusCode)
        }

        return data
    }

    // MARK: - Retry (GET only)

    private static let retryableStatusCodes: Set<Int> = [408, 429, 500, 502, 503, 504]
    private static let minDelay: UInt64 = 1_000_000_000           // 1 sec
    private static let maxDelay: UInt64 = 15 * 1_000_000_000      // 15 sec
    private static let factor: Double = 2
    private static let jitter: Double = 0.05
    private static let maxAttempts: Int = 3

    private func rawRequestWithRetry(
        method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?
    ) async throws -> Data {
        var attempt = 0
        var delayNs = Self.minDelay
        while true {
            do {
                return try await rawRequest(
                    method: method,
                    path: path,
                    authorization: authorization,
                    body: body
                )
            } catch NetworkError.httpError(let code) where Self.retryableStatusCodes.contains(code) {
                if attempt >= Self.maxAttempts {
                    throw NetworkError.httpError(statusCode: code)
                }
                let jitterNs = Double(delayNs) * Double.random(in: -Self.jitter...Self.jitter)
                let sleepNs = UInt64(max(Double(Self.minDelay), Double(delayNs) + jitterNs))
                try await Task.sleep(nanoseconds: sleepNs)
                delayNs = min(Self.maxDelay, UInt64(Double(delayNs) * Self.factor))
                attempt += 1
            }
        }
    }
}

// MARK: - Auth DTOs

nonisolated struct LoginRequest: Encodable, Sendable {
    let email: String
    let password: String
}

nonisolated struct RegisterRequest: Encodable, Sendable {
    let email: String
    let password: String
    let nickname: String
}

nonisolated struct AuthResponse: Decodable, Sendable {
    let accessToken: String
    let tokenType: String
    let user: AuthUser

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case tokenType = "token_type"
        case user
    }
}

nonisolated struct AuthUser: Decodable, Sendable {
    let id: String
    let email: String
    let nickname: String
}
