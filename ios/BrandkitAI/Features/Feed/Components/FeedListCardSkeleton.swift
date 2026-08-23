import SwiftUI

/// Скелетон карточки на время загрузки ленты: структура и отступы те же,
/// что у настоящей карточки, а высоту строк задаёт скрытый текст в тех же
/// шрифтах — при подмене данными вёрстка не сдвигается.
struct FeedListCardSkeleton: View {
    var body: some View {
        VStack(spacing: 0) {
            ShimmerPlaceholder()
                .aspectRatio(1, contentMode: .fit)

            VStack(alignment: .leading, spacing: 8) {
                textLine(width: 96, font: FeedListCardStyle.titleFont)

                HStack(spacing: 8) {
                    ShimmerPlaceholder()
                        .frame(width: 24, height: 24)
                        .clipShape(Circle())

                    textLine(width: 64, font: FeedListCardStyle.authorFont)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
        }
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .accessibilityHidden(true)
    }

    /// Полоска-заглушка ровно в размер строки текста: скрытый символ даёт
    /// высоту строки шрифта, шиммер растягивается по ней оверлеем.
    private func textLine(width: CGFloat, font: Font) -> some View {
        Text(verbatim: "•")
            .font(font)
            .hidden()
            .frame(width: width, alignment: .leading)
            .overlay {
                ShimmerPlaceholder()
                    .clipShape(Capsule())
            }
    }
}

#Preview {
    FeedListCardSkeleton()
        .padding(16)
        .preferredColorScheme(.dark)
}
