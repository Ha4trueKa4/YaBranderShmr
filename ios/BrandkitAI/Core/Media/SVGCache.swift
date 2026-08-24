import CryptoKit
import Foundation

actor SVGCache {
    static let shared = SVGCache()

    /// Ключ кэша для версии проекта. Используется в чате и превью: подписанный
    /// URL меняется каждый час, поэтому кэш привязывается к id версии, а не к ссылке.
    static func versionKey(projectID: UUID, versionID: UUID) -> String {
        "version:\(projectID.uuidString.lowercased()):\(versionID.uuidString.lowercased())"
    }

    private let memoryCache = NSCache<NSString, NSData>()
    private let session: URLSession
    private let directory: URL
    private var inFlight: [String: Task<Data, Error>] = [:]

    init() {
        let configuration = URLSessionConfiguration.default
        configuration.urlCache = nil
        configuration.timeoutIntervalForRequest = 15
        configuration.timeoutIntervalForResource = 15
        session = URLSession(configuration: configuration)

        directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("svg-previews", isDirectory: true)
        memoryCache.countLimit = 200
    }

    func dataIfCached(for key: String) -> Data? {
        let cacheKey = key as NSString
        if let data = memoryCache.object(forKey: cacheKey) {
            return data as Data
        }

        guard let data = try? Data(contentsOf: fileURL(for: key)) else { return nil }
        memoryCache.setObject(data as NSData, forKey: cacheKey)
        return data
    }

    func data(for url: URL, key: String) async throws -> Data {
        if let cached = dataIfCached(for: key) {
            return cached
        }

        if let task = inFlight[key] {
            return try await task.value
        }

        let session = session
        let task = Task<Data, Error> {
            let request = URLRequest(
                url: url,
                cachePolicy: .reloadIgnoringLocalCacheData,
                timeoutInterval: 15
            )
            let (data, response) = try await session.data(for: request)
            guard let response = response as? HTTPURLResponse,
                  (200...299).contains(response.statusCode),
                  String(data: data, encoding: .utf8) != nil
            else {
                throw NetworkError.invalidResponse
            }
            self.store(data, for: key)
            return data
        }
        inFlight[key] = task

        do {
            let data = try await task.value
            inFlight[key] = nil
            return data
        } catch {
            inFlight[key] = nil
            throw error
        }
    }

    private func store(_ data: Data, for key: String) {
        memoryCache.setObject(data as NSData, forKey: key as NSString)

        do {
            try FileManager.default.createDirectory(
                at: directory,
                withIntermediateDirectories: true
            )
            try data.write(to: fileURL(for: key), options: .atomic)
        } catch {
            // The memory cache still works if disk storage is unavailable.
        }
    }

    private func fileURL(for key: String) -> URL {
        let digest = SHA256.hash(data: Data(key.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
        return directory.appendingPathComponent(digest).appendingPathExtension("svg")
    }
}
