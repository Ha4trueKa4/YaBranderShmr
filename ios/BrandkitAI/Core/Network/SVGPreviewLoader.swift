import CoreGraphics
import Foundation
import SVGView

nonisolated struct SVGPreviewLoader {
    enum LoaderError: Error {
        case invalidSVG
    }

    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    func load(from url: URL) async throws -> SVGPreview {
        let request = URLRequest(
            url: url,
            cachePolicy: .returnCacheDataElseLoad,
            timeoutInterval: 15
        )
        let (data, _) = try await session.data(for: request)

        guard let node = SVGParser.parse(data: data) else {
            throw LoaderError.invalidSVG
        }

        return SVGPreview(node: node, aspectRatio: Self.aspectRatio(of: node))
    }

    private static func aspectRatio(of node: SVGNode) -> CGFloat {
        guard let viewport = node as? SVGViewport,
              let viewBox = viewport.viewBox,
              viewBox.width > 0, viewBox.height > 0
        else { return 1 }

        return viewBox.width / viewBox.height
    }
}
