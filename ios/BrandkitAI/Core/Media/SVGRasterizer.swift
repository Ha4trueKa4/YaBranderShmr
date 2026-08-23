import WebKit

@MainActor
final class SVGRasterizer: NSObject {
    enum RasterizationError: Error {
        case renderingFailed
        case timedOut
        case webContentTerminated
    }

    private static let loadTimeout: Duration = .seconds(10)

    private var webView: WKWebView?
    private var loadContinuation: CheckedContinuation<Void, Error>?
    private var timeoutTask: Task<Void, Never>?

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
        defer {
            finishLoad(.failure(CancellationError()))
            self.webView = nil
        }

        try await loadBlankPage(in: webView)
        try Task.checkCancellation()

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

    private func loadBlankPage(in webView: WKWebView) async throws {
        try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                loadContinuation = continuation
                startLoadTimeout()
                webView.loadHTMLString("<!DOCTYPE html><html><body></body></html>", baseURL: nil)
            }
        } onCancel: {
            Task { @MainActor [weak self] in
                self?.finishLoad(.failure(CancellationError()))
            }
        }
    }

    private func startLoadTimeout() {
        timeoutTask?.cancel()
        timeoutTask = Task { [weak self] in
            try? await Task.sleep(for: Self.loadTimeout)
            guard !Task.isCancelled else { return }
            self?.finishLoad(.failure(RasterizationError.timedOut))
        }
    }

    private func finishLoad(_ result: Result<Void, Error>) {
        timeoutTask?.cancel()
        timeoutTask = nil

        guard let continuation = loadContinuation else { return }
        loadContinuation = nil
        continuation.resume(with: result)
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
        finishLoad(.success(()))
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        finishLoad(.failure(error))
    }

    func webView(
        _ webView: WKWebView,
        didFailProvisionalNavigation navigation: WKNavigation!,
        withError error: Error
    ) {
        finishLoad(.failure(error))
    }
    
    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        finishLoad(.failure(RasterizationError.webContentTerminated))
    }
}
