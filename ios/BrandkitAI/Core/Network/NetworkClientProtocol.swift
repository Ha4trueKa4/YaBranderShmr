//
//  NetworkClientProtocol.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 12.08.2026.
//

import Foundation

enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case delete = "DELETE"
}

nonisolated protocol NetworkClientProtocol {
    func request<Response: Decodable>(
        _ method: HTTPMethod,
        path: String,
        body: (some Encodable)?
    ) async throws -> Response

    func request(
        _ method: HTTPMethod,
        path: String,
        body: (some Encodable)?
    ) async throws
}
