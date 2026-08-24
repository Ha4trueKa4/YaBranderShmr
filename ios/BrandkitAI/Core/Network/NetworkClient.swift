//
//  NetworkClient.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 12.08.2026.
//

import Foundation
import OSLog

nonisolated final class NetworkClient: NetworkClientProtocol, Sendable {
    private let baseURL: String
    private let tokenStore: TokenStore
    private let session: URLSession
    private let longSession: URLSession

    private static let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier ?? "com.brandkitai",
        category: "NetworkClient"
    )

    init(baseURL: String, tokenStore: TokenStore = TokenStore(), timeout: TimeInterval = 15) {
        self.baseURL = baseURL
        self.tokenStore = tokenStore

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = timeout
        config.timeoutIntervalForResource = timeout
        self.session = URLSession(configuration: config)

        let longConfig = URLSessionConfiguration.default
        longConfig.timeoutIntervalForRequest = 300
        longConfig.timeoutIntervalForResource = 300
        self.longSession = URLSession(configuration: longConfig)
    }

    static let shared = NetworkClient(baseURL: APIConfig.baseURL)

    func request<Response: Decodable & Sendable>(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?
    ) async throws -> Response {
        let bodyData = try encode(body)
        let data = try await execute(
            method: method,
            path: path,
            authorization: authorization,
            body: bodyData
        )
        return try decode(Response.self, from: data)
    }

    func request(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?
    ) async throws {
        let bodyData = try encode(body)
        _ = try await execute(
            method: method,
            path: path,
            authorization: authorization,
            body: bodyData
        )
    }

    func request<Response: Decodable & Sendable>(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement
    ) async throws -> Response {
        let data = try await execute(
            method: method,
            path: path,
            authorization: authorization,
            body: nil
        )
        return try decode(Response.self, from: data)
    }

    func request(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement
    ) async throws {
        _ = try await execute(
            method: method,
            path: path,
            authorization: authorization,
            body: nil
        )
    }

    func request<Response: Decodable & Sendable>(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?,
        timeout: TimeInterval?,
        retries: Bool = true
    ) async throws -> Response {
        let bodyData = try encode(body)
        let data = try await execute(
            method: method,
            path: path,
            authorization: authorization,
            body: bodyData,
            timeout: timeout,
            retries: retries
        )
        return try decode(Response.self, from: data)
    }

    // MARK: - Кодеки

    private static let jsonDecoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return decoder
    }()

    private static let jsonEncoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        return encoder
    }()

    // MARK: - Private

    private func decode<Response: Decodable & Sendable>(
        _ type: Response.Type,
        from data: Data
    ) throws -> Response {
        do {
            return try Self.jsonDecoder.decode(type, from: data)
        } catch {
            logDecodingError(error, responseType: String(describing: type))
            throw NetworkError.decodingError
        }
    }

    private func logDecodingError(_ error: Error, responseType: String) {
        let details: String

        switch error {
        case let DecodingError.keyNotFound(key, context):
            details = "keyNotFound(\(key.stringValue)): \(context.debugDescription)"
                + "; path=\(codingPathDescription(context.codingPath))"

        case let DecodingError.typeMismatch(type, context):
            details = "typeMismatch(\(type)): \(context.debugDescription)"
                + "; path=\(codingPathDescription(context.codingPath))"

        case let DecodingError.valueNotFound(type, context):
            details = "valueNotFound(\(type)): \(context.debugDescription)"
                + "; path=\(codingPathDescription(context.codingPath))"

        case let DecodingError.dataCorrupted(context):
            details = "dataCorrupted: \(context.debugDescription)"
                + "; path=\(codingPathDescription(context.codingPath))"

        default:
            details = String(describing: error)
        }

        Self.logger.error("Decoding \(responseType, privacy: .public) failed: \(details, privacy: .public)")
    }

    private func codingPathDescription(_ path: [CodingKey]) -> String {
        path.enumerated().map { index, key in
            if let arrayIndex = key.intValue {
                return "[\(arrayIndex)]"
            }
            return index == 0 ? key.stringValue : ".\(key.stringValue)"
        }
        .joined()
    }

    private func encode(_ body: (some Encodable & Sendable)?) throws -> Data? {
        guard let body else { return nil }
        do {
            return try Self.jsonEncoder.encode(body)
        } catch {
            throw NetworkError.encodingError
        }
    }

    private func execute(
        method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: Data?,
        timeout: TimeInterval? = nil,
        retries: Bool = true
    ) async throws -> Data {
        let shouldRetry = retries && method == .get
        if !shouldRetry {
            return try await rawRequest(
                method: method,
                path: path,
                authorization: authorization,
                body: body,
                timeout: timeout
            )
        }
        return try await rawRequestWithRetry(
            method: method,
            path: path,
            authorization: authorization,
            body: body,
            timeout: timeout
        )
    }

    private func rawRequest(
        method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: Data?,
        timeout: TimeInterval? = nil
    ) async throws -> Data {
        guard let url = URL(string: baseURL + path) else {
            throw NetworkError.invalidURL
        }

        let requestTimeout = timeout ?? session.configuration.timeoutIntervalForRequest
        var urlRequest = URLRequest(url: url, timeoutInterval: requestTimeout)
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
            urlRequest.httpBody = body
        }

        let (data, response) = try await (timeout != nil ? longSession : session).data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        Self.logger.info("\(method.rawValue, privacy: .public) \(path, privacy: .public) -> HTTP \(httpResponse.statusCode)")

        guard (200...299).contains(httpResponse.statusCode) else {
            let serverError = try? Self.jsonDecoder.decode(ServerErrorResponse.self, from: data)
            // «Генерация уже идёт» выводим из статус-кода + jobId, а не из тела:
            // иначе любой ответ с jobId в теле (или 409, к которому бэкенд
            // допишет code) уедет в serverValidation/httpError вместо
            // прицепления к существующей job.
            if httpResponse.statusCode == 409, let jobID = serverError?.jobId {
                throw NetworkError.generationInProgress(jobID: jobID)
            }
            if serverError?.code != nil || serverError?.field != nil {
                throw NetworkError.serverValidation(
                    statusCode: httpResponse.statusCode,
                    code: serverError?.code,
                    field: serverError?.field
                )
            }
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
        body: Data?,
        timeout: TimeInterval? = nil
    ) async throws -> Data {
        var attempt = 0
        var delayNs = Self.minDelay
        while true {
            do {
                return try await rawRequest(
                    method: method,
                    path: path,
                    authorization: authorization,
                    body: body,
                    timeout: timeout
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

nonisolated private struct ServerErrorResponse: Decodable, Sendable {
    let code: String?
    let field: String?
    let message: String?
    let jobId: String?
}
