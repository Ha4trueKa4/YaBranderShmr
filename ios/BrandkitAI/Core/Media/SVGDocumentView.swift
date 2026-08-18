import SwiftUI

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

    @State private var phase: Phase
    @State private var isRendered = false

    private static let memoryCache: NSCache<NSURL, NSString> = {
        let cache = NSCache<NSURL, NSString>()
        cache.countLimit = 200
        return cache
    }()

    private static let session: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.urlCache = URLCache(
            memoryCapacity: 20 * 1024 * 1024,
            diskCapacity: 100 * 1024 * 1024,
            directory: FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
                .appendingPathComponent("svg-previews")
        )
        return URLSession(configuration: configuration)
    }()

    init(source: Source) {
        self.source = source
        _phase = State(initialValue: Self.initialPhase(for: source))
    }

    private static func initialPhase(for source: Source) -> Phase {
        switch source {
        case .text(let svg):
            .ready(svg)
        case .url(nil):
            .failure
        case .url(let url?):
            memoryCache.object(forKey: url as NSURL).map { .ready($0 as String) } ?? .loading
        }
    }

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
        let initial = Self.initialPhase(for: source)
        if phase != initial {
            isRendered = false
            phase = initial
        }

        guard case .loading = initial, case .url(let url?) = source else { return }
        await load(from: url)
    }

    private func load(from url: URL) async {
        do {
            let request = URLRequest(
                url: url,
                cachePolicy: .returnCacheDataElseLoad,
                timeoutInterval: 15
            )
            let (data, _) = try await Self.session.data(for: request)

            guard let svg = String(data: data, encoding: .utf8) else {
                show(.failure)
                return
            }

            Self.memoryCache.setObject(svg as NSString, forKey: url as NSURL)
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
