import SwiftUI
import SVGView

struct FeedPreviewImage: View {
    let url: URL?

    private enum Content {
        case loading
        case svg(SVGNode, aspectRatio: CGFloat)
        case failure
    }

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

        case .svg(let node, let aspectRatio):
            SVGView(svg: node)
                .aspectRatio(aspectRatio, contentMode: .fit)

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
        guard case .loading = content else { return }

        guard let url else {
            show(.failure)
            return
        }

        do {
            let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
            let (data, _) = try await URLSession.shared.data(for: request)

            guard let node = SVGParser.parse(data: data) else {
                show(.failure)
                return
            }

            show(.svg(node, aspectRatio: Self.aspectRatio(of: node)))
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

    private static func aspectRatio(of node: SVGNode) -> CGFloat {
        guard let viewport = node as? SVGViewport,
              let viewBox = viewport.viewBox,
              viewBox.width > 0, viewBox.height > 0
        else { return 1 }

        return viewBox.width / viewBox.height
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
