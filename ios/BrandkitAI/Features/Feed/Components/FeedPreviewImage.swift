import SwiftUI
import SVGView

/// Превью проекта: чистое отображение состояний загрузки.
/// Сеть и парсинг — в `SVGPreviewLoader`.
struct FeedPreviewImage: View {
    let url: URL?

    private enum Content {
        case loading
        case svg(SVGPreview)
        case failure
    }

    private let loader = SVGPreviewLoader()

    @State private var content: Content = .loading

    var body: some View {
        preview
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.secondarySystemBackground))
            .aspectRatio(1, contentMode: .fit)
            .clipped()
            .task(id: url) {
                await load()
            }
    }

    @ViewBuilder
    private var preview: some View {
        switch content {
        case .loading:
            ShimmerPlaceholder()

        case .svg(let preview):
            SVGView(svg: preview.node)
                .aspectRatio(preview.aspectRatio, contentMode: .fit)

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
        // Результат уже есть — ячейка просто вернулась на экран.
        guard case .loading = content else { return }

        guard let url else {
            show(.failure)
            return
        }

        do {
            show(.svg(try await loader.load(from: url)))
        } catch {
            guard !Task.isCancelled else { return }
            show(.failure)
        }
    }

    private func show(_ newContent: Content) {
        withAnimation(.easeIn(duration: 0.2)) {
            content = newContent
        }
    }
}

#Preview {
    HStack(spacing: 12) {
        FeedPreviewImage(url: nil)

        ShimmerPlaceholder()
            .aspectRatio(1, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
    .padding(16)
    .preferredColorScheme(.dark)
}
