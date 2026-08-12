//
//  NetworkClient.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 12.08.2026.
//

import Foundation

nonisolated final class NetworkClient: NetworkClientProtocol {
    private let baseURL: String
    private let token: String
    private let session: URLSession
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    init(baseURL: String, token: String, timeout: TimeInterval = 15) {
        self.baseURL = baseURL
        self.token = token
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = timeout
        config.timeoutIntervalForResource = timeout
        self.session = URLSession(configuration: config)
        self.encoder = JSONEncoder()
        self.decoder = JSONDecoder()
    }

    func request<Response: Decodable>(
        _ method: HTTPMethod,
        path: String,
        body: (some Encodable)?
    ) async throws -> Response {
        let data = try await execute(method: method, path: path, body: body)
        do {
            return try decoder.decode(Response.self, from: data)
        } catch {
            throw NetworkError.decodingError(error)
        }
    }

    func request(
        _ method: HTTPMethod,
        path: String,
        body: (some Encodable)?
    ) async throws {
        _ = try await execute(method: method, path: path, body: body)
    }

    // MARK: - Private

    private func execute(
        method: HTTPMethod,
        path: String,
        body: (some Encodable)?
    ) async throws -> Data {
        let shouldRetry = method == .get
        if !shouldRetry {
            return try await rawRequest(method: method, path: path, body: body)
        }
        return try await rawRequestWithRetry(method: method, path: path, body: body)
    }

    private func rawRequest(
        method: HTTPMethod,
        path: String,
        body: (some Encodable)?
    ) async throws -> Data {
        guard let url = URL(string: baseURL + path) else {
            throw NetworkError.invalidURL
        }

        var urlRequest = URLRequest(url: url, timeoutInterval: session.configuration.timeoutIntervalForRequest)
        urlRequest.httpMethod = method.rawValue
        urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let body {
            do {
                urlRequest.httpBody = try encoder.encode(body)
            } catch {
                throw NetworkError.encodingError(error)
            }
        }

        let (data, response) = try await session.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            let bodyString = String(data: data, encoding: .utf8) ?? "<non-utf8>"
            print("[NetworkClient] HTTP \(httpResponse.statusCode) for \(method.rawValue) \(path): \(bodyString)")
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
        body: (some Encodable)?
    ) async throws -> Data {
        var attempt = 0
        var delayNs = Self.minDelay
        while true {
            do {
                return try await rawRequest(method: method, path: path, body: body)
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
