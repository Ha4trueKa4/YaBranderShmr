import SwiftUI


// TODO: Заменить мок данные на реальные с api
struct FeedListView: View {
    let selectedTab: FeedTab
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            LazyVStack(spacing: 12) {
                FeedListCardView(
                    title: "Project Alpha",
                    accountImage: Image("image.placeholder"),
                    accountAvatar: Image("image.placeholder"),
                    accountName: "Alice",
                    onAccountTap: {}
                )
                FeedListCardView(
                    title: "Project Gamma with a very long title that should truncate",
                    accountImage: Image("image.placeholder"),
                    accountAvatar: Image("image.placeholder"),
                    accountName: "Charlie",
                    onAccountTap: {}
                )
                FeedListCardView(
                    title: "Project Epsilon",
                    accountImage: Image("image.placeholder"),
                    accountAvatar: Image("image.placeholder"),
                    accountName: "Eve",
                    onAccountTap: {}
                )
            }
            .frame(maxWidth: .infinity)
            
            LazyVStack(spacing: 12) {
                FeedListCardView(
                    title: "Project Beta",
                    accountImage: Image("image.placeholder"),
                    accountAvatar: Image("image.placeholder"),
                    accountName: "Bob",
                    onAccountTap: {}
                )
                FeedListCardView(
                    title: "Project Delta",
                    accountImage: Image("image.placeholder"),
                    accountAvatar: Image("image.placeholder"),
                    accountName: "Diana",
                    onAccountTap: {}
                )
                FeedListCardView(
                    title: "Project Zeta",
                    accountImage: Image("image.placeholder"),
                    accountAvatar: Image("image.placeholder"),
                    accountName: "Frank",
                    onAccountTap: {}
                )
            }
            .frame(maxWidth: .infinity)
        }
        .padding(16)
    }
}

#Preview {
    FeedListView(selectedTab: .all)
}
