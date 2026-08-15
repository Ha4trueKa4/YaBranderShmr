//
//  NetworkError.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 12.08.2026.
//

import Foundation

enum NetworkError: Error, Sendable {
    case invalidURL
    case invalidResponse
    case missingAccessToken
    case tokenStorageError
    case httpError(statusCode: Int)
    case decodingError
    case encodingError
}
