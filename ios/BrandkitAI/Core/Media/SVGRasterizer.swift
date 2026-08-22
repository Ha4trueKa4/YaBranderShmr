import WebKit

@MainActor
final class SVGRasterizer: NSObject {
    enum RasterizationError: Error {
        case renderingFailed
    }

    private var webView: WKWebView?
    private var loadContinuation: CheckedContinuation<Void, Error>?

    static func pngData(fromSVG svgText: String, maxPixelSide: Int = 2048) async throws -> Data {
        try await SVGRasterizer().render(svgText: svgText, maxPixelSide: maxPixelSide)
    }

    private func render(svgText: String, maxPixelSide: Int) async throws -> Data {
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.suppressesIncrementalRendering = true

        let webView = WKWebView(frame: CGRect(x: 0, y: 0, width: 1, height: 1), configuration: configuration)
        webView.navigationDelegate = self
        self.webView = webView
        defer { self.webView = nil }

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            loadContinuation = continuation
            webView.loadHTMLString("<!DOCTYPE html><html><body></body></html>", baseURL: nil)
        }

        let result = try await webView.callAsyncJavaScript(
            Self.rasterizeScript,
            arguments: ["svgText": svgText, "maxSide": maxPixelSide],
            contentWorld: .page
        )

        guard let base64 = result as? String,
              let data = Data(base64Encoded: base64),
              !data.isEmpty
        else {
            throw RasterizationError.renderingFailed
        }
        return data
    }

    private static let rasterizeScript = """
    const url = URL.createObjectURL(new Blob([svgText], { type: "image/svg+xml" }));
    try {
        const image = new Image();
        await new Promise((resolve, reject) => {
            setTimeout(() => reject(new Error("timeout")), 10000);
            image.onload = resolve;
            image.onerror = () => reject(new Error("svg load failed"));
            image.src = url;
        });
        const ratio = image.naturalWidth > 0 && image.naturalHeight > 0
            ? image.naturalWidth / image.naturalHeight
            : 1;
        const width = Math.round(ratio >= 1 ? maxSide : maxSide * ratio);
        const height = Math.round(ratio >= 1 ? maxSide / ratio : maxSide);
        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        canvas.getContext("2d").drawImage(image, 0, 0, width, height);
        return canvas.toDataURL("image/png").split(",")[1];
    } finally {
        URL.revokeObjectURL(url);
    }
    """
}

extension SVGRasterizer: WKNavigationDelegate {
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        loadContinuation?.resume()
        loadContinuation = nil
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        loadContinuation?.resume(throwing: error)
        loadContinuation = nil
    }

    func webView(
        _ webView: WKWebView,
        didFailProvisionalNavigation navigation: WKNavigation!,
        withError error: Error
    ) {
        loadContinuation?.resume(throwing: error)
        loadContinuation = nil
    }
}
