import SwiftUI

/// Точный рендер SVG-документа через WebKit (`SVGWebView`) с состояниями
/// загрузки. Источники:
/// - `.text` — готовая SVG-строка (чат генерации, редактор: поле `data`);
/// - `.url` — удалённый файл; кеширование — HTTP-кешем `URLSession`
///   (лимиты подняты в `BrandkitAIApp.init`).
struct SVGDocumentView: View {
    enum Source: Equatable {
        case text(String)
        case url(URL?)
    }

    let source: Source

    private enum Phase: Equatable {
        case loading
        case ready(String)
        case failure
    }

    @State private var phase: Phase = .loading
    @State private var isRendered = false

    var body: some View {
        content
            .task(id: source) {
                await load()
            }
    }

    @ViewBuilder
    private var content: some View {
        switch phase {
        case .loading:
            ShimmerPlaceholder()

        case .ready(let svg):
            SVGWebView(svgText: svg) {
                withAnimation(.easeIn(duration: 0.2)) {
                    isRendered = true
                }
            }
            // Шиммер до конца рендера, чтобы не мигать белым webview.
            .opacity(isRendered ? 1 : 0)
            .overlay {
                if !isRendered {
                    ShimmerPlaceholder()
                }
            }

        case .failure:
            failurePlaceholder
        }
    }

    private var failurePlaceholder: some View {
        Image(systemName: "exclamationmark.triangle")
            .font(.system(size: 28, weight: .medium))
            .foregroundStyle(.secondary)
            .accessibilityLabel("Preview unavailable")
    }

    private func load() async {
        phase = .loading
        isRendered = false

        switch source {
        case .text(let svg):
            show(.ready(svg))

        case .url(nil):
            show(.failure)

        case .url(let url?):
            await load(from: url)
        }
    }

    private func load(from url: URL) async {
        do {
            let request = URLRequest(
                url: url,
                cachePolicy: .returnCacheDataElseLoad,
                timeoutInterval: 15
            )
            let (data, _) = try await URLSession.shared.data(for: request)

            guard let svg = String(data: data, encoding: .utf8) else {
                show(.failure)
                return
            }

            show(.ready(svg))
        } catch {
            guard !Task.isCancelled else { return }
            show(.failure)
        }
    }

    private func show(_ newPhase: Phase) {
        withAnimation(.easeIn(duration: 0.2)) {
            phase = newPhase
        }
    }
}
