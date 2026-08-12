//
//  NetworkClientProtocol.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 12.08.2026.
//

import Foundation

enum HTTPMethod: String, Sendable {
    case get = "GET"
    case post = "POST"
    case patch = "PATCH"
    case put = "PUT"
    case delete = "DELETE"
}

nonisolated protocol NetworkClientProtocol: Sendable {
    func request<Response: Decodable & Sendable>(
        _ method: HTTPMethod,
        path: String,
        body: (some Encodable & Sendable)?
    ) async throws -> Response

    func request(
        _ method: HTTPMethod,
        path: String,
        body: (some Encodable & Sendable)?
    ) async throws
}
