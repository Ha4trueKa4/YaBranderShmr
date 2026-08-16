//
//  NetworkClientProtocol.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 12.08.2026.
//

import Foundation

nonisolated protocol NetworkClientProtocol: Sendable {
    func request<Response: Decodable & Sendable>(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?
    ) async throws -> Response

    func request(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement,
        body: (some Encodable & Sendable)?
    ) async throws

    func request<Response: Decodable & Sendable>(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement
    ) async throws -> Response

    func request(
        _ method: HTTPMethod,
        path: String,
        authorization: AuthorizationRequirement
    ) async throws
}
