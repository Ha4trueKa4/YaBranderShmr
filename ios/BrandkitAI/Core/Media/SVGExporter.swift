import Foundation

nonisolated enum SVGExporter {
    static func writeSVGFile(_ data: Data, named baseName: String) throws -> URL {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("export", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)

        let url = directory
            .appendingPathComponent(sanitized(baseName))
            .appendingPathExtension("svg")
        try data.write(to: url, options: .atomic)
        return url
    }

    private static func sanitized(_ name: String) -> String {
        let invalid = CharacterSet(charactersIn: "/\\:?%*|\"<>")
        let clean = name
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .components(separatedBy: invalid)
            .joined()
            .prefix(60)
        return clean.isEmpty ? "brandkit" : String(clean)
    }
}
