import SwiftUI

struct SVGDocumentView: View {
    enum Source: Equatable {
        case text(String)
        case url(URL?, cacheKey: String? = nil, isPrivate: Bool = false)

        var url: URL? {
            switch self {
            case .text:
                nil
            case .url(let url, _, _):
                url
            }
        }

        var cacheKey: String? {
            cacheKey(for: url)
        }

        var isPrivate: Bool {
            switch self {
            case .text:
                false
            case .url(_, _, let isPrivate):
                isPrivate
            }
        }

        /// Ключ кэша для указанного URL. Если задан явный `cacheKey`, он
        /// приоритетнее (используется для версий проекта, где подписанная
        /// ссылка меняется, а изображение — нет). Иначе для приватных ссылок
        /// отбрасываются query/fragment (подпись протухает, картинка та же).
        func cacheKey(for url: URL?) -> String? {
            switch self {
            case .text:
                return nil
            case .url(_, let explicit, let isPrivate):
                if let explicit { return explicit }
                guard let url else { return nil }
                guard isPrivate else { return url.absoluteString }

                var components = URLComponents(url: url, resolvingAgainstBaseURL: false)
                components?.query = nil
                components?.fragment = nil
                return components?.url?.absoluteString ?? url.absoluteString
            }
        }
    }

    private enum Phase: Equatable {
        case loading
        case ready(String)
        case failure
        case needsManualDownload
    }

    let source: Source
    let refreshURL: (@Sendable () async throws -> URL)?
    let onTap: (() -> Void)?

    @State private var phase: Phase = .loading
    @State private var isRendered = false
    @State private var isManualLoading = false

    init(
        source: Source,
        refreshURL: (@Sendable () async throws -> URL)? = nil,
        onTap: (() -> Void)? = nil
    ) {
        self.source = source
        self.refreshURL = refreshURL
        self.onTap = onTap
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
            if let onTap {
                readyBase(svg)
                    .onTapGesture {
                        guard isRendered else { return }
                        onTap()
                    }
            } else {
                readyBase(svg)
            }

        case .failure:
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 28, weight: .medium))
                .foregroundStyle(.secondary)
                .accessibilityLabel("Preview unavailable")

        case .needsManualDownload:
            VStack(alignment: .center) {
                Button {
                    Task { await loadManually() }
                } label: {
                    if isManualLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity, minHeight: 44)
                    } else {
                        Label("Load image", systemImage: "arrow.down.circle")
                            .labelStyle(.iconOnly)
                            .font(.title)
                            .padding(4)
                    }
                }
                .buttonBorderShape(.circle)
                .buttonStyle(.bordered)
                .disabled(isManualLoading)
                .accessibilityHint("Downloads this private project image")
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func readyBase(_ svg: String) -> some View {
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
        .contentShape(Rectangle())
    }

    private func load() async {
        switch source {
        case .text(let svg):
            phase = .ready(svg)

        case .url(let url, _, let isPrivate):
            guard let cacheKey = source.cacheKey else {
                phase = .failure
                return
            }

            let cachedData = await SVGCache.shared.dataIfCached(for: cacheKey)
            guard !Task.isCancelled else { return }

            if let cachedData,
               let svg = String(data: cachedData, encoding: .utf8) {
                if case .ready(let current) = phase, current == svg {
                    return
                }
                isRendered = false
                show(.ready(svg))
                return
            }

            guard let url else {
                phase = refreshURL == nil ? .failure : .needsManualDownload
                return
            }

            if isPrivate && URLSignature.isExpired(url) {
                phase = .needsManualDownload
                return
            }

            await download(url: url, cacheKey: cacheKey)
        }
    }

    private func loadManually() async {
        guard case .url(_, _, let isPrivate) = source,
              isPrivate,
              let refreshURL
        else { return }

        isManualLoading = true
        defer { isManualLoading = false }

        do {
            let url = try await refreshURL()
            guard !Task.isCancelled else { return }
            let cacheKey = source.cacheKey(for: url) ?? url.absoluteString
            await download(url: url, cacheKey: cacheKey)
        } catch {
            guard !Task.isCancelled else { return }
            phase = .needsManualDownload
        }
    }

    private func download(url: URL, cacheKey: String) async {
        do {
            let data = try await SVGCache.shared.data(for: url, key: cacheKey)
            guard !Task.isCancelled else { return }
            guard let svg = String(data: data, encoding: .utf8) else {
                phase = .failure
                return
            }
            if case .ready(let current) = phase, current == svg {
                return
            }
            isRendered = false
            show(.ready(svg))
        } catch {
            guard !Task.isCancelled else { return }
            phase = source.isPrivate ? .needsManualDownload : .failure
        }
    }

    private func show(_ newPhase: Phase) {
        withAnimation(.easeIn(duration: 0.2)) {
            phase = newPhase
        }
    }
}
