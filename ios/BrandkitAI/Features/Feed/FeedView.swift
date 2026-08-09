import SwiftUI

struct FeedView: View {
    @State private var selectedTab: FeedTab = .all

    var body: some View {
        VStack(spacing: 0) {
            FeedHeaderView()

            FeedTabPickerView(selection: $selectedTab)

            FeedPagerView(selection: $selectedTab) { _ in
                FeedListView()
            }
        }
    }
}

#Preview {
    FeedView()
}
