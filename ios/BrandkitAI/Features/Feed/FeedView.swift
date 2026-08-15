import SwiftUI

struct FeedView: View {
    var onOpenSearch: () -> Void = {}
    var onOpenProject: (UUID) -> Void = { _ in }

    @State private var selectedTab: FeedTab = .all

    var body: some View {
        VStack(spacing: 0) {
            FeedHeaderView(onSearch: onOpenSearch)

            FeedTabPickerView(selection: $selectedTab)

            FeedPagerView(selection: $selectedTab) { tab in
                FeedListView(scope: tab.scope, onOpenProject: onOpenProject)
            }
        }
    }
}

#Preview {
    FeedView()
}
