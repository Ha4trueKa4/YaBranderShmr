import SwiftUI

struct FeedView: View {
    var onOpenSearch: () -> Void = {}
    var onOpenProject: () -> Void = {}

    @State private var selectedTab: FeedTab = .all

    var body: some View {
        VStack(spacing: 0) {
            FeedHeaderView(onSearch: onOpenSearch)

            FeedTabPickerView(selection: $selectedTab)

            FeedPagerView(selection: $selectedTab) { _ in
                FeedListView(onOpenProject: onOpenProject)
            }
        }
    }
}

#Preview {
    FeedView()
}
