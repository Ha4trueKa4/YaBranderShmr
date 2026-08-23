import SwiftUI

struct FeedHeaderView: View {
    var onSearch: () -> Void = {}

    var body: some View {
        HStack(spacing: 8) {
            logo

            Spacer(minLength: 8)

//            searchButton
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
    }

    private var logo: some View {
        HStack(spacing: 8) {
            Image(.appLogoXS)
                .resizable()
                .scaledToFit()
                .frame(width: 28, height: 28)
                .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))

            Text("Ya Brander")
                .font(.system(size: 18, weight: .bold))
                .lineLimit(1)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Ya Brander")
    }

//    private var searchButton: some View {
//        Button(action: onSearch) {
//            Image(systemName: "magnifyingglass")
//                .font(.system(size: 16, weight: .medium))
//                .foregroundStyle(.secondary)
//                .frame(width: 32, height: 32)
//                .background(Color(.tertiarySystemFill), in: .circle)
//                .contentShape(.circle)
//        }
//        .buttonStyle(.plain)
//        .accessibilityLabel("Search")
//    }
}

#Preview {
    VStack(spacing: 0) {
        FeedHeaderView()
        Spacer()
    }
}
