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
    case serverValidation(statusCode: Int, code: String?, field: String?)
    case decodingError
    case encodingError
    case generationInProgress(jobID: String)
}
