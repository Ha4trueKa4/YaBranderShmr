import SwiftUI

/// Превью проекта в карточке: квадратная обрезка поверх `SVGDocumentView`.
struct FeedPreviewImage: View {
    let url: URL?

    var body: some View {
        SVGDocumentView(source: .url(url))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.secondarySystemBackground))
            .aspectRatio(1, contentMode: .fit)
            .clipped()
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
