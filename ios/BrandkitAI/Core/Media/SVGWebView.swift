import SwiftUI
import WebKit

struct SVGWebView: UIViewRepresentable {
    let svgText: String
    var onRendered: (() -> Void)?

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = false
        configuration.suppressesIncrementalRendering = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
        webView.scrollView.isScrollEnabled = false
        webView.isUserInteractionEnabled = false
        webView.navigationDelegate = context.coordinator
        context.coordinator.loadedSVG = nil
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.onRendered = onRendered

        guard context.coordinator.loadedSVG != svgText else { return }
        context.coordinator.loadedSVG = svgText
        webView.loadHTMLString(Self.html(embedding: svgText), baseURL: nil)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        var loadedSVG: String?
        var onRendered: (() -> Void)?

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            onRendered?()
        }
    }

    private static func html(embedding svg: String) -> String {
        """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <style>
            html, body { margin: 0; height: 100%; background: transparent; }
            svg { width: 100vw; height: 100vh; display: block; }
        </style>
        </head>
        <body>\(svg)</body>
        </html>
        """
    }
}
