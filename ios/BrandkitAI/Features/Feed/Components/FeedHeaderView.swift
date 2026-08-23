import SwiftUI

struct FeedHeaderView: View {
    var onSearch: () -> Void = {}

    private static let brandGradient = LinearGradient(
        colors: [Color(.startGradient), Color(.endGradient)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    var body: some View {
        HStack(spacing: 8) {
            logo

            Spacer(minLength: 8)

            searchButton
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
    }

    private var logo: some View {
        HStack(spacing: 8) {
            Image(systemName: "cpu")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Self.brandGradient)

            HStack(spacing: 5) {
                Text("Brandkit")
                    .foregroundStyle(.primary)

                Text("AI")
                    .foregroundStyle(Self.brandGradient)
            }
            .font(.system(size: 18, weight: .bold))
            .lineLimit(1)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Ya Brander")
    }

    private var searchButton: some View {
        Button(action: onSearch) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(.secondary)
                .frame(width: 32, height: 32)
                .background(Color(.tertiarySystemFill), in: .circle)
                .contentShape(.circle)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Search")
    }
}

#Preview {
    VStack(spacing: 0) {
        FeedHeaderView()
        Spacer()
    }
}
